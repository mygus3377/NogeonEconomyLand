package com.nogeon.economyland.client;
 
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.SaveCookRecipePacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
 
public class CookRecipeScreen extends Screen {
    private final int screenWidth = 460;
    private final int screenHeight = 220;
    private int leftPos;
    private int topPos;
 
    private final int maxSlots;
    private final List<String> currentSelected = new ArrayList<>();
    private final List<BuffItem> buffItems = new ArrayList<>();
 
    public CookRecipeScreen(int maxSlots, List<String> initiallySelected) {
        super(Component.literal("나만의 레시피 설정"));
        this.maxSlots = maxSlots;
        if (initiallySelected != null) {
            this.currentSelected.addAll(initiallySelected);
        }
 
        buffItems.add(new BuffItem("DEATH_PREVENTION", "🛡️ 구원의 영양식", "§7치명상 시 하트 3칸 부활 + 넉백 충격파로 적 밀쳐냄 및 속도 둔화"));
        buffItems.add(new BuffItem("BOSS_DAMAGE", "⚔️ 대재앙의 학살자", "§7상시 피해 +15%, 보스 상대 시 피해 총 +40% (플러스 시 총 +60%)"));
        buffItems.add(new BuffItem("GOLDEN_LUCK", "🍀 황금 행운", "§7행운 I~III 부여 + 채광 및 사냥 시 더블 드랍 확률 +15% 상시 주입"));
        buffItems.add(new BuffItem("HEART_BREATH", "💖 대지의 숨결", "§7최대 체력 +20%~60% 상시 증가 + 모든 자연 치유량 +30% 상시 증폭"));
        buffItems.add(new BuffItem("IMMUNITY", "🧪 신성한 수호 결계", "§7디버프 면역 + 화염/낙하 등 모든 속성 피해 50% 무효화 + 넉백 저항 100%"));
        buffItems.add(new BuffItem("STEEL_GUARD", "🏹 금강불괴의 방벽", "§7모든 피해 -20% 상시 감쇄, 투사체 피격 시 총 -45% 곱연산 감쇄"));
    }
 
    @Override
    protected void init() {
        this.leftPos = (this.width - this.screenWidth) / 2;
        this.topPos = (this.height - this.screenHeight) / 2;
 
        this.clearWidgets();
 
        for (int i = 0; i < buffItems.size(); i++) {
            final BuffItem item = buffItems.get(i);
            int yOffset = this.topPos + 35 + i * 24;
 
            Button btn = Button.builder(
                Component.literal(currentSelected.contains(item.id) ? "§a[장착됨]" : "§7[미장착]"),
                button -> {
                    if (currentSelected.contains(item.id)) {
                        currentSelected.remove(item.id);
                    } else {
                        if (currentSelected.size() < maxSlots) {
                            currentSelected.add(item.id);
                        } else {
                            if (maxSlots == 1) {
                                currentSelected.clear();
                                currentSelected.add(item.id);
                            }
                        }
                    }
                    this.init();
                }
            ).bounds(this.leftPos + this.screenWidth - 85, yOffset, 70, 18).build();
            this.addRenderableWidget(btn);
        }
 
        this.addRenderableWidget(Button.builder(
            Component.literal("§e레시피 적용"),
            button -> {
                ModNetwork.CHANNEL.sendToServer(new SaveCookRecipePacket(currentSelected));
                this.onClose();
            }
        ).bounds(this.leftPos + 40, this.topPos + this.screenHeight - 30, 110, 20).build());
 
        this.addRenderableWidget(Button.builder(
            Component.literal("닫기"),
            button -> this.onClose()
        ).bounds(this.leftPos + this.screenWidth - 150, this.topPos + this.screenHeight - 30, 110, 20).build());
    }
 
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
 
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.screenWidth, this.topPos + this.screenHeight, 0xD0121110);
        
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.screenWidth, this.topPos + 1, 0xFFDFB24E);
        graphics.fill(this.leftPos, this.topPos + this.screenHeight - 1, this.leftPos + this.screenWidth, this.topPos + this.screenHeight, 0xFFDFB24E);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.screenHeight, 0xFFDFB24E);
        graphics.fill(this.leftPos + this.screenWidth - 1, this.topPos, this.leftPos + this.screenWidth, this.topPos + this.screenHeight, 0xFFDFB24E);
 
        graphics.drawString(this.font, "👨‍🍳 나만의 특수 레시피 비법", this.leftPos + 15, this.topPos + 12, 0xFFFFF6D3, true);
        
        String slotInfo = "장착 슬롯: §e" + currentSelected.size() + "§7 / §e" + maxSlots + "§r";
        graphics.drawString(this.font, slotInfo, this.leftPos + this.screenWidth - this.font.width(slotInfo) - 15, this.topPos + 12, 0xFFFFFFFF, true);
        
        graphics.fill(this.leftPos + 10, this.topPos + 24, this.leftPos + this.screenWidth - 10, this.topPos + 25, 0x44FFFFFF);
 
        for (int i = 0; i < buffItems.size(); i++) {
            BuffItem item = buffItems.get(i);
            int yOffset = this.topPos + 35 + i * 24;
 
            boolean isSelected = currentSelected.contains(item.id);
            int titleColor = isSelected ? 0xFFFFAA00 : 0xFFDDDDDD;
 
            graphics.drawString(this.font, item.title, this.leftPos + 15, yOffset + 1, titleColor, false);
            graphics.drawString(this.font, item.description, this.leftPos + 15, yOffset + 10, 0xFFAAAAAA, false);
        }
 
        super.render(graphics, mouseX, mouseY, partialTicks);
    }
 
    @Override
    public boolean isPauseScreen() {
        return false;
    }
 
    private static class BuffItem {
        final String id;
        final String title;
        final String description;
 
        BuffItem(String id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }
    }
}
