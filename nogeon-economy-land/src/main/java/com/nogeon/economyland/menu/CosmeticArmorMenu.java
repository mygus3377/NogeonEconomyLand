package com.nogeon.economyland.menu;

import com.nogeon.economyland.network.SyncCosmeticArmorPacket;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public final class CosmeticArmorMenu extends AbstractContainerMenu {
    public static final int[] COSMETIC_SLOT_X = { 14, 42, 70, 98 };
    public static final int COSMETIC_SLOT_Y = 34;
    private final Container cosmeticArmor;
    private final ServerPlayer owner;
    private boolean visible;

    public CosmeticArmorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        super(ModMenus.COSMETIC_ARMOR.get(), containerId);
        this.cosmeticArmor = new SimpleContainer(PlayerProfile.COSMETIC_ARMOR_SLOTS);
        this.owner = null;
        this.visible = buffer.readBoolean();
        addCosmeticSlots();
        addPlayerInventorySlots(playerInventory);
    }

    public CosmeticArmorMenu(int containerId, Inventory playerInventory, Container cosmeticArmor, boolean visible, ServerPlayer owner) {
        super(ModMenus.COSMETIC_ARMOR.get(), containerId);
        this.cosmeticArmor = cosmeticArmor;
        this.owner = owner;
        this.visible = visible;
        addCosmeticSlots();
        addPlayerInventorySlots(playerInventory);
    }

    private void addCosmeticSlots() {
        for (int i = 0; i < PlayerProfile.COSMETIC_ARMOR_SLOTS; i++) {
            this.addSlot(new CosmeticSlot(cosmeticArmor, i, COSMETIC_SLOT_X[i], COSMETIC_SLOT_Y, slotToEquipment(i)));
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public boolean visible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (owner != null && container == cosmeticArmor) {
            saveToProfile(owner);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            saveToProfile(serverPlayer);
        }
    }

    public void saveToProfile(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        for (int i = 0; i < PlayerProfile.COSMETIC_ARMOR_SLOTS; i++) {
            profile.setCosmeticArmor(i, cosmeticArmor.getItem(i));
        }
        profile.setCosmeticArmorVisible(visible);
        state.setDirty();
        SyncCosmeticArmorPacket.broadcast(player.server, player.getUUID(), profile);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack moving = slot.getItem();
        result = moving.copy();
        if (index < PlayerProfile.COSMETIC_ARMOR_SLOTS) {
            if (!this.moveItemStackTo(moving, PlayerProfile.COSMETIC_ARMOR_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveArmorToCosmeticSlot(moving)) {
            return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    private boolean moveArmorToCosmeticSlot(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            return false;
        }
        int target = equipmentToSlot(armorItem.getEquipmentSlot());
        if (target < 0) {
            return false;
        }
        return this.moveItemStackTo(stack, target, target + 1, false);
    }

    private static EquipmentSlot slotToEquipment(int slot) {
        return switch (slot) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    private static int equipmentToSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> -1;
        };
    }

    private static final class CosmeticSlot extends Slot {
        private final EquipmentSlot equipmentSlot;

        private CosmeticSlot(Container container, int slot, int x, int y, EquipmentSlot equipmentSlot) {
            super(container, slot, x, y);
            this.equipmentSlot = equipmentSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == equipmentSlot;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
