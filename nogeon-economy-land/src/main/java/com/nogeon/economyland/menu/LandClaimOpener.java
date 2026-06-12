package com.nogeon.economyland.menu;

import com.nogeon.economyland.land.LandSelection;
import com.nogeon.economyland.land.LandSelectionManager;
import com.nogeon.economyland.player.SocialClass;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class LandClaimOpener {
    private LandClaimOpener() {
    }

    public static void open(ServerPlayer player) {
        LandSelection selection = LandSelectionManager.get(player);
        if (selection == null || selection.cuboids().isEmpty()) {
            return;
        }
        SocialClass socialClass = EconomyState.get(player.server).profile(player.getUUID()).socialClass();
        int discountPercent = selection.type() == com.nogeon.economyland.land.LandType.ADMIN ? 100 : socialClass.landDiscountPercent();
        long discountedPrice = selection.type() == com.nogeon.economyland.land.LandType.ADMIN ? 0L : socialClass.discountedLandPrice(selection.price());
        LandClaimMenu snapshot = new LandClaimMenu(0, LandClaimMenu.Mode.OPTIONS, selection, discountedPrice, discountPercent);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new LandClaimMenu(containerId, LandClaimMenu.Mode.OPTIONS, selection, discountedPrice, discountPercent),
            Component.translatable("screen.nogeon_economy_land.land_claim")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }

    public static void openSelectionPrompt(ServerPlayer player) {
        LandSelection selection = LandSelectionManager.get(player);
        long price = 0L;
        int discountPercent = 0;
        if (selection != null && !selection.cuboids().isEmpty()) {
            SocialClass socialClass = EconomyState.get(player.server).profile(player.getUUID()).socialClass();
            discountPercent = selection.type() == com.nogeon.economyland.land.LandType.ADMIN ? 100 : socialClass.landDiscountPercent();
            price = selection.type() == com.nogeon.economyland.land.LandType.ADMIN ? 0L : socialClass.discountedLandPrice(selection.price());
        }
        final LandSelection promptSelection = selection;
        final long promptPrice = price;
        final int promptDiscountPercent = discountPercent;
        LandClaimMenu snapshot = new LandClaimMenu(0, LandClaimMenu.Mode.PROMPT, promptSelection, promptPrice, promptDiscountPercent);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new LandClaimMenu(containerId, LandClaimMenu.Mode.PROMPT, promptSelection, promptPrice, promptDiscountPercent),
            Component.translatable("screen.nogeon_economy_land.land_selection_prompt")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
