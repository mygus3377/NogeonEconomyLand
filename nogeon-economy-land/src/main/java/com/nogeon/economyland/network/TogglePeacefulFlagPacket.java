package com.nogeon.economyland.network;

import com.nogeon.economyland.player.PvpFlagBridge;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TogglePeacefulFlagPacket {
    public static void encode(TogglePeacefulFlagPacket packet, FriendlyByteBuf buffer) {
    }

    public static TogglePeacefulFlagPacket decode(FriendlyByteBuf buffer) {
        return new TogglePeacefulFlagPacket();
    }

    public static void handle(TogglePeacefulFlagPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            boolean peaceful = !profile.peacefulFlag();
            boolean updated = PvpFlagBridge.setPvpEnabled(player, !peaceful);
            if (updated) {
                profile.setPeacefulFlag(peaceful);
                state.setDirty();
            }
            String key = !updated
                ? "message.nogeon_economy_land.peaceful_flag.unavailable"
                : peaceful
                    ? "message.nogeon_economy_land.peaceful_flag.enabled"
                    : "message.nogeon_economy_land.peaceful_flag.disabled";
            player.displayClientMessage(Component.translatable(key), false);
        });
        context.setPacketHandled(true);
    }
}
