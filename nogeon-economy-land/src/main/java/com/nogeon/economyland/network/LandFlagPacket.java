package com.nogeon.economyland.network;

import com.nogeon.economyland.land.LandFlag;
import com.nogeon.economyland.menu.LandHomeOpener;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class LandFlagPacket {
    private final int landId;
    private final String flagId;
    private final boolean value;

    public LandFlagPacket(int landId, String flagId, boolean value) {
        this.landId = landId;
        this.flagId = flagId;
        this.value = value;
    }

    public static void encode(LandFlagPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.landId);
        buffer.writeUtf(packet.flagId);
        buffer.writeBoolean(packet.value);
    }

    public static LandFlagPacket decode(FriendlyByteBuf buffer) {
        return new LandFlagPacket(buffer.readVarInt(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(LandFlagPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            EconomyState state = EconomyState.get(player.server);
            LandFlag flag = LandFlag.byId(packet.flagId);
            if (flag != null) {
                state.setLandFlag(player.getUUID(), packet.landId, flag, packet.value);
                LandHomeOpener.open(player);
            }
        });
        context.setPacketHandled(true);
    }
}
