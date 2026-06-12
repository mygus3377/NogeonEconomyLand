package com.nogeon.economyland.network;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.menu.HighLowOpener;
import com.nogeon.economyland.menu.TraderActionOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.HighLowSession;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class HighLowActionPacket {
    private final String actionId;
    private final long amount;

    public HighLowActionPacket(String actionId) {
        this(actionId, 0L);
    }

    public HighLowActionPacket(String actionId, long amount) {
        this.actionId = actionId;
        this.amount = amount;
    }

    public static void encode(HighLowActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.actionId);
        buffer.writeLong(packet.amount);
    }

    public static HighLowActionPacket decode(FriendlyByteBuf buffer) {
        return new HighLowActionPacket(buffer.readUtf(), buffer.readLong());
    }

    public static void handle(HighLowActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            switch (packet.actionId) {
                case "start" -> {
                    PlayerProfile profile = state.profile(sender.getUUID());
                    long baseCap = profile.socialClass().maxBetCap();
                    long maxCap = Math.min(1000000L, Math.round(baseCap * (1.0D + Math.min(10, profile.gambleStreak()) * 0.1D)));
                    if (packet.amount > maxCap) {
                        sender.displayClientMessage(Component.literal("배팅 한도를 초과했습니다. (최대 " + maxCap + " C)"), false);
                        return;
                    }
                    if (packet.amount <= 0L || !profile.spendCredits(packet.amount)) {
                        sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.shop.no_money"), false);
                        return;
                    }
                    HighLowSession session = state.startHighLow(sender, packet.amount);
                    HighLowOpener.open(sender, session);
                    SyncCreditsPacket.send(sender, profile.credits());
                    state.setDirty();
                }
                case "higher" -> { // HIT
                    HighLowSession session = state.highLowSession(sender.getUUID());
                    if (session != null && session.canHit()) {
                        session.hit(sender);
                        HighLowOpener.open(sender, session);
                        state.setDirty();
                    }
                }
                case "lower" -> { // STAND
                    HighLowSession session = state.highLowSession(sender.getUUID());
                    if (session != null && session.canStand()) {
                        session.stand(sender);
                        HighLowOpener.open(sender, session);
                        state.setDirty();
                    }
                }
                case "raise" -> { // DOUBLE DOWN
                    HighLowSession session = state.highLowSession(sender.getUUID());
                    if (session != null && session.canDoubleDown(sender)) {
                        PlayerProfile profile = state.profile(sender.getUUID());
                        session.doubleDown(sender);
                        HighLowOpener.open(sender, session);
                        SyncCreditsPacket.send(sender, profile.credits());
                        state.setDirty();
                    }
                }
                case "cashout", "leave" -> { // EXIT & CLAIM PAYOUT
                    state.finishHighLow(sender, true);
                    TraderActionOpener.open(sender, TraderKind.GAMBLER);
                }
                default -> {
                }
            }
        });
        context.setPacketHandled(true);
    }
}
