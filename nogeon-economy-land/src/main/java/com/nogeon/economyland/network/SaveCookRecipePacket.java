package com.nogeon.economyland.network;

import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

public class SaveCookRecipePacket {
    private final List<String> buffs;

    public SaveCookRecipePacket(List<String> buffs) {
        this.buffs = buffs;
    }

    public static void encode(SaveCookRecipePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.buffs.size());
        for (String buff : msg.buffs) {
            buf.writeUtf(buff);
        }
    }

    public static SaveCookRecipePacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<String> buffs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            buffs.add(buf.readUtf());
        }
        return new SaveCookRecipePacket(buffs);
    }

    public static void handle(SaveCookRecipePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            
            profile.setCookRecipeBuffs(msg.buffs);
            state.setDirty();

            // 저장 성공 연출음
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.5F);
        });
        ctx.setPacketHandled(true);
    }
}
