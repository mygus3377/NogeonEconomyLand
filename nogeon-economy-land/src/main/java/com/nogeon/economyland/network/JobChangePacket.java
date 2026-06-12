package com.nogeon.economyland.network;

import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerDisplayNameManager;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class JobChangePacket {
    public static final long PRICE = 50000L;
    private final String jobId;

    public JobChangePacket(String jobId) {
        this.jobId = jobId;
    }

    public static void encode(JobChangePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.jobId);
    }

    public static JobChangePacket decode(FriendlyByteBuf buffer) {
        return new JobChangePacket(buffer.readUtf());
    }

    public static void handle(JobChangePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            JobType job;
            try {
                job = JobType.byId(packet.jobId);
            } catch (IllegalArgumentException exception) {
                return;
            }

            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            JobType previousJob = profile.selectedJob();
            if (profile.selectedJob() == job) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.job.already_selected",
                    Component.translatable("job.nogeon_economy_land." + job.id())), false);
                return;
            }
            if (!profile.spendCredits(PRICE)) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.job.change_no_money", PRICE), false);
                return;
            }

            profile.setSelectedJob(job);
            resetMinerAbilitiesOnJobChange(player, profile, previousJob, job);
            state.setDirty();
            SyncCreditsPacket.send(player, profile.credits());
            PlayerDisplayNameManager.refresh(player, profile);
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.job.changed_paid",
                Component.translatable("job.nogeon_economy_land." + job.id()), PRICE), false);
            player.closeContainer();
        });
        context.setPacketHandled(true);
    }

    private static void resetMinerAbilitiesOnJobChange(ServerPlayer player, PlayerProfile profile, JobType previousJob, JobType nextJob) {
        if (previousJob != JobType.MINER && nextJob != JobType.MINER) {
            return;
        }
        boolean bodyActive = false;
        int eyeRadius = 0;
        if (nextJob == JobType.MINER) {
            bodyActive = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_STONE_SKIN) > 0;
            int eyeLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_EYE_OPENING);
            eyeRadius = eyeLevel > 0 ? Math.min(28, 8 + eyeLevel * 2) : 0;
        }
        profile.setMinerBodyActive(bodyActive);
        profile.setMinerEyeActive(false);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMinerAbilityPacket(bodyActive, false, eyeRadius));
    }
}
