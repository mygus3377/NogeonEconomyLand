package com.nogeon.economyland.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.network.NetworkHooks;

public final class PortalEntity extends Entity {
    private static final EntityDataAccessor<String> TARGET_DIM =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<BlockPos> TARGET_POS =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BLOCK_POS);

    private UUID creatorUuid;
    private UUID linkedPortalUuid;
    private int age;

    public PortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(TARGET_DIM, "");
        entityData.define(TARGET_POS, BlockPos.ZERO);
    }

    public void setup(String targetDim, BlockPos targetPos, UUID creatorUuid, UUID linkedPortalUuid) {
        entityData.set(TARGET_DIM, targetDim);
        entityData.set(TARGET_POS, targetPos);
        this.creatorUuid = creatorUuid;
        this.linkedPortalUuid = linkedPortalUuid;
    }

    public String getTargetDim() {
        return entityData.get(TARGET_DIM);
    }

    public BlockPos getTargetPos() {
        return entityData.get(TARGET_POS);
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    public UUID getLinkedPortalUuid() {
        return linkedPortalUuid;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // 포탈 주변의 무작위 원형 범위에서 뿜어 나오는 파티클 효과 추가
            if (this.random.nextFloat() < 0.6F) {
                double rx = getX() + (this.random.nextDouble() - 0.5D) * 1.6D;
                double ry = getY() + this.random.nextDouble() * 1.8D - 0.3D;
                double rz = getZ() + (this.random.nextDouble() - 0.5D) * 1.6D;
                double mx = (this.random.nextDouble() - 0.5D) * 0.05D;
                double my = (this.random.nextDouble() - 0.5D) * 0.05D;
                double mz = (this.random.nextDouble() - 0.5D) * 0.05D;
                level().addParticle(ParticleTypes.PORTAL, rx, ry, rz, mx, my, mz);
            }
            
            // 잔잔하게 들리는 웅성거리는 포탈 소리 (60틱 = 3초마다)
            if (this.tickCount % 60 == 0) {
                level().playLocalSound(getX(), getY(), getZ(), SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.25F, 1.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.15F, false);
            }
        } else {
            age++;
            // 30분(36000 틱) 수명 제한
            if (age >= 36000) {
                discardWithLinked();
            }
        }
    }

    private void discardWithLinked() {
        discard();
        if (linkedPortalUuid != null && level() instanceof ServerLevel serverLevel) {
            for (ServerLevel otherLevel : serverLevel.getServer().getAllLevels()) {
                Entity other = otherLevel.getEntity(linkedPortalUuid);
                if (other instanceof PortalEntity portal) {
                    portal.discard();
                }
            }
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            String targetDimStr = getTargetDim();
            BlockPos targetPos = getTargetPos();

            if (!targetDimStr.isEmpty() && targetPos != null) {
                ResourceLocation dimLoc = ResourceLocation.tryParse(targetDimStr);
                if (dimLoc != null) {
                    ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
                    ServerLevel targetLevel = serverPlayer.server.getLevel(targetKey);
                    if (targetLevel != null) {
                        // 텔레포트 소리 효과 (출발지)
                        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.2F);
                        
                        // 순간이동
                        serverPlayer.teleportTo(targetLevel, targetPos.getX() + 0.5D, targetPos.getY() + 0.1D, targetPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
                        
                        // 텔레포트 소리 (도착지)
                        targetLevel.playSound(null, targetPos.getX() + 0.5D, targetPos.getY() + 0.1D, targetPos.getZ() + 0.5D, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.2F);

                        // 양쪽 포탈 파괴 (1회용 귀환)
                        discardWithLinked();
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        entityData.set(TARGET_DIM, nbt.getString("TargetDim"));
        entityData.set(TARGET_POS, new BlockPos(nbt.getInt("TargetX"), nbt.getInt("TargetY"), nbt.getInt("TargetZ")));
        if (nbt.hasUUID("CreatorUuid")) {
            creatorUuid = nbt.getUUID("CreatorUuid");
        }
        if (nbt.hasUUID("LinkedPortalUuid")) {
            linkedPortalUuid = nbt.getUUID("LinkedPortalUuid");
        }
        age = nbt.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putString("TargetDim", getTargetDim());
        BlockPos pos = getTargetPos();
        nbt.putInt("TargetX", pos.getX());
        nbt.putInt("TargetY", pos.getY());
        nbt.putInt("TargetZ", pos.getZ());
        if (creatorUuid != null) {
            nbt.putUUID("CreatorUuid", creatorUuid);
        }
        if (linkedPortalUuid != null) {
            nbt.putUUID("LinkedPortalUuid", linkedPortalUuid);
        }
        nbt.putInt("Age", age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
