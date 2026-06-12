package com.nogeon.economyland.network;

import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class RequestOpenCookRecipePacket {
    public RequestOpenCookRecipePacket() {
    }

    public static void encode(RequestOpenCookRecipePacket msg, FriendlyByteBuf buf) {
    }

    public static RequestOpenCookRecipePacket decode(FriendlyByteBuf buf) {
        return new RequestOpenCookRecipePacket();
    }

    public static void handle(RequestOpenCookRecipePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());

            if (profile.selectedJob() == JobType.COOK) {
                int masterRecipeLevel = profile.job(JobType.COOK).nodeLevel(SkillNode.COOK_MASTER_RECIPE);
                if (masterRecipeLevel > 0) {
                    // 요리사 100레벨 노드 확인 완료 -> 슬롯 계산 (레벨 1~2: 1개, 레벨 3: 2개)
                    int maxSlots = masterRecipeLevel >= 3 ? 2 : 1;
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new OpenCookRecipeScreenPacket(maxSlots, profile.cookRecipeBuffs()));
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
