package com.nogeon.economyland.client;
import com.nogeon.economyland.menu.LandClaimMenu;
import com.nogeon.economyland.network.LandClaimActionPacket;
import com.nogeon.economyland.network.ModNetwork;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class LandClaimScreen extends AbstractContainerScreen<LandClaimMenu> {
    private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private EditBox memoBox;

    public LandClaimScreen(LandClaimMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 226;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.mode() == LandClaimMenu.Mode.PROMPT) {
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.confirm"),
                button -> ModNetwork.CHANNEL.sendToServer(new LandClaimActionPacket("enter_selection")))
                .bounds(leftPos + 74, topPos + 182, 64, 20)
                .build());
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.cancel"),
                button -> ModNetwork.CHANNEL.sendToServer(new LandClaimActionPacket("close")))
                .bounds(leftPos + 162, topPos + 182, 64, 20)
                .danger(true)
                .build());
            return;
        }

        // 토지 이름 입력을 위한 넉넉한 244px 가로폭 제공
        memoBox = new EditBox(font, leftPos + 28, topPos + 124, 244, 18, Component.translatable("gui.nogeon_economy_land.memo"));
        memoBox.setMaxLength(32);
        memoBox.setValue(menu.memo());
        addRenderableWidget(memoBox);

        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.confirm"),
            button -> ModNetwork.CHANNEL.sendToServer(new LandClaimActionPacket("confirm", memoBox.getValue().trim())))
            .bounds(leftPos + 28, topPos + 188, 56, 20)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.land_undo"),
            button -> ModNetwork.CHANNEL.sendToServer(new LandClaimActionPacket("undo")))
            .bounds(leftPos + 90, topPos + 188, 56, 20)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.land_reset"),
            button -> ModNetwork.CHANNEL.sendToServer(new LandClaimActionPacket("reset")))
            .bounds(leftPos + 152, topPos + 188, 56, 20)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.close"),
            button -> ModNetwork.CHANNEL.sendToServer(new LandClaimActionPacket("close")))
            .bounds(leftPos + 214, topPos + 188, 56, 20)
            .danger(true)
            .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        // 프리미엄 칠흑 미드나이트-다크 배경
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); 
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); 
        
        // 네온 시안 & 블루 그라데이션 테두리
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // 상단 Cyan 네온
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 하단 Blue 네온
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); // 좌측
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 우측

        // 중앙 콘텐츠 영역 체임버
        graphics.fill(x + 18, y + 36, x + imageWidth - 18, y + 174, 0xFF0E1311);
        graphics.fill(x + 18, y + 36, x + 19, y + 174, 0xFF00FFCC); // 좌측 시안 포인트 데코
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFF00FFCC);
        if (menu.mode() == LandClaimMenu.Mode.PROMPT) {
            graphics.drawCenteredString(font, Component.translatable(menu.hasSelection()
                ? "gui.nogeon_economy_land.land_prompt_continue"
                : "gui.nogeon_economy_land.land_prompt_enter"), imageWidth / 2, 58, 0xFFE8E1C4);
            graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.land_prompt_hint"), imageWidth / 2, 82, 0xFF98A49C);
            if (menu.hasSelection()) {
                graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_cuboid_count").append(": ").append(FORMAT.format(menu.cuboidCount())), 28, 112, 0xFF98A49C, false);
                graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_blocks").append(": ").append(FORMAT.format(menu.blocks())), 28, 124, 0xFF98A49C, false);
                graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_price").append(": ").append(FORMAT.format(menu.price())).append(" C"), 28, 136, 0xFFFFD56A, false);
            }
            return;
        }
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_type").append(": ").append(Component.translatable(menu.typeKey())), 28, 48, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_cuboid_count").append(": ").append(FORMAT.format(menu.cuboidCount())), 28, 62, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_blocks").append(": ").append(FORMAT.format(menu.blocks())), 28, 76, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_price").append(": ").append(FORMAT.format(menu.price())).append(" C"), 28, 90, 0xFFFFD56A, false);
        if (menu.discountPercent() > 0) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_discount", menu.discountPercent()), 28, 102, 0xFF8FBF9B, false);
        }
        
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.memo"), 28, 114, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_option_hint"), 28, 150, 0xFF98A49C, false);
    }
}
