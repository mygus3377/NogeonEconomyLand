package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ExtendedInventoryMenu extends AbstractContainerMenu {
    private final Container extInventory;
    private int inventoryExtLevel;
    private int currentPage;

    // 클라이언트 사이드 생성자 (FriendlyByteBuf로부터 수신)
    public ExtendedInventoryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        super(ModMenus.EXTENDED_INVENTORY.get(), containerId);
        this.inventoryExtLevel = Math.max(0, buffer.readVarInt());
        this.currentPage = clampPage(buffer.readVarInt());
        this.extInventory = new SimpleContainer(270); // 최대 10페이지 분량 확보
        
        // 슬롯 추가
        addExtendedInventorySlots();
        addPlayerInventorySlots(playerInventory);
    }

    // 서버 사이드 생성자
    public ExtendedInventoryMenu(int containerId, Inventory playerInventory, Container extInventory, int extLevel, int currentPage) {
        super(ModMenus.EXTENDED_INVENTORY.get(), containerId);
        this.extInventory = extInventory;
        this.inventoryExtLevel = Math.max(0, extLevel);
        this.currentPage = clampPage(currentPage);

        // 슬롯 추가
        addExtendedInventorySlots();
        addPlayerInventorySlots(playerInventory);
    }

    private void addExtendedInventorySlots() {
        // 9x3 뷰포트 슬롯 생성 (0 ~ 26)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                this.addSlot(new ExtendedInventorySlot(extInventory, slotIndex, 8 + col * 18, 18 + row * 18));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        // 플레이어 기본 인벤토리 (27 ~ 53)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 플레이어 핫바 (54 ~ 62)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int inventoryExtLevel() {
        return inventoryExtLevel;
    }

    public void setInventoryExtLevel(int level) {
        this.inventoryExtLevel = Math.max(0, level);
        this.currentPage = clampPage(this.currentPage);
    }

    public int currentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = clampPage(page);
        // 슬롯들의 위치에 매핑된 실질적 슬롯 인덱스가 갱신되므로, 클라이언트에 변경사항 전송 유도
    }

    public Container getExtInventory() {
        return extInventory;
    }

    private int clampPage(int page) {
        int maxPage = Math.max(1, Math.min(10, ((Math.max(0, inventoryExtLevel) - 1) / 3) + 2));
        return Math.max(0, Math.min(page, maxPage - 1));
    }

    public boolean isSlotLocked(int slotIndex) {
        if (inventoryExtLevel <= 0) {
            return true;
        }
        int globalIndex = (currentPage * 27) + slotIndex;
        return globalIndex >= (inventoryExtLevel * 9);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.nogeon.economyland.state.EconomyState state = com.nogeon.economyland.state.EconomyState.get(serverPlayer.server);
            com.nogeon.economyland.player.PlayerProfile profile = state.profile(serverPlayer.getUUID());
            
            net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < extInventory.getContainerSize(); i++) {
                ItemStack stack = extInventory.getItem(i);
                if (!stack.isEmpty()) {
                    net.minecraft.nbt.CompoundTag itemNbt = new net.minecraft.nbt.CompoundTag();
                    itemNbt.putInt("Slot", i);
                    stack.save(itemNbt);
                    listTag.add(itemNbt);
                }
            }
            nbt.put("Items", listTag);
            profile.setExtInventoryData(nbt);
            state.setDirty();
            
            com.nogeon.economyland.network.SyncCreditsPacket.send(serverPlayer, profile.credits());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // 뷰포트 슬롯(0 ~ 26)에서 플레이어 인벤토리로 이동할 때
            if (index < 27) {
                if (isSlotLocked(index)) {
                    return ItemStack.EMPTY; // 잠긴 슬롯이면 작동 불가
                }
                if (!this.moveItemStackTo(itemstack1, 27, 63, true)) {
                    return ItemStack.EMPTY;
                }
            } 
            // 플레이어 인벤토리(27 ~ 62)에서 뷰포트 슬롯으로 이동할 때
            else {
                // 잠기지 않은 슬롯 범위를 파악하여 이동시킴
                if (!moveIntoActiveViewport(itemstack1)) {
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

    /**
     * 플레이어 인벤토리에서 Shift-Click 시 잠기지 않은 활성 뷰포트 슬롯으로 우선 병합 및 삽입하는 로직
     */
    private boolean moveIntoActiveViewport(ItemStack stack) {
        // 1단계: 기존에 동일한 아이템이 존재하고 겹칠 수 있는 곳을 찾아 병합
        for (int i = 0; i < 27; i++) {
            if (isSlotLocked(i)) continue;
            Slot targetSlot = this.slots.get(i);
            ItemStack targetStack = targetSlot.getItem();
            if (!targetStack.isEmpty() && ItemStack.isSameItemSameTags(stack, targetStack)) {
                int maxCount = Math.min(targetSlot.getMaxStackSize(), targetStack.getMaxStackSize());
                int transfer = Math.min(stack.getCount(), maxCount - targetStack.getCount());
                if (transfer > 0) {
                    targetStack.grow(transfer);
                    stack.shrink(transfer);
                    targetSlot.setChanged();
                    if (stack.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        // 2단계: 남은 아이템을 잠기지 않은 비어있는 슬롯에 순차적으로 삽입
        for (int i = 0; i < 27; i++) {
            if (isSlotLocked(i)) continue;
            Slot targetSlot = this.slots.get(i);
            ItemStack targetStack = targetSlot.getItem();
            if (targetStack.isEmpty()) {
                int transfer = Math.min(stack.getCount(), targetSlot.getMaxStackSize());
                ItemStack newStack = stack.copy();
                newStack.setCount(transfer);
                stack.shrink(transfer);
                targetSlot.setByPlayer(newStack);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }

        return !stack.isEmpty();
    }

    // 커스텀 슬롯: 현재 페이지에 맞춰 실제 보관소 Container의 (currentPage * 27 + index) 위치를 접근하며 잠김을 관리함
    public final class ExtendedInventorySlot extends Slot {
        private final int viewportIndex;

        public ExtendedInventorySlot(Container container, int viewportIndex, int x, int y) {
            super(container, viewportIndex, x, y);
            this.viewportIndex = viewportIndex;
        }

        // 이 슬롯이 바라보는 보관소의 실제 물리적인 글로벌 인덱스
        public int getGlobalIndex() {
            return (currentPage * 27) + viewportIndex;
        }

        @Override
        public ItemStack getItem() {
            return this.container.getItem(getGlobalIndex());
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            this.container.setItem(getGlobalIndex(), stack);
            this.setChanged();
        }

        @Override
        public void set(ItemStack stack) {
            this.container.setItem(getGlobalIndex(), stack);
            this.setChanged();
        }

        @Override
        public void setChanged() {
            this.container.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack stack = this.container.removeItem(getGlobalIndex(), amount);
            if (!stack.isEmpty()) {
                this.setChanged();
            }
            return stack;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isSlotLocked(viewportIndex);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isSlotLocked(viewportIndex);
        }
    }
}
