package com.nogeon.economyland.menu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DroneStorageMenu extends AbstractContainerMenu {
    private final int level;
    private final SimpleContainer container;

    // Client-side Constructor
    public DroneStorageMenu(int containerId, Inventory playerInv, FriendlyByteBuf buffer) {
        this(containerId, playerInv, buffer.readVarInt());
    }

    // Common Constructor
    public DroneStorageMenu(int containerId, Inventory playerInv, int level) {
        super(ModMenus.DRONE_STORAGE.get(), containerId);
        this.level = level;

        // 47 slots total: 0 = Gun, 1 = Ammo, 2~46 = Storage slots
        this.container = new SimpleContainer(47) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (!playerInv.player.level().isClientSide) {
                    saveToPlayerNbt(playerInv.player);
                }
            }
        };

        if (!playerInv.player.level().isClientSide) {
            loadFromPlayerNbt(playerInv.player);
        }

        // 1. Add Gun slot (Slot 0) -> x=44, y=18
        this.addSlot(new Slot(container, 0, 44, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return com.tacz.guns.api.item.IGun.getIGunOrNull(stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 2. Add Ammo slot (Slot 1) -> x=116, y=18
        this.addSlot(new Slot(container, 1, 116, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return com.tacz.guns.api.item.IAmmo.getIAmmoOrNull(stack) != null
                    || stack.getItem() instanceof com.tacz.guns.api.item.IAmmoBox;
            }
        });

        // 3. Add Storage slots (Slots 2~46) -> 9x5 Grid, starting x=8, y=58
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 2 + row * 9 + col;
                final int finalRow = row;
                
                this.addSlot(new Slot(container, slotIndex, 8 + col * 18, 58 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // Only active rows can accept items
                        return finalRow < level && super.mayPlace(stack);
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        // Only active rows can be picked up
                        return finalRow < level && super.mayPickup(player);
                    }
                });
            }
        }

        // 4. Add Player Inventory (Slots 9~35) -> x=8, y=156
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, 9 + row * 9 + col, 8 + col * 18, 156 + row * 18));
            }
        }

        // 5. Add Player Hotbar (Slots 0~8) -> x=8, y=214
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 214));
        }
    }

    public SimpleContainer getContainer() {
        return this.container;
    }

    public int getDroneLevel() {
        return level;
    }

    private void loadFromPlayerNbt(Player player) {
        CompoundTag persist = player.getPersistentData();
        if (persist.contains("nogeon_engineer_drone_storage_items")) {
            ListTag list = persist.getList("nogeon_engineer_drone_storage_items", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag slotTag = list.getCompound(i);
                int slotIndex = slotTag.getInt("Slot");
                if (slotIndex >= 0 && slotIndex < container.getContainerSize()) {
                    container.setItem(slotIndex, ItemStack.of(slotTag.getCompound("Item")));
                }
            }
        } else {
            // Restore legacy Gun & Ammo from persistent NBT if present
            if (persist.contains("nogeon_engineer_drone_gun")) {
                container.setItem(0, ItemStack.of(persist.getCompound("nogeon_engineer_drone_gun")));
            }
            if (persist.contains("nogeon_engineer_drone_ammo")) {
                container.setItem(1, ItemStack.of(persist.getCompound("nogeon_engineer_drone_ammo")));
            }
            saveToPlayerNbt(player);
        }
    }

    private void saveToPlayerNbt(Player player) {
        CompoundTag persist = player.getPersistentData();
        ListTag list = new ListTag();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", i);
                slotTag.put("Item", stack.save(new CompoundTag()));
                list.add(slotTag);
            }
        }
        persist.put("nogeon_engineer_drone_storage_items", list);

        // Sync back Gun NBT for support firing compatibility
        ItemStack gun = container.getItem(0);
        if (gun.isEmpty()) {
            persist.remove("nogeon_engineer_drone_gun");
        } else {
            persist.put("nogeon_engineer_drone_gun", gun.save(new CompoundTag()));
        }

        // Sync back Ammo NBT for support firing compatibility
        ItemStack ammo = container.getItem(1);
        if (ammo.isEmpty()) {
            persist.remove("nogeon_engineer_drone_ammo");
        } else {
            persist.put("nogeon_engineer_drone_ammo", ammo.save(new CompoundTag()));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            // From Drone Storage (slots 0 ~ 46) to Player Inventory (slots 47 ~ 82)
            if (index < 47) {
                if (!this.moveItemStackTo(itemstack1, 47, 83, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From Player Inventory to Drone Storage
            else {
                boolean isGun = com.tacz.guns.api.item.IGun.getIGunOrNull(itemstack1) != null;
                boolean isAmmo = com.tacz.guns.api.item.IAmmo.getIAmmoOrNull(itemstack1) != null
                    || itemstack1.getItem() instanceof com.tacz.guns.api.item.IAmmoBox;
                
                if (isGun) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isAmmo) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Try to move to active storage slots (slots 2 ~ (2 + level * 9 - 1))
                    int activeSlotsEnd = 2 + this.level * 9;
                    if (!this.moveItemStackTo(itemstack1, 2, activeSlotsEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            
            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(player, itemstack1);
        }
        
        return itemstack;
    }
}
