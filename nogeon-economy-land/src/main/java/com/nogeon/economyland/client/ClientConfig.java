package com.nogeon.economyland.client;

import com.nogeon.economyland.NoGeonEconomyLand;
import java.io.*;
import java.util.Properties;

public final class ClientConfig {
    private static final File CONFIG_FILE = new File("config", "nogeon_economy_land_client.properties");
    private static final Properties PROPERTIES = new Properties();

    public static boolean weaponVfx = true;
    public static boolean armorVfx = true;
    public static boolean hitVfx = true;
    public static boolean soundVfx = true;
    public static boolean safetyLock = true;
    public static boolean itemVfx = true;

    private ClientConfig() {
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            PROPERTIES.load(in);
            weaponVfx = Boolean.parseBoolean(PROPERTIES.getProperty("weaponVfx", "true"));
            armorVfx = Boolean.parseBoolean(PROPERTIES.getProperty("armorVfx", "true"));
            hitVfx = Boolean.parseBoolean(PROPERTIES.getProperty("hitVfx", "true"));
            soundVfx = Boolean.parseBoolean(PROPERTIES.getProperty("soundVfx", "true"));
            safetyLock = Boolean.parseBoolean(PROPERTIES.getProperty("safetyLock", "true"));
            itemVfx = Boolean.parseBoolean(PROPERTIES.getProperty("itemVfx", "true"));
        } catch (IOException e) {
            NoGeonEconomyLand.LOGGER.error("Failed to load client config", e);
        }
    }

    public static void save() {
        PROPERTIES.setProperty("weaponVfx", String.valueOf(weaponVfx));
        PROPERTIES.setProperty("armorVfx", String.valueOf(armorVfx));
        PROPERTIES.setProperty("hitVfx", String.valueOf(hitVfx));
        PROPERTIES.setProperty("soundVfx", String.valueOf(soundVfx));
        PROPERTIES.setProperty("safetyLock", String.valueOf(safetyLock));
        PROPERTIES.setProperty("itemVfx", String.valueOf(itemVfx));
        
        File parent = CONFIG_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            PROPERTIES.store(out, "NoGeon Economy Land Client Configuration");
        } catch (IOException e) {
            NoGeonEconomyLand.LOGGER.error("Failed to save client config", e);
        }
    }
}
