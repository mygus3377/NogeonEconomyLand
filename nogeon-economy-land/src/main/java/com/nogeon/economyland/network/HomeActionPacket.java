package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.LandHomeOpener;
import com.nogeon.economyland.player.HomeEntry;
import com.nogeon.economyland.player.HomeTeleportService;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class HomeActionPacket {
    private final String action;
    private final String name;

    public HomeActionPacket(String action, String name) {
        this.action = action;
        this.name = name;
    }

    public static void encode(HomeActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action);
        buffer.writeUtf(packet.name);
    }

    public static HomeActionPacket decode(FriendlyByteBuf buffer) {
        return new HomeActionPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(HomeActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.name.isBlank()) {
                return;
            }

            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            switch (packet.action) {
                case "save" -> save(player, state, profile, packet.name);
                case "delete" -> delete(state, profile, packet.name);
                case "go" -> go(player, profile, packet.name);
                default -> {
                }
            }
            LandHomeOpener.open(player);
        });
        context.setPacketHandled(true);
    }

    private static void save(ServerPlayer player, EconomyState state, PlayerProfile profile, String name) {
        if (!state.isHomeSaveAllowed(player.getUUID(), player.level().dimension(), player.blockPosition())) {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.home.save_outside_land"), false);
            return;
        }
        if (!profile.homes().containsKey(name) && !profile.canAddHome()) {
            player.displayClientMessage(Component.translatable("command.nogeon_economy_land.home.limit"), false);
            return;
        }
        profile.homes().put(name, HomeEntry.fromPlayer(name, player));
        state.setDirty();
    }

    private static void delete(EconomyState state, PlayerProfile profile, String name) {
        if (profile.homes().remove(name) != null) {
            state.setDirty();
        }
    }

    private static void go(ServerPlayer player, PlayerProfile profile, String name) {
        HomeEntry home = profile.homes().get(name);
        if (home == null) {
            return;
        }
        HomeTeleportService.request(player, home);
    }
}
