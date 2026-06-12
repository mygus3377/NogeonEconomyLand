package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.DiceDuelResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class DiceDuelOpener {
    private DiceDuelOpener() {
    }

    public static void openSetup(ServerPlayer player) {
        open(player, new DiceDuelResult(0L, 1, 1, 1, 1, 0L, ""));
    }

    public static void open(ServerPlayer player, DiceDuelResult result) {
        com.nogeon.economyland.state.EconomyState state = com.nogeon.economyland.state.EconomyState.get(player.server);
        com.nogeon.economyland.player.PlayerProfile profile = state.profile(player.getUUID());
        int streak = profile.gambleStreak();
        com.nogeon.economyland.player.SocialClass socialClass = profile.socialClass();

        DiceDuelMenu snapshot = new DiceDuelMenu(0, result.stake(), result.playerDieOne(), result.playerDieTwo(),
            result.dealerDieOne(), result.dealerDieTwo(), result.payout(), result.resultKey(), streak, socialClass);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new DiceDuelMenu(containerId, result.stake(), result.playerDieOne(), result.playerDieTwo(),
                result.dealerDieOne(), result.dealerDieTwo(), result.payout(), result.resultKey(), streak, socialClass),
            Component.translatable("screen.nogeon_economy_land.dice_duel")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
