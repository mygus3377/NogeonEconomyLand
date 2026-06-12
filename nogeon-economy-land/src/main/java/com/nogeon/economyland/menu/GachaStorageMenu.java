package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GachaStorageMenu extends AbstractContainerMenu {
    private final List<ItemStack> rewards;

    public GachaStorageMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.GACHA_STORAGE.get(), containerId);
        int count = buffer.readVarInt();
        List<ItemStack> decoded = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            decoded.add(buffer.readItem());
        }
        rewards = List.copyOf(decoded);
        addPlayerInventorySlots(inventory);
    }

    public GachaStorageMenu(int containerId, Inventory inventory, List<ItemStack> rewards) {
        super(ModMenus.GACHA_STORAGE.get(), containerId);
        this.rewards = List.copyOf(rewards);
        addPlayerInventorySlots(inventory);
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        // 일반 인벤토리 (3행 9열) - 슬롯 인덱스 9 ~ 35
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 95 + col * 18, 128 + row * 18));
            }
        }

        // 단축바 (1행 9열) - 슬롯 인덱스 0 ~ 8
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inventory, col, 95 + col * 18, 188));
        }
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(rewards.size());
        for (ItemStack reward : rewards) {
            buffer.writeItem(reward);
        }
    }

    public List<ItemStack> rewards() {
        return rewards;
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
            
            // 인벤토리(0~26) -> 단축바(27~35) 퀵무브
            if (index < 27) {
                if (!this.moveItemStackTo(itemstack1, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } 
            // 단축바(27~35) -> 인벤토리(0~26) 퀵무브
            else {
                if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
                    return ItemStack.EMPTY;
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

