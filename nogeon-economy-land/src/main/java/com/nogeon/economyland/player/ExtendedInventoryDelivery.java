package com.nogeon.economyland.player;

import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.shop.ShopItemProtection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ExtendedInventoryDelivery {
    private static final int MAX_SLOTS = 270;

    private ExtendedInventoryDelivery() {
    }

    public static boolean give(ServerPlayer player, ItemStack stack) {
        return giveRemainder(player, stack).isEmpty();
    }

    public static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        ItemStack remainder = giveRemainder(player, stack);
        if (!remainder.isEmpty()) {
            player.drop(remainder, false);
        }
    }

    public static ItemStack giveRemainder(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = stack.copy();
        player.getInventory().add(remainder);
        if (!remainder.isEmpty()) {
            storeOverflow(player, remainder);
        }
        return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
    }

    private static void storeOverflow(ServerPlayer player, ItemStack remainder) {
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        int unlockedSlots = Math.min(MAX_SLOTS, Math.max(0, profile.inventoryExtLevel() * 9));
        if (unlockedSlots <= 0) {
            return;
        }

        ItemStack[] items = load(profile.extInventoryData());
        boolean changed = merge(items, unlockedSlots, remainder);
        changed |= fillEmpty(items, unlockedSlots, remainder);
        if (changed) {
            profile.setExtInventoryData(save(items));
            state.setDirty();
        }
    }

    private static boolean merge(ItemStack[] items, int unlockedSlots, ItemStack remainder) {
        boolean changed = false;
        for (int slot = 0; slot < unlockedSlots && !remainder.isEmpty(); slot++) {
            ItemStack stored = items[slot];
            if (stored.isEmpty() || !ItemStack.isSameItemSameTags(stored, remainder)) {
                continue;
            }
            int maxCount = Math.min(stored.getMaxStackSize(), remainder.getMaxStackSize());
            int moved = Math.min(remainder.getCount(), maxCount - stored.getCount());
            if (moved > 0) {
                stored.grow(moved);
                remainder.shrink(moved);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean fillEmpty(ItemStack[] items, int unlockedSlots, ItemStack remainder) {
        boolean changed = false;
        for (int slot = 0; slot < unlockedSlots && !remainder.isEmpty(); slot++) {
            if (!items[slot].isEmpty()) {
                continue;
            }
            int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
            ItemStack stored = remainder.copy();
            stored.setCount(moved);
            items[slot] = stored;
            remainder.shrink(moved);
            changed = true;
        }
        return changed;
    }

    public static ItemStack[] load(CompoundTag data) {
        ItemStack[] items = new ItemStack[MAX_SLOTS];
        for (int slot = 0; slot < items.length; slot++) {
            items[slot] = ItemStack.EMPTY;
        }
        if (data == null || !data.contains("Items", Tag.TAG_LIST)) {
            return items;
        }
        ListTag list = data.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag itemNbt = list.getCompound(index);
            int slot = itemNbt.getInt("Slot");
            if (slot >= 0 && slot < items.length) {
                items[slot] = ItemStack.of(itemNbt);
            }
        }
        return items;
    }

    public static CompoundTag save(ItemStack[] items) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (int slot = 0; slot < items.length; slot++) {
            ItemStack stack = items[slot];
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag itemNbt = new CompoundTag();
            itemNbt.putInt("Slot", slot);
            stack.save(itemNbt);
            list.add(itemNbt);
        }
        data.put("Items", list);
        return data;
    }

    public static java.util.List<ItemStack> findAllBackpacks(net.minecraft.world.entity.player.Player player) {
        java.util.List<ItemStack> backpacks = new java.util.ArrayList<>();
        
        // 1. Curios 슬롯 스캔 (리플렉션)
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object helper = curiosApiClass.getMethod("getCuriosHelper").invoke(null);
            Object equipped = helper.getClass()
                .getMethod("getEquippedCurios", net.minecraft.world.entity.LivingEntity.class)
                .invoke(helper, player);
            addBackpacksFromHandler(backpacks, unwrapOptionalOrLazy(equipped));

            Object curiosInventory = helper.getClass()
                .getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class)
                .invoke(helper, player);
            Object unwrappedInventory = unwrapOptionalOrLazy(curiosInventory);
            if (unwrappedInventory != null) {
                Object curiosMap = unwrappedInventory.getClass().getMethod("getCurios").invoke(unwrappedInventory);
                if (curiosMap instanceof java.util.Map<?, ?> map) {
                    for (Object slotInventory : map.values()) {
                        Object stacks = slotInventory.getClass().getMethod("getStacks").invoke(slotInventory);
                        addBackpacksFromHandler(backpacks, stacks);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 2. 흉갑 슬롯 스캔
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent()) {
            boolean duplicate = false;
            for (ItemStack bp : backpacks) {
                if (bp == chest) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                backpacks.add(chest);
            }
        }

        // 3. 일반 인벤토리 스캔 (장착 장비/보조손 제외)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                boolean duplicate = false;
                for (ItemStack bp : backpacks) {
                    if (bp == stack) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    backpacks.add(stack);
                }
            }
        }

        return backpacks;
    }

    private static Object unwrapOptionalOrLazy(Object value) {
        if (value instanceof java.util.Optional<?> optional) {
            return optional.orElse(null);
        }
        if (value != null) {
            try {
                return value.getClass().getMethod("orElse", Object.class).invoke(value, new Object[] { null });
            } catch (Exception ignored) {
            }
        }
        return value;
    }

    private static void addBackpacksFromHandler(java.util.List<ItemStack> backpacks, Object handlerObject) {
        if (!(handlerObject instanceof net.minecraftforge.items.IItemHandler handler)) {
            return;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            addBackpackIfPresent(backpacks, handler.getStackInSlot(slot));
        }
    }

    private static void addBackpackIfPresent(java.util.List<ItemStack> backpacks, ItemStack stack) {
        if (stack.isEmpty() || !stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent()) {
            return;
        }
        for (ItemStack backpack : backpacks) {
            if (backpack == stack) {
                return;
            }
        }
        backpacks.add(stack);
    }

    public static int countAllOwned(net.minecraft.world.entity.player.Player player, ItemStack target) {
        return countAllOwned(player, target, com.nogeon.economyland.entity.TraderKind.GENERAL);
    }

    public static int countAllOwnedClient(net.minecraft.world.entity.player.Player player, ItemStack target, CompoundTag extInventoryNbt) {
        return countAllOwnedClient(player, target, extInventoryNbt, "");
    }

    public static int consumeAllOwned(ServerPlayer player, ItemStack target, int amount) {
        return consumeAllOwned(player, target, amount, com.nogeon.economyland.entity.TraderKind.GENERAL);
    }

    public static int countAllOwned(net.minecraft.world.entity.player.Player player, ItemStack target, com.nogeon.economyland.entity.TraderKind kind) {
        if (target.isEmpty()) return 0;
        int count = 0;

        // 1. 일반 인벤토리 검사 (장착 장비/보조손 36~40 슬롯 제외)
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            if (!ShopItemProtection.isSellBlocked(stack) && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, target, kind)) {
                count += stack.getCount();
            }
        }

        // 2. 장착했거나 소지한 모든 가방(Backpack) 내부 검사
        for (ItemStack backpack : findAllBackpacks(player)) {
            count += countInBackpack(backpack, target, kind);
        }

        // 3. 확장 보관함(Extended Inventory) 검사
        if (player instanceof ServerPlayer serverPlayer) {
            EconomyState state = EconomyState.get(serverPlayer.server);
            PlayerProfile profile = state.profile(serverPlayer.getUUID());
            ItemStack[] extItems = load(profile.extInventoryData());
            int unlockedSlots = Math.min(MAX_SLOTS, Math.max(0, profile.inventoryExtLevel() * 9));
            for (int slot = 0; slot < unlockedSlots; slot++) {
                ItemStack stack = extItems[slot];
                if (!stack.isEmpty() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, target, kind)) {
                    count += stack.getCount();
                }
            }
        }

        return count;
    }

    public static int countAllOwnedClient(net.minecraft.world.entity.player.Player player, ItemStack target, CompoundTag extInventoryNbt, String kindId) {
        com.nogeon.economyland.entity.TraderKind kind = (kindId == null || kindId.isEmpty()) ? com.nogeon.economyland.entity.TraderKind.GENERAL : com.nogeon.economyland.entity.TraderKind.byId(kindId);
        if (target.isEmpty()) return 0;
        int count = 0;

        // 1. 일반 인벤토리 검사 (장착 장비/보조손 36~40 슬롯 제외)
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            if (!ShopItemProtection.isSellBlocked(stack) && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, target, kind)) {
                count += stack.getCount();
            }
        }

        // 2. 장착했거나 소지한 모든 가방 내부 검사
        for (ItemStack backpack : findAllBackpacks(player)) {
            count += countInBackpack(backpack, target, kind);
        }

        // 3. 확장 보관함 검사 (클라이언트 측 NBT 연동)
        if (extInventoryNbt != null) {
            ItemStack[] extItems = load(extInventoryNbt);
            for (ItemStack stack : extItems) {
                if (stack != null && !stack.isEmpty() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, target, kind)) {
                    count += stack.getCount();
                }
            }
        }

        return count;
    }

    public static int countInBackpack(ItemStack bagStack, ItemStack target) {
        return countInBackpack(bagStack, target, com.nogeon.economyland.entity.TraderKind.GENERAL);
    }

    public static int countInBackpack(ItemStack bagStack, ItemStack target, com.nogeon.economyland.entity.TraderKind kind) {
        if (bagStack.isEmpty()) return 0;
        int count = 0;
        var cap = bagStack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        if (cap.isPresent()) {
            net.minecraftforge.items.IItemHandler handler = cap.orElse(null);
            if (handler != null) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stored = handler.getStackInSlot(slot);
                    if (!stored.isEmpty() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stored, target, kind)) {
                        count += stored.getCount();
                    }
                }
            }
        }
        return count;
    }

    public static int consumeAllOwned(ServerPlayer player, ItemStack target, int amount, com.nogeon.economyland.entity.TraderKind kind) {
        if (target.isEmpty() || amount <= 0) return 0;
        int remaining = amount;

        // 1. 일반 인벤토리에서 소비 (장착 장비/보조손 36~40 슬롯 제외)
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && !ShopItemProtection.isSellBlocked(stack) && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, target, kind)) {
                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                remaining -= removed;
            }
        }

        // 2. 확장 보관함에서 소비
        if (remaining > 0) {
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            ItemStack[] extItems = load(profile.extInventoryData());
            int unlockedSlots = Math.min(MAX_SLOTS, Math.max(0, profile.inventoryExtLevel() * 9));
            boolean changed = false;
            for (int slot = 0; slot < unlockedSlots && remaining > 0; slot++) {
                ItemStack stored = extItems[slot];
                if (!stored.isEmpty() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stored, target, kind)) {
                    int removed = Math.min(remaining, stored.getCount());
                    stored.shrink(removed);
                    if (stored.isEmpty()) {
                        extItems[slot] = ItemStack.EMPTY;
                    }
                    remaining -= removed;
                    changed = true;
                }
            }
            if (changed) {
                profile.setExtInventoryData(save(extItems));
                state.setDirty();
            }
        }

        // 3. 장착했거나 소지한 모든 가방(Backpack) 내부에서 소비
        if (remaining > 0) {
            for (ItemStack backpack : findAllBackpacks(player)) {
                if (remaining <= 0) break;
                remaining = consumeInBackpack(backpack, target, remaining, kind);
            }
        }

        return amount - remaining;
    }

    public static int consumeInBackpack(ItemStack bagStack, ItemStack target, int amount) {
        return consumeInBackpack(bagStack, target, amount, com.nogeon.economyland.entity.TraderKind.GENERAL);
    }

    public static int consumeInBackpack(ItemStack bagStack, ItemStack target, int amount, com.nogeon.economyland.entity.TraderKind kind) {
        if (bagStack.isEmpty() || amount <= 0) return amount;
        int remaining = amount;
        var cap = bagStack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        if (cap.isPresent()) {
            net.minecraftforge.items.IItemHandler handler = cap.orElse(null);
            if (handler instanceof net.minecraftforge.items.IItemHandlerModifiable modifiable) {
                for (int slot = 0; slot < modifiable.getSlots() && remaining > 0; slot++) {
                    ItemStack stored = modifiable.getStackInSlot(slot);
                    if (!stored.isEmpty() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stored, target, kind)) {
                        int removed = Math.min(remaining, stored.getCount());
                        stored.shrink(removed);
                        if (stored.isEmpty()) {
                            modifiable.setStackInSlot(slot, ItemStack.EMPTY);
                        } else {
                            modifiable.setStackInSlot(slot, stored);
                        }
                        remaining -= removed;
                    }
                }
            }
        }
        return remaining;
    }
}
