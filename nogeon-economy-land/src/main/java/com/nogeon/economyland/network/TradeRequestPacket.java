package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeRequestOpener;
import com.nogeon.economyland.state.EconomyState;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeRequestPacket {
    private final String targetId;

    public TradeRequestPacket(String targetId) {
        this.targetId = targetId;
    }

    public static void encode(TradeRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.targetId);
    }

    public static TradeRequestPacket decode(FriendlyByteBuf buffer) {
        return new TradeRequestPacket(buffer.readUtf());
    }

    public static void handle(TradeRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            ServerPlayer target = sender.server.getPlayerList().getPlayer(UUID.fromString(packet.targetId));
            if (target == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            if (!state.canTradeTogether(sender, target)) {
                sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.too_far"), false);
                return;
            }
            if (!state.requestTrade(sender, target)) {
                sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.busy"), false);
                return;
            }
            sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.request_sent", target.getName()), false);
            TradeRequestOpener.open(target, sender);
        });
        context.setPacketHandled(true);
    }
}