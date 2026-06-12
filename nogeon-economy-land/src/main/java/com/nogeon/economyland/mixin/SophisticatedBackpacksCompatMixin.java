package com.nogeon.economyland.mixin;

import com.mafuyu404.taczaddon.compat.SophisticatedBackpacksCompatInner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.UUID;

@Mixin(value = SophisticatedBackpacksCompatInner.class, remap = false)
public class SophisticatedBackpacksCompatMixin {

    @Unique
    private static final UUID NOGEON$DUMMY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Unique
    private static java.lang.reflect.Field nogeon$backpackUuidField;

    static {
        try {
            nogeon$backpackUuidField = BackpackContentsMessage.class.getDeclaredField("backpackUuid");
            nogeon$backpackUuidField.setAccessible(true);
        } catch (Exception e) {
            nogeon$backpackUuidField = null;
        }
    }

    @Redirect(
        method = "modifyBackpack",
        at = @At(value = "INVOKE", target = "Ljava/util/Optional;get()Ljava/lang/Object;"),
        remap = false
    )
    private static Object nogeon$redirectGet(Optional<UUID> optional) {
        if (optional.isPresent()) {
            return optional.get();
        }
        return NOGEON$DUMMY_UUID;
    }

    @Redirect(
        method = "modifyBackpack",
        at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedbackpacks/backpack/BackpackStorage;getOrCreateBackpackContents(Ljava/util/UUID;)Lnet/minecraft/nbt/CompoundTag;"),
        remap = false
    )
    private static CompoundTag nogeon$redirectGetOrCreateBackpackContents(BackpackStorage instance, UUID uuid) {
        if (NOGEON$DUMMY_UUID.equals(uuid)) {
            return new CompoundTag();
        }
        return instance.getOrCreateBackpackContents(uuid);
    }

    @Redirect(
        method = "modifyBackpack",
        at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedbackpacks/network/SBPPacketHandler;sendToClient(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/Object;)V"),
        remap = false
    )
    private static void nogeon$redirectSendToClient(SBPPacketHandler instance, ServerPlayer player, Object message) {
        if (message instanceof BackpackContentsMessage backpackMessage && nogeon$backpackUuidField != null) {
            try {
                UUID uuid = (UUID) nogeon$backpackUuidField.get(backpackMessage);
                if (NOGEON$DUMMY_UUID.equals(uuid)) {
                    // Skip sending empty dummy contents to the client
                    return;
                }
            } catch (Exception e) {
                // Ignore and proceed
            }
        }
        instance.sendToClient(player, message);
    }
}
