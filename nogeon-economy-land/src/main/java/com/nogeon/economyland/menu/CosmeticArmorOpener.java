package com.nogeon.economyland.menu;

import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class CosmeticArmorOpener {
    private CosmeticArmorOpener() {
    }

    public static void open(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        SimpleContainer container = new SimpleContainer(PlayerProfile.COSMETIC_ARMOR_SLOTS);
        for (int i = 0; i < PlayerProfile.COSMETIC_ARMOR_SLOTS; i++) {
            container.setItem(i, profile.cosmeticArmor(i).copy());
        }
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new CosmeticArmorMenu(containerId, inventory, container, profile.cosmeticArmorVisible(), player),
            Component.literal("치장 장비")
        ), (FriendlyByteBuf buffer) -> buffer.writeBoolean(profile.cosmeticArmorVisible()));
    }
}
