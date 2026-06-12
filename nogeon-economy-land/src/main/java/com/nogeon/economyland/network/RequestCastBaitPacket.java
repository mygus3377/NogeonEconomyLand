package com.nogeon.economyland.network;

import com.nogeon.economyland.job.JobEvents;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkEvent;

public class RequestCastBaitPacket {
    public RequestCastBaitPacket() {
    }

    public static void encode(RequestCastBaitPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestCastBaitPacket decode(FriendlyByteBuf buf) {
        return new RequestCastBaitPacket();
    }

    public static void handle(RequestCastBaitPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());

            if (profile.selectedJob() != JobType.FISHER) {
                return;
            }

            int calmWaterLevel = profile.job(JobType.FISHER).nodeLevel(SkillNode.FISHER_CALM_WATER);
            if (calmWaterLevel <= 0) {
                player.displayClientMessage(Component.literal("§c[미끼 뿌리기] §f미끼 뿌리기 스킬(75레벨)이 해금되지 않았습니다!"), true);
                return;
            }

            net.minecraft.nbt.CompoundTag playerNbt = player.getPersistentData();
            int gauge = playerNbt.getInt("nogeon_fisher_flow_gauge");
            if (gauge < 100) {
                player.displayClientMessage(Component.literal("§c[미끼 뿌리기] §f흐름 게이지가 아직 부족합니다! (§e" + gauge + "%§f / 100%)"), true);
                return;
            }

            // 시선 방향의 물 블록 찾기 (최대 6블록)
            BlockPos targetPos = JobEvents.waterBlockForFishingHook(player.serverLevel(), player.fishing);
            HitResult hit = player.pick(6.0D, 0.0F, true);
            if (targetPos != null || hit.getType() == HitResult.Type.BLOCK) {
                if (targetPos == null) {
                    targetPos = ((BlockHitResult) hit).getBlockPos();
                }
                if (targetPos != null && player.level().getBlockState(targetPos).is(Blocks.WATER)
                    && JobEvents.isFishingWaterBody(player.serverLevel(), targetPos)) {
                    // 게이지 차감
                    playerNbt.putInt("nogeon_fisher_flow_gauge", 0);
                    
                    int duration = Math.min(180, 60 + calmWaterLevel * 12) * 20;
                    JobEvents.FISHERY_ZONES.put(targetPos, new JobEvents.FisheryZone(targetPos, duration, calmWaterLevel, player.getUUID()));
                    JobEvents.syncFisherDataToPlayer(player);
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel sLevel) {
                        double radius = Math.min(18.0D, 5.0D + calmWaterLevel * 1.4D);
                        for (int i = 0; i < 48; i++) {
                            double angle = (i / 48.0D) * Math.PI * 2.0D;
                            double x = targetPos.getX() + 0.5D + Math.cos(angle) * radius;
                            double z = targetPos.getZ() + 0.5D + Math.sin(angle) * radius;
                            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, x, targetPos.getY() + 0.9D, z, 2, 0.0D, 0.12D, 0.0D, 0.0D);
                            if (i % 2 == 0) {
                                sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW, x, targetPos.getY() + 1.05D, z, 1, 0.0D, 0.04D, 0.0D, 0.0D);
                            }
                        }
                        sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FISHING,
                            targetPos.getX() + 0.5D, targetPos.getY() + 1.0D, targetPos.getZ() + 0.5D,
                            40, radius * 0.45D, 0.15D, radius * 0.45D, 0.0D);
                    }

                    player.displayClientMessage(Component.literal("§d🌊 [미끼 뿌리기] §f이곳에 물고기가 가득 찬 §b어장§f을 성공적으로 형성했습니다!"), false);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.FISHING_BOBBER_SPLASH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
                    player.level().playSound(null, targetPos,
                        net.minecraft.sounds.SoundEvents.CONDUIT_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.75F, 1.45F);
                } else {
                    player.displayClientMessage(Component.literal("§c[미끼 뿌리기] §f물을 조준하고 사용해야 합니다!"), true);
                }
            } else {
                player.displayClientMessage(Component.literal("§c[미끼 뿌리기] §f조준 거리가 너무 멉니다. 물을 더 가깝게 바라보세요!"), true);
            }
        });
        ctx.setPacketHandled(true);
    }
}
