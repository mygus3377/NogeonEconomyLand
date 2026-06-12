package com.nogeon.economyland.network;

import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class MarkHunterPreyPacket {
    public MarkHunterPreyPacket() {
    }

    public static void encode(MarkHunterPreyPacket packet, FriendlyByteBuf buffer) {
    }

    public static MarkHunterPreyPacket decode(FriendlyByteBuf buffer) {
        return new MarkHunterPreyPacket();
    }

    public static void handle(MarkHunterPreyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            if (profile.selectedJob() != JobType.HUNTER) {
                sender.displayClientMessage(Component.literal("§c사냥꾼 직업일 때만 표식을 새길 수 있습니다."), true);
                return;
            }

            int markLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_WILD_STEP);
            if (markLevel <= 0) {
                sender.displayClientMessage(Component.literal("§c먼저 [사냥감의 표식] 스킬을 배워야 합니다."), true);
                return;
            }

            if (!profile.hunterSenseActive()) {
                sender.displayClientMessage(Component.literal("§c[추적자의 감각] 상태가 켜져 있어야 표식을 새길 수 있습니다!"), true);
                return;
            }

            // 시선 레이캐스트
            double range = 24.0D;
            Vec3 eyePos = sender.getEyePosition(1.0F);
            Vec3 viewVec = sender.getViewVector(1.0F);
            Vec3 endVec = eyePos.add(viewVec.scale(range));
            
            AABB searchArea = sender.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(2.0D, 2.0D, 2.0D);
            LivingEntity targetEntity = null;
            double bestDist = range;

            for (Entity entity : sender.level().getEntities(sender, searchArea, e -> e instanceof LivingEntity)) {
                if (entity == sender) continue;
                AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
                Optional<Vec3> clip = aabb.clip(eyePos, endVec);
                if (aabb.contains(eyePos)) {
                    targetEntity = (LivingEntity) entity;
                    break;
                } else if (clip.isPresent()) {
                    double dist = eyePos.distanceToSqr(clip.get());
                    if (dist < bestDist * bestDist) {
                        bestDist = Math.sqrt(dist);
                        targetEntity = (LivingEntity) entity;
                    }
                }
            }

            if (targetEntity == null) {
                sender.displayClientMessage(Component.literal("§c바라보고 있는 사냥 대상이 없습니다."), true);
                return;
            }

            // 감지되었는지 확인 (Glowing 효과 검증)
            if (!targetEntity.hasEffect(MobEffects.GLOWING)) {
                sender.displayClientMessage(Component.literal("§c[추적자의 감각]으로 감지되어 아웃라인이 빛나는 대상에게만 표식을 새길 수 있습니다."), true);
                return;
            }

            String targetUUID = targetEntity.getStringUUID();
            profile.setHunterPreyMarkedUUID(targetUUID);
            targetEntity.getPersistentData().putBoolean("nogeon_hunter_marked", true);
            state.setDirty();

            // 표식 지정 사운드 및 입자
            ServerLevel level = sender.serverLevel();
            level.playSound(null, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(),
                SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.2F, 0.6F);
            
            for (int i = 0; i < 12; i++) {
                double px = targetEntity.getX() + (level.random.nextDouble() - 0.5D) * 0.8D;
                double py = targetEntity.getY() + level.random.nextDouble() * targetEntity.getBbHeight();
                double pz = targetEntity.getZ() + (level.random.nextDouble() - 0.5D) * 0.8D;
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }

            sender.displayClientMessage(Component.literal("§4[사냥감의 표식] §f새로운 사냥 대상을 조준하고 표식을 새겼습니다!"), true);

            int quickDrawLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_QUICK_DRAW);
            int radius = Math.min(42, 12 + quickDrawLevel * 3);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                new SyncHunterAbilityPacket(true, radius, targetUUID));
        });
        context.setPacketHandled(true);
    }
}
