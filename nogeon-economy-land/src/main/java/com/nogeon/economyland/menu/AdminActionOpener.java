package com.nogeon.economyland.menu;

import com.nogeon.economyland.entity.TraderKind;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class AdminActionOpener {
    private AdminActionOpener() {
    }

    public static void open(ServerPlayer player, TraderKind kind) {
        List<TraderActionLine> lines = TraderActionOpener.lines(kind);
        AdminActionMenu snapshot = new AdminActionMenu(0, kind.id(), lines);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new AdminActionMenu(containerId, kind.id(), lines),
            Component.translatable("screen.nogeon_economy_land.admin_action", Component.translatable(kind.translationKey()))
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
