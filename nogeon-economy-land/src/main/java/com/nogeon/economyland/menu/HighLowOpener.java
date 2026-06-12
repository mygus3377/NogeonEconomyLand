package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.HighLowSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class HighLowOpener {
    private HighLowOpener() {
    }

    public static void openSetup(ServerPlayer player) {
        com.nogeon.economyland.state.EconomyState state = com.nogeon.economyland.state.EconomyState.get(player.server);
        com.nogeon.economyland.player.PlayerProfile profile = state.profile(player.getUUID());
        int streak = profile.gambleStreak();
        com.nogeon.economyland.player.SocialClass socialClass = profile.socialClass();

        HighLowMenu snapshot = new HighLowMenu(0, 0L, "", "", 0, 0, 0L, 0L, false, false, false, "gui.nogeon_economy_land.blackjack_status_ready", streak, socialClass);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new HighLowMenu(containerId, 0L, "", "", 0, 0, 0L, 0L, false, false, false, "gui.nogeon_economy_land.blackjack_status_ready", streak, socialClass),
            Component.translatable("screen.nogeon_economy_land.high_low")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }

    public static void open(ServerPlayer player, HighLowSession session) {
        com.nogeon.economyland.state.EconomyState state = com.nogeon.economyland.state.EconomyState.get(player.server);
        com.nogeon.economyland.player.PlayerProfile profile = state.profile(player.getUUID());
        int streak = profile.gambleStreak();
        com.nogeon.economyland.player.SocialClass socialClass = profile.socialClass();

        HighLowMenu snapshot = new HighLowMenu(0, session.stake(), session.playerCardsString(), session.dealerCardsString(),
            session.playerCards().size(), 0, session.payout(), 0L,
            session.canHit(), session.canDoubleDown(player), session.canStand(), session.statusKey(), streak, socialClass);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new HighLowMenu(containerId, session.stake(), session.playerCardsString(), session.dealerCardsString(),
                session.playerCards().size(), 0, session.payout(), 0L,
                session.canHit(), session.canDoubleDown(player), session.canStand(), session.statusKey(), streak, socialClass),
            Component.translatable("screen.nogeon_economy_land.high_low")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
