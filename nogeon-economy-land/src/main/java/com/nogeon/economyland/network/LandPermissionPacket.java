package com.nogeon.economyland.network;

import com.nogeon.economyland.land.LandPermission;
import com.nogeon.economyland.menu.LandHomeOpener;
import com.nogeon.economyland.state.EconomyState;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class LandPermissionPacket {
    private final int landId;
    private final String playerName;
    private final String permissionId;

    public LandPermissionPacket(int landId, String playerName, String permissionId) {
        this.landId = landId;
        this.playerName = playerName;
        this.permissionId = permissionId;
    }

    public static void encode(LandPermissionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.landId);
        buffer.writeUtf(packet.playerName);
        buffer.writeUtf(packet.permissionId);
    }

    public static LandPermissionPacket decode(FriendlyByteBuf buffer) {
        return new LandPermissionPacket(buffer.readVarInt(), buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(LandPermissionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            EconomyState state = EconomyState.get(player.server);
            UUID target = state.findKnownPlayer(packet.playerName);
            if (target == null) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.player_not_found"), false);
                return;
            }
            boolean updated = state.setLandPermission(player.getUUID(), packet.landId, target, LandPermission.byId(packet.permissionId));
            player.displayClientMessage(Component.translatable(updated
                ? "message.nogeon_economy_land.land.permission_updated"
                : "message.nogeon_economy_land.land.permission_failed"), false);
            LandHomeOpener.open(player);
        });
        context.setPacketHandled(true);
    }
}
