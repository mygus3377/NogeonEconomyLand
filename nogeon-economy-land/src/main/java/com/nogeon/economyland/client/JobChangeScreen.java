package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.JobChangeMenu;
import com.nogeon.economyland.network.JobChangePacket;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenJobChangePacket;
import com.nogeon.economyland.network.OpenWalletPacket;
import com.nogeon.economyland.player.JobType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class JobChangeScreen extends AbstractContainerScreen<JobChangeMenu> {
    public JobChangeScreen(JobChangeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = menu.targetJobId().isEmpty() ? 196 : 140;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.targetJobId().isEmpty()) {
            addJobButton(JobType.FARMER, 20, 70);
            addJobButton(JobType.FISHER, 128, 70);
            addJobButton(JobType.MINER, 20, 98);
            addJobButton(JobType.COOK, 128, 98);
            addJobButton(JobType.HUNTER, 20, 126);
            addJobButton(JobType.ENGINEER, 128, 126);
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.back"),
                button -> ModNetwork.CHANNEL.sendToServer(new OpenWalletPacket()))
                .bounds(leftPos + 88, topPos + 164, 72, 20)
                .danger(true) // 뒤로가기 오렌지 네온선 적용
                .build());
            return;
        }

        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.confirm"),
            button -> ModNetwork.CHANNEL.sendToServer(new JobChangePacket(menu.targetJobId())))
            .bounds(leftPos + 42, topPos + 100, 72, 20)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.cancel"),
            button -> onClose())
            .bounds(leftPos + 134, topPos + 100, 72, 20)
            .danger(true) // 취소 오렌지 네온선 적용
            .build());
    }

    private void addJobButton(JobType job, int x, int y) {
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("job.nogeon_economy_land." + job.id()),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenJobChangePacket(job.id())))
            .bounds(leftPos + x, topPos + y, 100, 20)
            .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        int themeNeonColor = 0xFFD455FF; // 마법 보라
        int secondaryNeonColor = 0xFF00C8FF; // 사파이어 블루
        
        // 1. 칠흑 배경 및 딥 퍼플 내벽
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0E0D);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA140F19);
        
        // 2. 외곽 보라/블루 네온 테두리 선
        graphics.fill(x, y, x + imageWidth, y + 1, themeNeonColor);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, secondaryNeonColor);
        graphics.fill(x, y, x + 1, y + imageHeight, themeNeonColor);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, secondaryNeonColor);
        
        // 3. 상단 헤더 프레임
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 20, 0xFF0F0D13);
        drawCustomBorder(graphics, x + 1, y + 1, imageWidth - 2, 20, 0xFF2A1E2F);
        
        // 4. 구획 헥스테크 판넬화
        if (menu.targetJobId().isEmpty()) {
            framedPanel(graphics, x + 12, y + 24, x + imageWidth - 12, y + imageHeight - 36, 0xFF2A1E2F, 0xFF0F0E13);
        } else {
            framedPanel(graphics, x + 12, y + 24, x + imageWidth - 12, y + imageHeight - 48, 0xFF2A1E2F, 0xFF0F0E13);
        }
    }
    
    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void framedPanel(GuiGraphics graphics, int left, int top, int right, int bottom, int border, int fill) {
        graphics.fill(left, top, right, bottom, 0xFF050505);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, border);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, fill);
        graphics.fill(left + 2, top + 2, right - 2, top + 3, 0x22FFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 14, 0xFFE8E1C4);
        if (menu.targetJobId().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.job_change_pick"), imageWidth / 2, 34, 0xFFBFC7A7);
            graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.job_change_cost"), imageWidth / 2, 54, 0xFFFFD56A);
            return;
        }
        Component jobName = Component.translatable("job.nogeon_economy_land." + menu.targetJobId());
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.job_change_question", jobName), imageWidth / 2, 60, 0xFFE8E1C4);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.job_change_cost"), imageWidth / 2, 78, 0xFFFFD56A);
    }
}
