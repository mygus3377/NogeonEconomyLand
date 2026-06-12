package com.nogeon.economyland.menu;

import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class SkillsOpener {
    private SkillsOpener() {
    }

    public static void open(ServerPlayer player) {
        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
        SkillsMenu snapshot = new SkillsMenu(0, profile);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new SkillsMenu(containerId, profile),
            Component.translatable("screen.nogeon_economy_land.skills")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
