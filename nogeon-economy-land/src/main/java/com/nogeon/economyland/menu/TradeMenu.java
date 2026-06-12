package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class TradeMenu extends AbstractContainerMenu {
    private final String partnerId;
    private final String partnerName;
    private final long availableCredits;
    private final long offeredCredits;
    private final long partnerCredits;
    private final boolean ready;
    private final boolean partnerReady;
    private final boolean confirmed;
    private final boolean partnerConfirmed;
    private final List<TradeOfferLine> ownOffers;
    private final List<TradeOfferLine> partnerOffers;
    private final List<TradeLandLine> ownLandOffers;
    private final List<TradeLandLine> partnerLandOffers;
    private final List<TradeChatLine> chatLines;

    public TradeMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.TRADE.get(), containerId);
        partnerId = buffer.readUtf();
        partnerName = buffer.readUtf();
        availableCredits = buffer.readLong();
        offeredCredits = buffer.readLong();
        partnerCredits = buffer.readLong();
        ready = buffer.readBoolean();
        partnerReady = buffer.readBoolean();
        confirmed = buffer.readBoolean();
        partnerConfirmed = buffer.readBoolean();
        ownOffers = readLines(buffer);
        partnerOffers = readLines(buffer);
        ownLandOffers = readLandLines(buffer);
        partnerLandOffers = readLandLines(buffer);
        chatLines = readChatLines(buffer);
    }

    public TradeMenu(int containerId, String partnerId, String partnerName, long availableCredits, long offeredCredits,
        long partnerCredits, boolean ready, boolean partnerReady, boolean confirmed, boolean partnerConfirmed,
        List<TradeOfferLine> ownOffers, List<TradeOfferLine> partnerOffers,
        List<TradeLandLine> ownLandOffers, List<TradeLandLine> partnerLandOffers, List<TradeChatLine> chatLines) {
        super(ModMenus.TRADE.get(), containerId);
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.availableCredits = availableCredits;
        this.offeredCredits = offeredCredits;
        this.partnerCredits = partnerCredits;
        this.ready = ready;
        this.partnerReady = partnerReady;
        this.confirmed = confirmed;
        this.partnerConfirmed = partnerConfirmed;
        this.ownOffers = ownOffers;
        this.partnerOffers = partnerOffers;
        this.ownLandOffers = ownLandOffers;
        this.partnerLandOffers = partnerLandOffers;
        this.chatLines = chatLines;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(partnerId);
        buffer.writeUtf(partnerName);
        buffer.writeLong(availableCredits);
        buffer.writeLong(offeredCredits);
        buffer.writeLong(partnerCredits);
        buffer.writeBoolean(ready);
        buffer.writeBoolean(partnerReady);
        buffer.writeBoolean(confirmed);
        buffer.writeBoolean(partnerConfirmed);
        writeLines(buffer, ownOffers);
        writeLines(buffer, partnerOffers);
        writeLandLines(buffer, ownLandOffers);
        writeLandLines(buffer, partnerLandOffers);
        writeChatLines(buffer, chatLines);
    }

    public String partnerId() {
        return partnerId;
    }

    public String partnerName() {
        return partnerName;
    }

    public long availableCredits() {
        return availableCredits;
    }

    public long offeredCredits() {
        return offeredCredits;
    }

    public long partnerCredits() {
        return partnerCredits;
    }

    public boolean ready() {
        return ready;
    }

    public boolean partnerReady() {
        return partnerReady;
    }

    public boolean confirmed() {
        return confirmed;
    }

    public boolean partnerConfirmed() {
        return partnerConfirmed;
    }

    public List<TradeOfferLine> ownOffers() {
        return ownOffers;
    }

    public List<TradeOfferLine> partnerOffers() {
        return partnerOffers;
    }

    public List<TradeLandLine> ownLandOffers() {
        return ownLandOffers;
    }

    public List<TradeLandLine> partnerLandOffers() {
        return partnerLandOffers;
    }

    public List<TradeChatLine> chatLines() {
        return chatLines;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static List<TradeOfferLine> readLines(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<TradeOfferLine> lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            lines.add(new TradeOfferLine(buffer.readUtf(), buffer.readVarInt()));
        }
        return lines;
    }

    private static void writeLines(FriendlyByteBuf buffer, List<TradeOfferLine> lines) {
        buffer.writeVarInt(lines.size());
        for (TradeOfferLine line : lines) {
            buffer.writeUtf(line.itemKey());
            buffer.writeVarInt(line.count());
        }
    }

    private static List<TradeLandLine> readLandLines(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<TradeLandLine> lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            lines.add(new TradeLandLine(buffer.readVarInt(), buffer.readUtf(), buffer.readLong()));
        }
        return lines;
    }

    private static void writeLandLines(FriendlyByteBuf buffer, List<TradeLandLine> lines) {
        buffer.writeVarInt(lines.size());
        for (TradeLandLine line : lines) {
            buffer.writeVarInt(line.landId());
            buffer.writeUtf(line.typeKey());
            buffer.writeLong(line.blocks());
        }
    }

    private static List<TradeChatLine> readChatLines(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<TradeChatLine> lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            lines.add(new TradeChatLine(buffer.readUtf(), buffer.readUtf(), buffer.readBoolean()));
        }
        return lines;
    }

    private static void writeChatLines(FriendlyByteBuf buffer, List<TradeChatLine> lines) {
        buffer.writeVarInt(lines.size());
        for (TradeChatLine line : lines) {
            buffer.writeUtf(line.senderName());
            buffer.writeUtf(line.message());
            buffer.writeBoolean(line.own());
        }
    }
}