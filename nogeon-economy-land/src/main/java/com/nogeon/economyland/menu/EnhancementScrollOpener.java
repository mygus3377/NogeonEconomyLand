package com.nogeon.economyland.menu;

import com.nogeon.economyland.item.SmithingService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class EnhancementScrollOpener {
    private EnhancementScrollOpener() {
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status) {
        int resolvedSlot = SmithingService.normalizeSelectedSlot(player, selectedSlot);
        Component resolvedStatus = status == null
            ? SmithingService.defaultStatus(SmithingService.stackForSlot(player, resolvedSlot))
            : status;
        EnhancementScrollMenu snapshot = new EnhancementScrollMenu(0, resolvedSlot, resolvedStatus);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new EnhancementScrollMenu(containerId, resolvedSlot, resolvedStatus),
            Component.translatable("action.nogeon_economy_land.smith_scrolls")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
