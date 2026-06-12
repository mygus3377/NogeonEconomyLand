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

public final class SocketRemoveGemPacket {
    private final int slot;
    private final int gemIndex;

    public SocketRemoveGemPacket(int slot, int gemIndex) {
        this.slot = slot;
        this.gemIndex = gemIndex;
    }

    public static void encode(SocketRemoveGemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
        buffer.writeVarInt(packet.gemIndex);
    }

    public static SocketRemoveGemPacket decode(FriendlyByteBuf buffer) {
        return new SocketRemoveGemPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SocketRemoveGemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
            Component status = SocketUpgradeService.tryRemoveGem(sender, profile, stack, packet.gemIndex);

            SyncCreditsPacket.send(sender, profile.credits());
            SyncPlayerInventoryPacket.send(sender);
            sender.inventoryMenu.broadcastChanges();
            state.setDirty();
            SocketUpgradeOpener.open(sender, selectedSlot, status);
        });
        context.setPacketHandled(true);
    }
}
