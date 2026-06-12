package com.nogeon.economyland.shop;

import com.nogeon.economyland.network.SyncPlayerInventoryPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;

public final class ItemLockEvents {
    private ItemLockEvents() {}

    public static void onItemToss(ItemTossEvent event) {
        ItemStack tossed = event.getEntity().getItem();
        if (!ShopItemProtection.isLocked(tossed)) {
            return;
        }

        event.setCanceled(true);
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack restored = tossed.copy();
        Component itemName = restored.getHoverName();
        restore(player, restored);
        player.containerMenu.broadcastChanges();
        SyncPlayerInventoryPacket.send(player);
        player.displayClientMessage(Component.translatable(
            "message.nogeon_economy_land.item_lock.drop_blocked",
            itemName
        ), true);
    }

    private static void restore(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (stack.isEmpty()) {
            return;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setItem(slot, stack.copy());
                stack.setCount(0);
                return;
            }
        }
    }
}
