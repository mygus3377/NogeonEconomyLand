package com.nogeon.economyland.network;

import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class ToggleMinerAbilityPacket {
    private final int abilityType; // 1: 우월한 신체, 2: 개안

    public ToggleMinerAbilityPacket(int abilityType) {
        this.abilityType = abilityType;
    }

    public static void encode(ToggleMinerAbilityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.abilityType);
    }

    public static ToggleMinerAbilityPacket decode(FriendlyByteBuf buffer) {
        return new ToggleMinerAbilityPacket(buffer.readInt());
    }

    public static void handle(ToggleMinerAbilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            if (profile.selectedJob() != JobType.MINER) {
                sender.displayClientMessage(Component.literal("§c광부 직업일 때만 스킬을 활성화할 수 있습니다."), true);
                return;
            }

            int eyeLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_EYE_OPENING);
            int eyeRadius = eyeLevel > 0 ? Math.min(28, 8 + eyeLevel * 2) : 0;

            if (packet.abilityType == 1) {
                // 우월한 신체 (50레벨)
                if (profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_STONE_SKIN) <= 0) {
                    sender.displayClientMessage(Component.literal("§c먼저 [우월한 신체] 스킬을 배워야 합니다."), true);
                    return;
                }
                boolean next = !profile.minerBodyActive();
                profile.setMinerBodyActive(next);
                state.setDirty();
                sender.displayClientMessage(Component.literal("§6[우월한 신체] §f스킬이 " + (next ? "§a활성화(ON)" : "§c비활성화(OFF)") + "§f되었습니다."), true);
                
                // 클라이언트로 즉시 동기화 패킷 발송
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                    new SyncMinerAbilityPacket(next, profile.minerEyeActive(), eyeRadius));
            } else if (packet.abilityType == 2) {
                // 개안 (100레벨)
                if (eyeLevel <= 0) {
                    sender.displayClientMessage(Component.literal("§c먼저 [개안] 스킬을 배워야 합니다."), true);
                    return;
                }
                boolean next = !profile.minerEyeActive();
                profile.setMinerEyeActive(next);
                state.setDirty();
                sender.displayClientMessage(Component.literal("§b[개안] §f스킬이 " + (next ? "§a활성화(ON)" : "§c비활성화(OFF)") + "§f되었습니다."), true);

                if (next && sender.getHealth() <= sender.getMaxHealth() * 0.05F) {
                    profile.setMinerEyeActive(false);
                    state.setDirty();
                    sender.displayClientMessage(Component.literal("§c[개안] §f체력이 부족하여 개안을 활성화할 수 없습니다."), true);
                    next = false;
                }
                
                // 클라이언트로 즉시 동기화 패킷 발송
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                    new SyncMinerAbilityPacket(profile.minerBodyActive(), next, eyeRadius));
            }
        });
        context.setPacketHandled(true);
    }
}
