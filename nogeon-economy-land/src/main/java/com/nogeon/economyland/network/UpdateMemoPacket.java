package com.nogeon.economyland.network;

import com.nogeon.economyland.land.LandRegion;
import com.nogeon.economyland.player.HomeEntry;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class UpdateMemoPacket {
    private final String target; // "land" or "home"
    private final String nameOrId;
    private final String memo;

    public UpdateMemoPacket(String target, String nameOrId, String memo) {
        this.target = target;
        this.nameOrId = nameOrId;
        this.memo = memo;
    }

    public static void encode(UpdateMemoPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.target);
        buffer.writeUtf(packet.nameOrId);
        buffer.writeUtf(packet.memo);
    }

    public static UpdateMemoPacket decode(FriendlyByteBuf buffer) {
        return new UpdateMemoPacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(UpdateMemoPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());

            if ("home".equals(packet.target)) {
                HomeEntry home = profile.homes().get(packet.nameOrId);
                if (home != null) {
                    home.setMemo(packet.memo);
                    state.setDirty();
                }
            } else if ("land".equals(packet.target)) {
                try {
                    int landId = Integer.parseInt(packet.nameOrId);
                    LandRegion land = state.landById(landId);
                    if (land != null && land.owner().equals(player.getUUID())) {
                        land.setMemo(packet.memo);
                        state.setDirty();
                    }
                } catch (NumberFormatException ignored) {}
            }
        });
        context.setPacketHandled(true);
    }
}
