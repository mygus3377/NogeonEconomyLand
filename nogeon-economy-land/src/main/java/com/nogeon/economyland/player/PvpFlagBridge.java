package com.nogeon.economyland.player;

import com.nogeon.economyland.NoGeonEconomyLand;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

public final class PvpFlagBridge {
    private static final String REGISTRAR_CLASS = "io.github.realkarmakun.pvpflag.components.PlayerFlagComponentRegistrar";

    private PvpFlagBridge() {
    }

    public static boolean setPvpEnabled(ServerPlayer player, boolean enabled) {
        try {
            Class<?> registrar = Class.forName(REGISTRAR_CLASS);
            Field flagDataField = registrar.getField("FLAG_DATA");
            Object flagData = flagDataField.get(null);
            Method get = flagData.getClass().getMethod("get", Object.class);
            Object component = get.invoke(flagData, player);
            Method setState = component.getClass().getMethod("setState", boolean.class);
            setState.invoke(component, enabled);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            NoGeonEconomyLand.LOGGER.warn("Could not sync pvpflag state for {}", player.getGameProfile().getName(), exception);
            return false;
        }
    }
}
