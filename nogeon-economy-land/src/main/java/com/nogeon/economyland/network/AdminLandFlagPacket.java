package com.nogeon.economyland.network;

import com.nogeon.economyland.land.LandFlag;
import com.nogeon.economyland.menu.AdminLandOpener;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class AdminLandFlagPacket {
    private final int landId;
    private final String flagId;
    private final boolean value;

    public AdminLandFlagPacket(int landId, String flagId, boolean value) {
        this.landId = landId;
        this.flagId = flagId;
        this.value = value;
    }

    public static void encode(AdminLandFlagPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.landId);
        buffer.writeUtf(packet.flagId);
        buffer.writeBoolean(packet.value);
    }

    public static AdminLandFlagPacket decode(FriendlyByteBuf buffer) {
        return new AdminLandFlagPacket(buffer.readVarInt(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(AdminLandFlagPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }
            LandFlag flag = LandFlag.byId(packet.flagId);
            EconomyState.get(player.server).setAdminLandFlag(packet.landId, flag, packet.value);
            AdminLandOpener.open(player);
        });
        context.setPacketHandled(true);
    }
}
