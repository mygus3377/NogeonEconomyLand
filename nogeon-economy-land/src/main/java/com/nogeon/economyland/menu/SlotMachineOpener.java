package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.SlotMachineResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class SlotMachineOpener {
    private SlotMachineOpener() {
    }

    public static void openSetup(ServerPlayer player) {
        open(player, new SlotMachineResult(0L, 0, 1, 2, 0L, ""));
    }

    public static void open(ServerPlayer player, SlotMachineResult result) {
        com.nogeon.economyland.state.EconomyState state = com.nogeon.economyland.state.EconomyState.get(player.server);
        com.nogeon.economyland.player.PlayerProfile profile = state.profile(player.getUUID());
        int streak = profile.gambleStreak();
        com.nogeon.economyland.player.SocialClass socialClass = profile.socialClass();

        SlotMachineMenu snapshot = new SlotMachineMenu(0, result.stake(), result.leftSymbol(), result.middleSymbol(),
            result.rightSymbol(), result.payout(), result.resultKey(), streak, socialClass);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new SlotMachineMenu(containerId, result.stake(), result.leftSymbol(), result.middleSymbol(),
                result.rightSymbol(), result.payout(), result.resultKey(), streak, socialClass),
            Component.translatable("screen.nogeon_economy_land.slot_machine")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
