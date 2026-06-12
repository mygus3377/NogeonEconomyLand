package com.nogeon.economyland.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import com.nogeon.economyland.client.ClientPacketHandler;

public class OpenCookRecipeScreenPacket {
    private final int maxSlots;
    private final List<String> selectedBuffs;

    public OpenCookRecipeScreenPacket(int maxSlots, List<String> selectedBuffs) {
        this.maxSlots = maxSlots;
        this.selectedBuffs = selectedBuffs;
    }

    public static void encode(OpenCookRecipeScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.maxSlots);
        buf.writeInt(msg.selectedBuffs.size());
        for (String buff : msg.selectedBuffs) {
            buf.writeUtf(buff);
        }
    }

    public static OpenCookRecipeScreenPacket decode(FriendlyByteBuf buf) {
        int maxSlots = buf.readInt();
        int size = buf.readInt();
        List<String> selectedBuffs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            selectedBuffs.add(buf.readUtf());
        }
        return new OpenCookRecipeScreenPacket(maxSlots, selectedBuffs);
    }

    public static void handle(OpenCookRecipeScreenPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenCookRecipeScreen(msg.maxSlots, msg.selectedBuffs));
        });
        ctx.setPacketHandled(true);
    }
}
