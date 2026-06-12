package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.LuckExchangeMenu;
import com.nogeon.economyland.menu.LuckExchangeOffer;
import com.nogeon.economyland.network.LuckExchangePacket;
import com.nogeon.economyland.network.ModNetwork;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LuckExchangeScreen extends AbstractContainerScreen<LuckExchangeMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);

    public LuckExchangeScreen(LuckExchangeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 330;
        imageHeight = 232;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        for (int index = 0; index < menu.offers().size(); index++) {
            LuckExchangeOffer offer = menu.offers().get(index);
            HextechButton button = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.exchange"),
                ignored -> ModNetwork.CHANNEL.sendToServer(new LuckExchangePacket(offer.id())))
                .bounds(leftPos + 244, topPos + 64 + index * 34, 58, 18)
                .build());
            button.active = menu.tokenCount() >= offer.tokenCost();
        }

        // 실물 증표 일괄 등록 버튼 추가
        HextechButton depositBtn = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.luck_exchange_deposit_button"),
            ignored -> ModNetwork.CHANNEL.sendToServer(new LuckExchangePacket("deposit")))
            .bounds(leftPos + 215, topPos + 38, 97, 15)
            .build());
        depositBtn.active = menu.itemTokenCount() > 0;

        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.back"), ignored -> onClose())
            .bounds(leftPos + 126, topPos + 202, 78, 20)
            .danger(true)
            .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        // 1. 칠흑 헥스테크 배경
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E);
        
        // 2. Cyan 네온 외곽 테두리 (1px)
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // 상
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00FFCC); // 하
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); // 좌
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00FFCC); // 우

        // 3. 상부 바 (보유 정보 영역) 배경 및 테두리
        graphics.fill(x + 18, y + 36, x + imageWidth - 18, y + 56, 0xFF14201D);
        graphics.fill(x + 18, y + 36, x + imageWidth - 18, y + 37, 0xFF1B2C27); // 상 테두리
        graphics.fill(x + 18, y + 55, x + imageWidth - 18, y + 56, 0xFF1B2C27); // 하 테두리
        graphics.fill(x + 18, y + 36, x + 19, y + 56, 0xFF1B2C27); // 좌 테두리
        graphics.fill(x + imageWidth - 19, y + 36, x + imageWidth - 18, y + 56, 0xFF1B2C27); // 우 테두리

        // 4. 하부 바 (목록 영역) 배경 및 테두리
        graphics.fill(x + 18, y + 60, x + imageWidth - 18, y + 194, 0xFF0E1311);
        graphics.fill(x + 18, y + 60, x + imageWidth - 18, y + 61, 0xFF1B2C27); // 상 테두리
        graphics.fill(x + 18, y + 193, x + imageWidth - 18, y + 194, 0xFF1B2C27); // 하 테두리
        graphics.fill(x + 18, y + 60, x + 19, y + 194, 0xFF1B2C27); // 좌 테두리
        graphics.fill(x + imageWidth - 19, y + 60, x + imageWidth - 18, y + 194, 0xFF1B2C27); // 우 테두리

        // 5. 각 교환 항목 행 렌더링
        for (int index = 0; index < menu.offers().size(); index++) {
            int rowY = y + 64 + index * 34;
            graphics.fill(x + 24, rowY - 4, x + imageWidth - 24, rowY + 22, 0xFF1B2C27); // 행 테두리
            graphics.fill(x + 25, rowY - 3, x + imageWidth - 25, rowY + 21, 0xFF121B18); // 행 배경
            graphics.renderItem(rewardStack(menu.offers().get(index)), x + 34, rowY + 1);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 제목 (Cyan 네온)
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFF00FFCC);
        
        // 힌트 (민트그레이)
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.luck_exchange_hint"), 24, 25, 0xFF769B8E, false);
        
        // 가상 불운의 증표 표시 (Cyan)
        Component virtualText = Component.translatable("gui.nogeon_economy_land.luck_exchange_virtual_tokens")
            .append(": ").append(CREDIT_FORMAT.format(menu.virtualTokenCount()));
        graphics.drawString(font, virtualText, 24, 40, 0xFF00FFCC, false);
        
        // 소지한 실물 증표 표시 (민트그레이)
        Component itemText = Component.translatable("gui.nogeon_economy_land.luck_exchange_item_tokens")
            .append(": ").append(CREDIT_FORMAT.format(menu.itemTokenCount()));
        graphics.drawString(font, itemText, 124, 40, 0xFF769B8E, false);

        // 각 행의 텍스트
        for (int index = 0; index < menu.offers().size(); index++) {
            LuckExchangeOffer offer = menu.offers().get(index);
            int y = 66 + index * 34;
            graphics.drawString(font, Component.translatable(offer.labelKey()).append(" x").append(String.valueOf(offer.rewardCount())), 58, y, 0xFFE0F7F4, false);
            
            Component costText = Component.translatable("gui.nogeon_economy_land.luck_exchange_cost")
                .append(": ").append(String.valueOf(offer.tokenCost()));
            int costColor = menu.tokenCount() >= offer.tokenCost() ? 0xFF00FFCC : 0xFFFF5555;
            graphics.drawString(font, costText, 58, y + 11, costColor, false);
        }
    }

    private ItemStack rewardStack(LuckExchangeOffer offer) {
        ResourceLocation id = ResourceLocation.tryParse(offer.rewardItemId());
        Item item = id == null ? Items.BARRIER : BuiltInRegistries.ITEM.get(id);
        ItemStack stack = new ItemStack(item == Items.AIR ? Items.BARRIER : item, Math.max(1, offer.rewardCount()));
        return stack;
    }
}
