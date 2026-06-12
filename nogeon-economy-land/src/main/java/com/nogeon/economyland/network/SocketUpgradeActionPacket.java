package com.nogeon.economyland.network;

import com.nogeon.economyland.item.SocketUpgradeService;
import com.nogeon.economyland.menu.SocketUpgradeOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class SocketUpgradeActionPacket {
    private final int slot;

    public SocketUpgradeActionPacket(int slot) {
        this.slot = slot;
    }

    public static void encode(SocketUpgradeActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
    }

    public static SocketUpgradeActionPacket decode(FriendlyByteBuf buffer) {
        return new SocketUpgradeActionPacket(buffer.readVarInt());
    }

    public static void handle(SocketUpgradeActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            int selectedSlot = SocketUpgradeService.normalizeSelectedSlot(sender, packet.slot);
            ItemStack stack = selectedSlot < 0 ? ItemStack.EMPTY : sender.getInventory().getItem(selectedSlot);
            Component status = SocketUpgradeService.tryUpgrade(sender, profile, stack);

            SyncCreditsPacket.send(sender, profile.credits());
            SyncPlayerInventoryPacket.send(sender);
            sender.inventoryMenu.broadcastChanges();
            state.setDirty();
            SocketUpgradeOpener.open(sender, selectedSlot, status);
        });
        context.setPacketHandled(true);
    }
}
