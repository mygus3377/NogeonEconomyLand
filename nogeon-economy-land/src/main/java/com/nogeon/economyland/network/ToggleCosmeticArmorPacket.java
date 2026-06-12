package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.CosmeticArmorMenu;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class ToggleCosmeticArmorPacket {
    private final boolean visible;

    public ToggleCosmeticArmorPacket(boolean visible) {
        this.visible = visible;
    }

    public static void encode(ToggleCosmeticArmorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.visible);
    }

    public static ToggleCosmeticArmorPacket decode(FriendlyByteBuf buffer) {
        return new ToggleCosmeticArmorPacket(buffer.readBoolean());
    }

    public static void handle(ToggleCosmeticArmorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            profile.setCosmeticArmorVisible(packet.visible);
            if (sender.containerMenu instanceof CosmeticArmorMenu menu) {
                menu.setVisible(packet.visible);
                menu.saveToProfile(sender);
                return;
            }
            state.setDirty();
            SyncCosmeticArmorPacket.broadcast(sender.server, sender.getUUID(), profile);
        });
        context.setPacketHandled(true);
    }
}
