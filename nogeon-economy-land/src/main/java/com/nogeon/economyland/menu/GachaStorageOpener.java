package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.EconomyState;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class GachaStorageOpener {
    private GachaStorageOpener() {
    }

    public static void open(ServerPlayer player) {
        List<ItemStack> rewards = EconomyState.get(player.server).pendingGachaRewards(player.getUUID());
        GachaStorageMenu snapshot = new GachaStorageMenu(0, player.getInventory(), rewards);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new GachaStorageMenu(containerId, inventory, rewards),
            Component.literal("가챠 보관함")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}

