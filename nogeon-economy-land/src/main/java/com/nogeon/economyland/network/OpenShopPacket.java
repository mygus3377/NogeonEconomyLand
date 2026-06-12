package com.nogeon.economyland.network;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.menu.ShopOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenShopPacket {
    private final String kindId;

    public OpenShopPacket() {
        this.kindId = "general";
    }

    public OpenShopPacket(String kindId) {
        this.kindId = kindId;
    }

    public static void encode(OpenShopPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.kindId);
    }

    public static OpenShopPacket decode(FriendlyByteBuf buffer) {
        return new OpenShopPacket(buffer.readUtf());
    }

    public static void handle(OpenShopPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                ShopOpener.open(sender, TraderKind.byId(packet.kindId));
            }
        });
        context.setPacketHandled(true);
    }
}
