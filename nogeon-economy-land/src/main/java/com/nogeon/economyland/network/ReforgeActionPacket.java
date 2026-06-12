package com.nogeon.economyland.network;

import com.nogeon.economyland.item.ReforgeService;
import com.nogeon.economyland.menu.ReforgeOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class ReforgeActionPacket {
    private final String action;
    private final int slot;
    private final int reforgeSlotIndex;

    public ReforgeActionPacket(String action, int slot, int reforgeSlotIndex) {
        this.action = action;
        this.slot = slot;
        this.reforgeSlotIndex = reforgeSlotIndex;
    }

    public static void encode(ReforgeActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action);
        buffer.writeVarInt(packet.slot);
        buffer.writeVarInt(packet.reforgeSlotIndex);
    }

    public static ReforgeActionPacket decode(FriendlyByteBuf buffer) {
        return new ReforgeActionPacket(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(ReforgeActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;

            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            int selectedSlot = ReforgeService.normalizeSelectedSlot(sender, packet.slot);
            ItemStack stack = selectedSlot < 0 ? ItemStack.EMPTY : sender.getInventory().getItem(selectedSlot);
            boolean migrated = ReforgeService.migrateBalance(stack);

            if (stack.isEmpty()) {
                ReforgeOpener.open(sender, selectedSlot, Component.translatable("message.nogeon_economy_land.reforge.invalid_item"));
                return;
            }

            Component status = null;
            switch (packet.action) {
                case "unlock":
                    status = ReforgeService.tryUnlock(sender, profile, stack);
                    break;
                case "roll":
                    status = ReforgeService.tryRoll(sender, profile, stack, false);
                    break;
                case "roll_silent":
                    status = ReforgeService.tryRoll(sender, profile, stack, true);
                    break;
                case "lock":
                    ReforgeService.toggleLock(stack, packet.reforgeSlotIndex);
                    break;
            }

            SyncCreditsPacket.send(sender, profile.credits());
            SyncPlayerInventoryPacket.send(sender);
            sender.inventoryMenu.broadcastChanges();
            state.setDirty();
            
            SyncReforgeStatusPacket.send(sender, status);
        });
        context.setPacketHandled(true);
    }
}
