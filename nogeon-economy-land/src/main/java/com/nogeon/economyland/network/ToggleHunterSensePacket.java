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

public final class ToggleHunterSensePacket {
    public ToggleHunterSensePacket() {
    }

    public static void encode(ToggleHunterSensePacket packet, FriendlyByteBuf buffer) {
    }

    public static ToggleHunterSensePacket decode(FriendlyByteBuf buffer) {
        return new ToggleHunterSensePacket();
    }

    public static void handle(ToggleHunterSensePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            if (profile.selectedJob() != JobType.HUNTER) {
                sender.displayClientMessage(Component.literal("§c사냥꾼 직업일 때만 스킬을 활성화할 수 있습니다."), true);
                return;
            }

            int quickDrawLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_QUICK_DRAW);
            if (quickDrawLevel <= 0) {
                sender.displayClientMessage(Component.literal("§c먼저 [추적자의 감각] 스킬을 배워야 합니다."), true);
                return;
            }

            boolean next = !profile.hunterSenseActive();
            
            if (next && sender.getFoodData().getFoodLevel() <= 2) {
                sender.displayClientMessage(Component.literal("§c허기가 너무 부족하여 추적 상태에 진입할 수 없습니다!"), true);
                return;
            }

            profile.setHunterSenseActive(next);
            if (next) {
                profile.setHunterSenseTicks(0);
            }
            state.setDirty();

            sender.displayClientMessage(Component.literal("§2[추적자의 감각] §f스킬이 " + (next ? "§a활성화(ON)" : "§c비활성화(OFF)") + "§f되었습니다."), true);

            int radius = Math.min(42, 12 + quickDrawLevel * 3);
            if (next) {
                com.nogeon.economyland.job.JobEvents.applyHunterSenseGlow(sender, radius);
            } else {
                com.nogeon.economyland.job.JobEvents.clearHunterSenseGlow(sender, radius);
            }
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                new SyncHunterAbilityPacket(next, radius, profile.hunterPreyMarkedUUID()));
        });
        context.setPacketHandled(true);
    }
}
