package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkHooks;

public final class TradeOpener {
    private TradeOpener() {
    }

    public static void open(ServerPlayer player) {
        TradeSession session = EconomyState.get(player.server).tradeSession(player.getUUID());
        if (session != null) {
            open(player, session);
        }
    }

    public static void open(ServerPlayer player, TradeSession session) {
        UUID partnerId = session.partner(player.getUUID());
        ServerPlayer partner = player.server.getPlayerList().getPlayer(partnerId);
        String partnerName = partner == null ? "Offline" : partner.getName().getString();
        long availableCredits = EconomyState.get(player.server).profile(player.getUUID()).credits();
        EconomyState state = EconomyState.get(player.server);
        List<TradeOfferLine> ownOffers = toLines(session.offers(player.getUUID()));
        List<TradeOfferLine> partnerOffers = toLines(session.partnerOffers(player.getUUID()));
        List<TradeLandLine> ownLandOffers = toLandLines(state, session.landOffers(player.getUUID()));
        List<TradeLandLine> partnerLandOffers = toLandLines(state, session.partnerLandOffers(player.getUUID()));
        List<TradeChatLine> chatLines = toChatLines(session, player.getUUID());
        TradeMenu snapshot = new TradeMenu(0, partnerId.toString(), partnerName, availableCredits,
            session.credits(player.getUUID()), session.partnerCredits(player.getUUID()), session.ready(player.getUUID()),
            session.partnerReady(player.getUUID()), session.confirmed(player.getUUID()), session.partnerConfirmed(player.getUUID()),
            ownOffers, partnerOffers, ownLandOffers, partnerLandOffers, chatLines);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new TradeMenu(containerId, partnerId.toString(), partnerName, availableCredits,
                session.credits(player.getUUID()), session.partnerCredits(player.getUUID()), session.ready(player.getUUID()),
                session.partnerReady(player.getUUID()), session.confirmed(player.getUUID()), session.partnerConfirmed(player.getUUID()),
                ownOffers, partnerOffers, ownLandOffers, partnerLandOffers, chatLines),
            Component.translatable("screen.nogeon_economy_land.trade")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }

    public static void refreshBoth(ServerPlayer first, ServerPlayer second, TradeSession session) {
        open(first, session);
        open(second, session);
    }

    private static List<TradeOfferLine> toLines(Map<String, Integer> offers) {
        List<TradeOfferLine> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : offers.entrySet()) {
            ResourceLocation itemId = new ResourceLocation(entry.getKey());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            lines.add(new TradeOfferLine(item.getDescriptionId(), entry.getValue()));
        }
        return lines;
    }

    private static List<TradeLandLine> toLandLines(EconomyState state, Set<Integer> landIds) {
        List<TradeLandLine> lines = new ArrayList<>();
        for (Integer landId : landIds) {
            if (landId == null) {
                continue;
            }
            com.nogeon.economyland.land.LandRegion land = state.landById(landId);
            if (land != null) {
                lines.add(new TradeLandLine(land.id(), land.type().translationKey(), land.blocks()));
            }
        }
        return lines;
    }

    private static List<TradeChatLine> toChatLines(TradeSession session, UUID playerId) {
        List<TradeChatLine> lines = new ArrayList<>();
        for (TradeSession.ChatMessage message : session.chatMessages()) {
            lines.add(new TradeChatLine(message.senderName(), message.message(), playerId.equals(message.senderId())));
        }
        return lines;
    }
}