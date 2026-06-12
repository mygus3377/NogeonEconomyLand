package com.nogeon.economyland.network;

import com.nogeon.economyland.item.SmithingService;
import com.nogeon.economyland.menu.EnhancementScrollOpener;
import com.nogeon.economyland.menu.SmithOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class SmithActionPacket {
    private final String actionId;
    private final int slot;
    private final String entryId;
    private final boolean shopMode;
    private final boolean scrollMode;

    public SmithActionPacket(String actionId, int slot, String entryId, boolean shopMode) {
        this(actionId, slot, entryId, shopMode, false);
    }

    public SmithActionPacket(String actionId, int slot, String entryId, boolean shopMode, boolean scrollMode) {
        this.actionId = actionId;
        this.slot = slot;
        this.entryId = entryId == null ? "" : entryId;
        this.shopMode = shopMode;
        this.scrollMode = scrollMode;
    }

    public static void encode(SmithActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.actionId);
        buffer.writeVarInt(packet.slot);
        buffer.writeUtf(packet.entryId);
        buffer.writeBoolean(packet.shopMode);
        buffer.writeBoolean(packet.scrollMode);
    }

    public static SmithActionPacket decode(FriendlyByteBuf buffer) {
        return new SmithActionPacket(buffer.readUtf(), buffer.readVarInt(), buffer.readUtf(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(SmithActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            int selectedSlot = SmithingService.normalizeSelectedSlot(sender, packet.slot);
            Component status = switch (packet.actionId) {
                case "enhance" -> SmithingService.tryEnhance(sender, profile, selectedSlot, gemTier(packet.entryId));
                case "repair" -> SmithingService.tryRepair(sender, profile, selectedSlot);
                case "deconstruct" -> SmithingService.tryDeconstruct(sender, profile, selectedSlot);
                case "buy" -> SmithingService.tryBuy(sender, profile, packet.entryId);
                case "scroll_1" -> SmithingService.tryUseScroll(sender, 1, selectedSlot);
                case "scroll_2" -> SmithingService.tryUseScroll(sender, 2, selectedSlot);
                case "scroll_3" -> SmithingService.tryUseScroll(sender, 3, selectedSlot);
                case "scroll_4" -> SmithingService.tryUseScroll(sender, 4, selectedSlot);
                case "scroll_5" -> SmithingService.tryUseScroll(sender, 5, selectedSlot);
                case "scroll_6" -> SmithingService.tryUseScroll(sender, 6, selectedSlot);
                case "scroll_7" -> SmithingService.tryUseScroll(sender, 7, selectedSlot);
                case "scroll_8" -> SmithingService.tryUseScroll(sender, 8, selectedSlot);
                case "scroll_9" -> SmithingService.tryUseScroll(sender, 9, selectedSlot);
                case "scroll_10" -> SmithingService.tryUseScroll(sender, 10, selectedSlot);
                case "scroll_11" -> SmithingService.tryUseScroll(sender, 11, selectedSlot);
                case "scroll_12" -> SmithingService.tryUseScroll(sender, 12, selectedSlot);
                case "scroll_13" -> SmithingService.tryUseScroll(sender, 13, selectedSlot);
                case "scroll_14" -> SmithingService.tryUseScroll(sender, 14, selectedSlot);
                case "scroll_15" -> SmithingService.tryUseScroll(sender, 15, selectedSlot);
                default -> null;
            };
            SyncCreditsPacket.send(sender, profile.credits());
            SyncPlayerInventoryPacket.send(sender);
            sender.inventoryMenu.broadcastChanges();
            state.setDirty();
            if (packet.scrollMode) {
                EnhancementScrollOpener.open(sender, selectedSlot, status);
            } else {
                SmithOpener.open(sender, selectedSlot, status, packet.shopMode, false, false);
            }
        });
        context.setPacketHandled(true);
    }

    private static int gemTier(String entryId) {
        if (entryId == null || !entryId.startsWith("gem_")) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(6, Integer.parseInt(entryId.substring(4))));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
