package com.nogeon.economyland.client;

import com.nogeon.economyland.player.PlayerProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ClientCosmeticArmorData {
    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    private ClientCosmeticArmorData() {
    }

    public static void set(UUID playerId, boolean visible, ItemStack[] stacks) {
        ItemStack[] copy = new ItemStack[PlayerProfile.COSMETIC_ARMOR_SLOTS];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = i < stacks.length && stacks[i] != null ? stacks[i].copy() : ItemStack.EMPTY;
        }
        ENTRIES.put(playerId, new Entry(visible, copy));
    }

    public static boolean has(UUID playerId) {
        return ENTRIES.containsKey(playerId);
    }

    public static boolean isVisible(UUID playerId) {
        Entry entry = ENTRIES.get(playerId);
        return entry != null && entry.visible;
    }

    public static ItemStack itemFor(UUID playerId, EquipmentSlot slot) {
        Entry entry = ENTRIES.get(playerId);
        if (entry == null || !entry.visible) {
            return ItemStack.EMPTY;
        }
        int index = switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> -1;
        };
        if (index < 0 || index >= entry.stacks.length) {
            return ItemStack.EMPTY;
        }
        return entry.stacks[index];
    }

    public static ItemStack renderStackFor(UUID playerId, EquipmentSlot slot, ItemStack fallback) {
        Entry entry = ENTRIES.get(playerId);
        if (entry == null || !entry.visible) {
            return fallback;
        }
        return itemFor(playerId, slot);
    }

    private record Entry(boolean visible, ItemStack[] stacks) {
        private boolean hasAnyCosmetic() {
            for (ItemStack stack : stacks) {
                if (stack != null && !stack.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }
}
