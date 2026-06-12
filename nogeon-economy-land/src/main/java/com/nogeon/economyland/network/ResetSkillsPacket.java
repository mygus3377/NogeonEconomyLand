package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.SkillsOpener;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class ResetSkillsPacket {
    private final String jobId;
    public static final long COST = 100000L;

    public ResetSkillsPacket(String jobId) {
        this.jobId = jobId;
    }

    public static void encode(ResetSkillsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.jobId);
    }

    public static ResetSkillsPacket decode(FriendlyByteBuf buffer) {
        return new ResetSkillsPacket(buffer.readUtf());
    }

    public static void handle(ResetSkillsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

            if (!profile.spendCredits(COST)) {
                sender.displayClientMessage(Component.literal("§c스킬 초기화에 필요한 100,000 크레딧이 부족합니다."), false);
                return;
            }

            profile.job(job).resetSkills();
            
            // 직업별 활성화 모듈 해제
            if (job == JobType.MINER) {
                profile.setMinerEyeActive(false);
            } else if (job == JobType.HUNTER) {
                profile.setHunterSenseActive(false);
                profile.setHunterSenseTicks(0);
                profile.setHunterPreyMarkedUUID("");
            }

            state.setDirty();
            sender.displayClientMessage(Component.literal("§a" + Component.translatable("job.nogeon_economy_land." + job.id()).getString() + " 직업의 스킬을 100,000 크레딧을 지불하고 모두 초기화하였습니다!"), false);
            
            SkillsOpener.open(sender);
        });
        context.setPacketHandled(true);
    }
}
