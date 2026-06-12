package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class SmithMenu extends AbstractContainerMenu {
    private final int selectedSlot;
    private final boolean shopMode;
    private final boolean scrollMode;
    private final boolean deliveryMode;
    private final List<ShopLine> shopLines;
    private final List<ShopLine> deliveryLines;
    private final Component status;
    private final int lowDowngradeScrolls;
    private final int midDowngradeScrolls;
    private final int highDowngradeScrolls;
    private final int highestDowngradeScrolls;
    private final int resetProtectionScrolls;

    public SmithMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.SMITH.get(), containerId);
        selectedSlot = buffer.readVarInt();
        shopMode = buffer.readBoolean();
        scrollMode = buffer.readBoolean();
        deliveryMode = buffer.readBoolean();
        
        int shopCount = buffer.readVarInt();
        shopLines = new ArrayList<>();
        for (int index = 0; index < shopCount; index++) {
            shopLines.add(new ShopLine(buffer.readUtf(), buffer.readUtf(), buffer.readItem(), buffer.readLong(), buffer.readVarInt(), buffer.readBoolean()));
        }
        
        int deliveryCount = buffer.readVarInt();
        deliveryLines = new ArrayList<>();
        for (int index = 0; index < deliveryCount; index++) {
            deliveryLines.add(new ShopLine(buffer.readUtf(), buffer.readUtf(), buffer.readItem(), buffer.readLong(), buffer.readVarInt(), buffer.readBoolean()));
        }
        
        status = buffer.readComponent();
        lowDowngradeScrolls = buffer.readVarInt();
        midDowngradeScrolls = buffer.readVarInt();
        highDowngradeScrolls = buffer.readVarInt();
        highestDowngradeScrolls = buffer.readVarInt();
        resetProtectionScrolls = buffer.readVarInt();
    }

    public SmithMenu(int containerId, int selectedSlot, boolean shopMode, List<ShopLine> shopLines, Component status) {
        this(containerId, selectedSlot, shopMode, false, false, shopLines, List.of(), status, 0, 0, 0, 0);
    }

    public SmithMenu(int containerId, int selectedSlot, boolean shopMode, boolean scrollMode, boolean deliveryMode, List<ShopLine> shopLines, List<ShopLine> deliveryLines, Component status) {
        this(containerId, selectedSlot, shopMode, scrollMode, deliveryMode, shopLines, deliveryLines, status, 0, 0, 0, 0);
    }

    public SmithMenu(int containerId, int selectedSlot, boolean shopMode, boolean scrollMode, boolean deliveryMode, List<ShopLine> shopLines, List<ShopLine> deliveryLines, Component status,
        int lowDowngradeScrolls, int midDowngradeScrolls, int highDowngradeScrolls, int highestDowngradeScrolls) {
        this(containerId, selectedSlot, shopMode, scrollMode, deliveryMode, shopLines, deliveryLines, status,
            lowDowngradeScrolls, midDowngradeScrolls, highDowngradeScrolls, highestDowngradeScrolls, 0);
    }

    public SmithMenu(int containerId, int selectedSlot, boolean shopMode, boolean scrollMode, boolean deliveryMode, List<ShopLine> shopLines, List<ShopLine> deliveryLines, Component status,
        int lowDowngradeScrolls, int midDowngradeScrolls, int highDowngradeScrolls, int highestDowngradeScrolls, int resetProtectionScrolls) {
        super(ModMenus.SMITH.get(), containerId);
        this.selectedSlot = selectedSlot;
        this.shopMode = shopMode;
        this.scrollMode = scrollMode;
        this.deliveryMode = deliveryMode;
        this.shopLines = List.copyOf(shopLines);
        this.deliveryLines = List.copyOf(deliveryLines);
        this.status = status;
        this.lowDowngradeScrolls = lowDowngradeScrolls;
        this.midDowngradeScrolls = midDowngradeScrolls;
        this.highDowngradeScrolls = highDowngradeScrolls;
        this.highestDowngradeScrolls = highestDowngradeScrolls;
        this.resetProtectionScrolls = resetProtectionScrolls;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(selectedSlot);
        buffer.writeBoolean(shopMode);
        buffer.writeBoolean(scrollMode);
        buffer.writeBoolean(deliveryMode);
        
        buffer.writeVarInt(shopLines.size());
        for (ShopLine line : shopLines) {
            buffer.writeUtf(line.kindId());
            buffer.writeUtf(line.id());
            buffer.writeItem(line.stack());
            buffer.writeLong(line.price());
            buffer.writeVarInt(line.remaining());
            buffer.writeBoolean(line.delivery());
        }
        
        buffer.writeVarInt(deliveryLines.size());
        for (ShopLine line : deliveryLines) {
            buffer.writeUtf(line.kindId());
            buffer.writeUtf(line.id());
            buffer.writeItem(line.stack());
            buffer.writeLong(line.price());
            buffer.writeVarInt(line.remaining());
            buffer.writeBoolean(line.delivery());
        }
        
        buffer.writeComponent(status);
        buffer.writeVarInt(lowDowngradeScrolls);
        buffer.writeVarInt(midDowngradeScrolls);
        buffer.writeVarInt(highDowngradeScrolls);
        buffer.writeVarInt(highestDowngradeScrolls);
        buffer.writeVarInt(resetProtectionScrolls);
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public boolean shopMode() {
        return shopMode;
    }

    public boolean scrollMode() {
        return scrollMode;
    }

    public boolean deliveryMode() {
        return deliveryMode;
    }

    public List<ShopLine> shopLines() {
        return shopLines;
    }

    public List<ShopLine> deliveryLines() {
        return deliveryLines;
    }

    public Component status() {
        return status;
    }

    public int lowDowngradeScrolls() {
        return lowDowngradeScrolls;
    }

    public int midDowngradeScrolls() {
        return midDowngradeScrolls;
    }

    public int highDowngradeScrolls() {
        return highDowngradeScrolls;
    }

    public int highestDowngradeScrolls() {
        return highestDowngradeScrolls;
    }

    public int resetProtectionScrolls() {
        return resetProtectionScrolls;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
