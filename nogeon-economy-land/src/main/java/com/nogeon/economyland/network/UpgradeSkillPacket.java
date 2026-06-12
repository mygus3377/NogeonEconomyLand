package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.SkillsOpener;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class UpgradeSkillPacket {
    private final String jobId;
    private final String skillId;

    public UpgradeSkillPacket(String jobId, String skillId) {
        this.jobId = jobId;
        this.skillId = skillId;
    }

    public static void encode(UpgradeSkillPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.jobId);
        buffer.writeUtf(packet.skillId);
    }

    public static UpgradeSkillPacket decode(FriendlyByteBuf buffer) {
        return new UpgradeSkillPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(UpgradeSkillPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            JobType job = JobType.byId(packet.jobId);
            if (profile.selectedJob() != job) {
                return;
            }

            SkillNode skill = SkillNode.byId(packet.skillId);
            if (profile.job(job).upgrade(skill)) {
                state.setDirty();
                sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.skill.upgraded",
                    Component.translatable(skill.titleKey())), false);
            }
            SkillsOpener.open(sender);
        });
        context.setPacketHandled(true);
    }
}
