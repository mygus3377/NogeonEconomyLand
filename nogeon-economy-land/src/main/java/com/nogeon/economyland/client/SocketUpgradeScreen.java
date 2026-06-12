package com.nogeon.economyland.client;

import com.nogeon.economyland.item.SocketUpgradeService;
import com.nogeon.economyland.menu.SocketUpgradeMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.SocketRemoveGemPacket;
import com.nogeon.economyland.network.SocketUpgradeActionPacket;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class SocketUpgradeScreen extends AbstractContainerScreen<SocketUpgradeMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int INVENTORY_X = 206;
    private static final int INVENTORY_Y = 74;
    private static final int SLOT_SIZE = 18;
    private static final int GEM_X = 86;
    private static final int GEM_Y = 104;
    private static final int GEM_GAP = 28;
    private HextechButton upgradeButton;
    private HextechButton removeButton;
    private int selectedSlot;
    private int selectedGemIndex = -1;

    public SocketUpgradeScreen(SocketUpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 388;
        this.imageHeight = 264;
        this.selectedSlot = menu.selectedSlot();
    }

    @Override
    protected void init() {
        super.init();
        upgradeButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.socket_upgrade_button"),
            button -> ModNetwork.CHANNEL.sendToServer(new SocketUpgradeActionPacket(selectedSlot)))
            .bounds(leftPos + 24, topPos + 152, 148, 20)
            .build());
        removeButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("소켓 보석 제거"),
            button -> ModNetwork.CHANNEL.sendToServer(new SocketRemoveGemPacket(selectedSlot, selectedGemIndex)))
            .bounds(leftPos + 24, topPos + 176, 148, 20)
            .danger(true)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.smith_close"), button -> onClose())
            .bounds(leftPos + 300, topPos + 224, 64, 18)
            .danger(true)
            .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack stack = getSelectedStack();
        upgradeButton.active = SocketUpgradeService.canUpgrade(stack);
        if (!SocketUpgradeService.hasRemovableGem(stack, selectedGemIndex)) {
            selectedGemIndex = firstRemovableGem(stack);
        }
        removeButton.active = SocketUpgradeService.hasRemovableGem(stack, selectedGemIndex);
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawCyberAccents(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        int len = 6;
        // Top-Left
        graphics.fill(x, y, x + len, y + 1, color);
        graphics.fill(x, y, x + 1, y + len, color);
        // Top-Right
        graphics.fill(x + w - len, y, x + w, y + 1, color);
        graphics.fill(x + w - 1, y, x + w, y + len, color);
        // Bottom-Left
        graphics.fill(x, y + h - 1, x + len, y + h, color);
        graphics.fill(x, y + h - len, x + 1, y + h, color);
        // Bottom-Right
        graphics.fill(x + w - len, y + h - 1, x + w, y + h, color);
        graphics.fill(x + w - 1, y + h - len, x + w, y + h, color);
    }

    private void framedPanel(GuiGraphics graphics, int left, int top, int right, int bottom, int border, int fill) {
        graphics.fill(left, top, right, bottom, 0xFF050505);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, border);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, fill);
        graphics.fill(left + 2, top + 2, right - 2, top + 3, 0x22FFFFFF);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        int themeColor = 0xFF00FF88; // Emerald Neon Green
        int secondaryColor = 0xFF00FFCC; // Cyan Neon Blue
        
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA050907);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA0E1512); // Deep emerald-dark interior
        
        // Cyber-green borders
        graphics.fill(x, y, x + imageWidth, y + 1, themeColor);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, secondaryColor);
        graphics.fill(x, y, x + 1, y + imageHeight, themeColor);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, secondaryColor);
        
        // Header
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 20, 0xFF0A100E);
        drawCustomBorder(graphics, x + 1, y + 1, imageWidth - 2, 20, 0xFF142F25);

        // Paneling
        framedPanel(graphics, x + 18, y + 52, x + 196, y + 190, 0xFF142F25, 0xFF091310);
        framedPanel(graphics, x + 200, y + 52, x + imageWidth - 18, y + 190, 0xFF142F25, 0xFF091310);
        framedPanel(graphics, x + 18, y + 194, x + imageWidth - 18, y + imageHeight - 18, 0xFF142F25, 0xFF091310);

        // Preview chamber pedestal
        framedPanel(graphics, x + 24, y + 74, x + 72, y + 122, themeColor, 0xFF0A0C0A);
        drawCyberAccents(graphics, x + 24, y + 74, 48, 48, secondaryColor);

        ItemStack stack = getSelectedStack();
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 40, y + 90);
            graphics.renderItemDecorations(font, stack, x + 40, y + 90);
        }
        renderGemSockets(graphics, stack);
        renderInventoryGrid(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 10, 0xFFF2E3BC);
        ItemStack stack = getSelectedStack();
        Component name = stack.isEmpty() ? Component.translatable("gui.nogeon_economy_land.none") : stack.getHoverName();
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.socket_selected", name), 84, 76, 0xFFE8E1C4, false);
        
        // 1열 (X = 28) Y = 206 / 222
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.socket_cost", CREDIT_FORMAT.format(SocketUpgradeService.cost(stack))), 28, 206, 0xFFFFD56A, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.socket_count", SocketUpgradeService.sockets(stack), SocketUpgradeService.MAX_SOCKETS), 28, 222, 0xFFD9BF7E, false);
        
        // 2열 (X = 196) Y = 206
        if (menu.status() != null) {
            graphics.drawString(font, menu.status(), 196, 206, 0xFF8ED79E, false);
        }
        long removeCost = SocketUpgradeService.removeCost(stack, selectedGemIndex);
        if (removeCost > 0L) {
            graphics.drawString(font, Component.literal("제거 비용: " + CREDIT_FORMAT.format(removeCost) + " C"), 196, 222, 0xFFFF8A65, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderGemTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int gemIndex = gemIndexAt(mouseX, mouseY);
            if (SocketUpgradeService.hasRemovableGem(getSelectedStack(), gemIndex)) {
                selectedGemIndex = gemIndex;
                return true;
            }
            int inventorySlot = inventorySlotAt(mouseX, mouseY);
            if (inventorySlot >= 0 && minecraft != null && !minecraft.player.getInventory().getItem(inventorySlot).isEmpty()) {
                selectedSlot = inventorySlot;
                selectedGemIndex = firstRemovableGem(getSelectedStack());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderGemSockets(GuiGraphics graphics, ItemStack stack) {
        int socketCount = Math.min(SocketUpgradeService.sockets(stack), SocketUpgradeService.MAX_SOCKETS);
        List<GemInstance> gems = SocketUpgradeService.gems(stack);
        for (int index = 0; index < SocketUpgradeService.MAX_SOCKETS; index++) {
            int left = leftPos + GEM_X + (index % 3) * GEM_GAP;
            int top = topPos + GEM_Y + (index / 3) * (SLOT_SIZE + 4);
            boolean opened = index < socketCount;
            GemInstance gem = index < gems.size() ? gems.get(index) : null;
            boolean validGem = gem != null && gem.isValid() && !gem.gemStack().isEmpty();
            int border = validGem ? gemBorderColor(gem, index) : opened ? 0xFF345246 : 0xFF151A17;
            if (index == selectedGemIndex && validGem) {
                border = 0xFFFFFFFF;
            }

            graphics.fill(left, top, left + SLOT_SIZE, top + SLOT_SIZE, border);
            graphics.fill(left + 1, top + 1, left + SLOT_SIZE - 1, top + SLOT_SIZE - 1, opened ? 0xFF101713 : 0xFF070907);
            if (validGem) {
                graphics.renderItem(gem.gemStack(), left + 1, top + 1);
                graphics.renderItemDecorations(font, gem.gemStack(), left + 1, top + 1);
            }
        }
    }

    private int gemBorderColor(GemInstance gem, int index) {
        if (!SocketUpgradeService.isRainbowGem(gem)) {
            return SocketUpgradeService.gemColor(gem);
        }
        int phase = (int) ((System.currentTimeMillis() / 120L + index * 2L) % 6L);
        return switch (phase) {
            case 0 -> 0xFFFF5555;
            case 1 -> 0xFFFFAA00;
            case 2 -> 0xFFFFFF55;
            case 3 -> 0xFF55FF55;
            case 4 -> 0xFF55FFFF;
            default -> 0xFFFF55FF;
        };
    }

    private void renderGemTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = gemIndexAt(mouseX, mouseY);
        List<GemInstance> gems = SocketUpgradeService.gems(getSelectedStack());
        if (index < 0 || index >= gems.size()) {
            return;
        }
        GemInstance gem = gems.get(index);
        if (gem != null && gem.isValid() && !gem.gemStack().isEmpty()) {
            graphics.renderTooltip(font, gem.gemStack(), mouseX, mouseY);
        }
    }

    private int firstRemovableGem(ItemStack stack) {
        for (int index = 0; index < SocketUpgradeService.MAX_SOCKETS; index++) {
            if (SocketUpgradeService.hasRemovableGem(stack, index)) {
                return index;
            }
        }
        return -1;
    }

    private void renderInventoryGrid(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderInventoryCell(graphics, 9 + row * 9 + column, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + row * SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderInventoryCell(graphics, column, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + 62);
        }
    }

    private void renderInventoryCell(GuiGraphics graphics, int slot, int x, int y) {
        ItemStack stack = minecraft.player.getInventory().getItem(slot);
        int left = leftPos + x;
        int top = topPos + y;
        boolean selected = slot == selectedSlot;
        
        int cellBorder = selected ? 0xFF00FF88 : 0xFF142F25;
        int cellBg = selected ? 0xFF10281F : 0xFF0E110F;
        
        graphics.fill(left, top, left + SLOT_SIZE, top + SLOT_SIZE, cellBorder);
        graphics.fill(left + 1, top + 1, left + SLOT_SIZE - 1, top + SLOT_SIZE - 1, cellBg);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, left + 1, top + 1);
            graphics.renderItemDecorations(font, stack, left + 1, top + 1);
        }
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        int relativeX = Mth.floor(mouseX) - leftPos;
        int relativeY = Mth.floor(mouseY) - topPos;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                if (insideBox(relativeX, relativeY, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + row * SLOT_SIZE)) {
                    return 9 + row * 9 + column;
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            if (insideBox(relativeX, relativeY, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + 62)) {
                return column;
            }
        }
        return -1;
    }

    private int gemIndexAt(double mouseX, double mouseY) {
        int relativeX = Mth.floor(mouseX) - leftPos;
        int relativeY = Mth.floor(mouseY) - topPos;
        for (int index = 0; index < SocketUpgradeService.MAX_SOCKETS; index++) {
            int slotX = GEM_X + (index % 3) * GEM_GAP;
            int slotY = GEM_Y + (index / 3) * (SLOT_SIZE + 4);
            if (insideBox(relativeX, relativeY, slotX, slotY)) {
                return index;
            }
        }
        return -1;
    }

    private boolean insideBox(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
    }

    private ItemStack getSelectedStack() {
        if (minecraft == null || minecraft.player == null || selectedSlot < 0 || selectedSlot >= minecraft.player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return minecraft.player.getInventory().getItem(selectedSlot);
    }
}
