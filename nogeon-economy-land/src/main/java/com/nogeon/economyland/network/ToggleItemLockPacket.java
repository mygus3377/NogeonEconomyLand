package com.nogeon.economyland.network;

import com.nogeon.economyland.shop.ShopItemProtection;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class ToggleItemLockPacket {
    private final int slot;

    public ToggleItemLockPacket(int slot) {
        this.slot = slot;
    }

    public static void encode(ToggleItemLockPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
    }

    public static ToggleItemLockPacket decode(FriendlyByteBuf buffer) {
        return new ToggleItemLockPacket(buffer.readVarInt());
    }

    public static void handle(ToggleItemLockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.slot < 0 || packet.slot >= player.getInventory().getContainerSize()) {
                return;
            }
            ItemStack stack = player.getInventory().getItem(packet.slot);
            if (stack.isEmpty()) {
                return;
            }
            boolean locked = ShopItemProtection.toggleLocked(stack);
            player.displayClientMessage(Component.translatable(locked
                ? "message.nogeon_economy_land.item_lock.enabled"
                : "message.nogeon_economy_land.item_lock.disabled", stack.getHoverName()), false);
            SyncPlayerInventoryPacket.send(player);
        });
        context.setPacketHandled(true);
    }
}
