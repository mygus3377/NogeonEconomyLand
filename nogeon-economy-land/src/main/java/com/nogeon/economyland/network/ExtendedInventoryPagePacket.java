package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.ExtendedInventoryMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class ExtendedInventoryPagePacket {
    private final int pageIndex;

    public ExtendedInventoryPagePacket(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public static void encode(ExtendedInventoryPagePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.pageIndex);
    }

    public static ExtendedInventoryPagePacket decode(FriendlyByteBuf buffer) {
        return new ExtendedInventoryPagePacket(buffer.readVarInt());
    }

    public static void handle(ExtendedInventoryPagePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            if (player.containerMenu instanceof ExtendedInventoryMenu menu) {
                menu.setCurrentPage(packet.pageIndex);
                menu.sendAllDataToRemote(); // 모든 슬롯 동기화 요청
            }
        });
        context.setPacketHandled(true);
    }
}
