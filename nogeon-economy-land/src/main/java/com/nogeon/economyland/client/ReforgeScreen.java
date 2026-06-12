package com.nogeon.economyland.client;

import com.nogeon.economyland.item.ReforgeService;
import com.nogeon.economyland.menu.ReforgeMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.ReforgeActionPacket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.components.EditBox;

public final class ReforgeScreen extends AbstractContainerScreen<ReforgeMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int PREVIEW_X = 24;
    private static final int PREVIEW_Y = 74;
    private static final int INVENTORY_X = 206;
    private static final int INVENTORY_Y = 74;
    private static final int SLOT_SIZE = 18;
    private static final String[] EQUIP_NAMES = {"⚔️ 무기", "🏹 활", "🔫 총기", "🛡️ 방어구", "🔮 마법 무기", "💍 장신구", "🛠️ 도구"};
    private static final String[] TOOL_NAMES = {"⛏️ 곡괭이", "🪓 도끼", "🧹 삽", "👨‍🌾 괭이", "🎣 낚시대"};

    private HextechButton unlockButton;
    private HextechButton rollButton;
    private final HextechButton[] lockButtons = new HextechButton[3];
    private final EditBox[] valueFields = new EditBox[3];
    private int selectedSlot;

    // 재련 도파민 애니메이션 및 스킵 필드
    private static boolean skipAnimation = false;
    private boolean isAnimating = false;
    private int reforgeAnimationTicks = 0;
    private int animationFlashTicks = 0;
    private HextechButton skipToggleButton;
    private HextechButton helpButton;
    private HextechButton closeButton;
    private boolean showHelp = false;
    private int selectedEquipType = 0; // 0: 근접 무기, 1: 활 & 쇠뇌, 2: 총기, 3: 방어구, 4: 도구
    private int selectedToolType = 0;  // 0: 곡괭이, 1: 도끼, 2: 삽, 3: 괭이
    private int selectedStatIndex = 0; // 선택된 세부 스탯 옵션
    private boolean equipDropdownOpen = false;
    private boolean toolDropdownOpen = false;
    private boolean statDropdownOpen = false;

    private static final String[] autoTargetOption = new String[]{"any", "any", "any"};
    private static final ReforgeService.Rarity[] autoTargetRarity = new ReforgeService.Rarity[]{null, null, null};
    private static final double[] autoTargetValue = new double[]{0.0, 0.0, 0.0};
    private static boolean autoReforgeActive = false;
    private static int autoReforgeTimer = 0;
    private static int autoReforgeTimeoutTicks = 0;
    private static int lastSelectedSlot = -1;
    private static String lastEvaluatedNbt = "";
    private Component status;

    public ReforgeScreen(ReforgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 388;
        this.imageHeight = 264;
        this.inventoryLabelY = 10_000; // 인벤토리 레이블 숨김
        this.selectedSlot = menu.selectedSlot();
        this.status = menu.status();
    }

    public void setStatus(Component status) {
        this.status = status;
    }

    @Override
    protected void init() {
        super.init();
        this.isAnimating = false;
        this.reforgeAnimationTicks = 0;
        this.animationFlashTicks = 0;
        
        if (selectedSlot != lastSelectedSlot) {
            lastSelectedSlot = selectedSlot;
            autoReforgeActive = false;
            lastEvaluatedNbt = "";
            for (int i = 0; i < 3; i++) {
                autoTargetOption[i] = "any";
                autoTargetRarity[i] = null;
                autoTargetValue[i] = 0.0;
            }
        }

        unlockButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.reforge_unlock"), 
            b -> triggerAction("unlock", -1))
            .bounds(leftPos + 24, topPos + 222, 80, 20) // Y좌표를 222로 리밸런싱하여 텍스트 겹침 제거!
            .build());
        
        rollButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.reforge_roll"), 
            b -> triggerAction("roll", -1))
            .bounds(leftPos + 110, topPos + 222, 80, 20) // Y좌표를 222로 리밸런싱하여 텍스트 겹침 제거!
            .build());

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            lockButtons[i] = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.reforge_lock"), 
                b -> triggerAction("lock", idx))
                .bounds(leftPos + 24 + i * 58, topPos + 165, 54, 18)
                .build());
        }

        // 스킵 토글 단추
        skipToggleButton = addRenderableWidget(HextechButton.hextechBuilder(
            Component.literal(skipAnimation ? "✦ 연출: SKIP" : "✦ 연출: 보기"), b -> toggleSkipAnimation())
            .bounds(leftPos + 270, topPos + 204, 94, 18)
            .danger(skipAnimation)
            .build());

        closeButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.smith_close"), b -> onClose())
            .bounds(leftPos + 270, topPos + 224, 94, 18)
            .danger(true)
            .build());

        helpButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("?"), b -> { showHelp = !showHelp; refreshButtons(); })
            .bounds(leftPos + imageWidth - 34, topPos + 24, 16, 16)
            .build());

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            int panelTop = topPos + 18;
            int slotY = panelTop + 14 + i * 53;
            int valY = slotY + 34;
            int unlocked = ReforgeService.getUnlockedCount(getSelectedStack());
            
            EditBox editBox = new EditBox(font, leftPos - 135 + 22, valY, 60, 12, Component.literal(""));
            editBox.setValue(autoTargetValue[idx] > 0.0001 ? String.format(Locale.ROOT, "%.1f", autoTargetValue[idx] * 100.0) : "0.0");
            editBox.setResponder(text -> {
                if (text.isEmpty()) {
                    autoTargetValue[idx] = 0.0;
                    return;
                }
                try {
                    double val = Double.parseDouble(text);
                    autoTargetValue[idx] = Math.max(0.0, Math.min(100.0, val)) / 100.0;
                } catch (NumberFormatException ignored) {
                }
            });
            editBox.active = (idx < unlocked) && !isAnimating;
            editBox.visible = !showHelp && (idx < unlocked);
            editBox.setTextColor(0xFFFFFF55);
            editBox.setBordered(true);
            
            valueFields[idx] = addRenderableWidget(editBox);
        }

        refreshButtons();
    }

    private void triggerAction(String action, int lockIndex) {
        if (skipAnimation) {
            ModNetwork.CHANNEL.sendToServer(new ReforgeActionPacket(action, selectedSlot, lockIndex));
            refreshButtons();
        } else {
            if (isAnimating) return;
            if (action.equals("roll")) {
                this.isAnimating = true;
                this.reforgeAnimationTicks = 0;
                this.animationFlashTicks = 0;
            } else {
                ModNetwork.CHANNEL.sendToServer(new ReforgeActionPacket(action, selectedSlot, lockIndex));
            }
            refreshButtons();
        }
    }

    private void toggleSkipAnimation() {
        skipAnimation = !skipAnimation;
        if (skipToggleButton != null) {
            skipToggleButton.setMessage(Component.literal(skipAnimation ? "✦ 연출: SKIP" : "✦ 연출: 보기"));
            skipToggleButton.danger(skipAnimation);
        }
    }

    private void refreshButtons() {
        boolean setupActive = !isAnimating && !showHelp;
        ItemStack stack = getSelectedStack();
        int unlocked = ReforgeService.getUnlockedCount(stack);
        
        unlockButton.active = setupActive && unlocked < 3 && !stack.isEmpty() && !ReforgeService.getPool(stack).isEmpty();
        unlockButton.visible = !showHelp;
        
        rollButton.active = setupActive && unlocked > 0;
        rollButton.visible = !showHelp;
        
        ListTag list = stack.getOrCreateTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < 3; i++) {
            if (lockButtons[i] != null) {
                lockButtons[i].active = setupActive;
                lockButtons[i].visible = !showHelp && (i < unlocked);
                if (i < list.size()) {
                    boolean locked = list.getCompound(i).getBoolean("Locked");
                    lockButtons[i].setMessage(Component.translatable(locked ? "gui.nogeon_economy_land.reforge_unlock_short" : "gui.nogeon_economy_land.reforge_lock"));
                    lockButtons[i].danger(locked); // 락이 걸려 있을 시 레드 네온 경고 표시
                }
            }
        }
        if (skipToggleButton != null) {
            skipToggleButton.active = setupActive;
            skipToggleButton.visible = !showHelp;
        }
        if (closeButton != null) {
            closeButton.active = setupActive;
            closeButton.visible = !showHelp;
        }
        for (int i = 0; i < 3; i++) {
            if (valueFields[i] != null) {
                valueFields[i].active = setupActive && (i < unlocked);
                valueFields[i].visible = !showHelp && (i < unlocked);
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        
        if (animationFlashTicks > 0) {
            animationFlashTicks--;
        }

        if (isAnimating) {
            reforgeAnimationTicks++;
            if (reforgeAnimationTicks == 1) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.8F, 1.2F);
                }
            } else if (reforgeAnimationTicks == 6 || reforgeAnimationTicks == 12 || reforgeAnimationTicks == 18) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.3F + (reforgeAnimationTicks * 0.03F));
                }
            } else if (reforgeAnimationTicks == 24) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 0.8F);
                    minecraft.player.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.2F, 1.5F);
                }
                ModNetwork.CHANNEL.sendToServer(new ReforgeActionPacket("roll", selectedSlot, -1));
                animationFlashTicks = 6;
            }
            
            if (reforgeAnimationTicks >= 28) {
                isAnimating = false;
                reforgeAnimationTicks = 0;
            }
            refreshButtons();
            return;
        }

        if (autoReforgeActive) {
            if (autoReforgeTimer > 0) {
                autoReforgeTimer--;
            } else {
                ItemStack stack = getSelectedStack();
                int unlocked = ReforgeService.getUnlockedCount(stack);
                if (stack.isEmpty() || unlocked <= 0) {
                    autoReforgeActive = false;
                } else {
                    String currentNbtStr = stack.getOrCreateTag().toString();
                    if (currentNbtStr.equals(lastEvaluatedNbt)) {
                        autoReforgeTimeoutTicks++;
                        if (autoReforgeTimeoutTicks < 40) {
                            return;
                        }
                        lastEvaluatedNbt = "";
                    }
                    autoReforgeTimeoutTicks = 0;

                    ListTag list = stack.getOrCreateTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
                    List<AutoReforgeTarget> targets = new ArrayList<>();
                    for (int j = 0; j < 3; j++) {
                        String opt = autoTargetOption[j];
                        ReforgeService.Rarity rar = autoTargetRarity[j];
                        double val = autoTargetValue[j];
                        boolean isSpecific = !opt.equals("any") || rar != null || val > 0.0001;
                        if (isSpecific) {
                            targets.add(new AutoReforgeTarget(opt, rar, val, j));
                        }
                    }

                    boolean[] isLocked = new boolean[3];
                    boolean[] shouldBeLocked = new boolean[3];

                    // 1단계: 이미 잠긴 슬롯들을 목표에서 우선 매핑하여 제거
                    for (int i = 0; i < unlocked; i++) {
                        CompoundTag tag = getReforgeSlotTag(list, i);
                        if (tag == null) continue;
                        
                        boolean locked = tag.getBoolean("Locked");
                        isLocked[i] = locked;
                        if (locked) {
                            String modId = tag.getString("ModifierId");
                            ReforgeService.Rarity rarity = ReforgeService.Rarity.safe(tag.getString("Rarity"));
                            double value = tag.getDouble("Value");
                            
                            // 매칭되는 타겟을 찾아서 제거
                            for (int t = 0; t < targets.size(); t++) {
                                if (targets.get(t).matches(modId, rarity, value)) {
                                    targets.remove(t);
                                    break;
                                }
                            }
                            shouldBeLocked[i] = true;
                        }
                    }

                    // 2단계: 잠기지 않은 슬롯 중 목표에 매칭되는 슬롯 탐색 및 매핑
                    for (int i = 0; i < unlocked; i++) {
                        if (isLocked[i]) continue;
                        
                        CompoundTag tag = getReforgeSlotTag(list, i);
                        if (tag == null) continue;
                        
                        String modId = tag.getString("ModifierId");
                        ReforgeService.Rarity rarity = ReforgeService.Rarity.safe(tag.getString("Rarity"));
                        double value = tag.getDouble("Value");
                        
                        for (int t = 0; t < targets.size(); t++) {
                            if (targets.get(t).matches(modId, rarity, value)) {
                                targets.remove(t);
                                shouldBeLocked[i] = true;
                                break;
                            }
                        }
                    }

                    // 모든 구체적인 목표가 매칭되어 남아있는 목표가 없으면 완료
                    boolean allSatisfied = targets.isEmpty();
                    
                    if (allSatisfied) {
                        autoReforgeActive = false;
                        lastEvaluatedNbt = "";
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                            minecraft.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.0F);
                        }
                    } else {
                        boolean sentPacket = false;
                        for (int i = 0; i < unlocked; i++) {
                            CompoundTag tag = getReforgeSlotTag(list, i);
                            if (tag == null) continue;
                            boolean locked = tag.getBoolean("Locked");
                            // 자동 재련 루프는 오직 잠금(Locked: false -> true) 설정만 전송할 수 있도록 제약하여
                            // 비동기 NBT 싱크 오류나 타이밍 어긋남으로 이미 잠긴 자물쇠가 풀리는 대참사를 원천 방지
                            if (!locked && shouldBeLocked[i]) {
                                lastEvaluatedNbt = currentNbtStr;
                                ModNetwork.CHANNEL.sendToServer(new ReforgeActionPacket("lock", selectedSlot, i));
                                autoReforgeTimer = 2; // 잠금 전송 딜레이 단축
                                sentPacket = true;
                                break;
                            }
                        }
                        
                        if (!sentPacket) {
                            lastEvaluatedNbt = currentNbtStr;
                            ModNetwork.CHANNEL.sendToServer(new ReforgeActionPacket("roll_silent", selectedSlot, -1)); // 조용한 롤링 호출
                            autoReforgeTimer = 2; // 롤링 딜레이 단축 (렉 방지 및 0.1초 고속 주사위)
                        }
                    }
                }
            }
        }

        ItemStack stack = getSelectedStack();
        if (stack.isEmpty() && getFallbackSelectedSlot() >= 0) {
            selectedSlot = getFallbackSelectedSlot();
        }
        refreshButtons();
    }

    private int getFallbackSelectedSlot() {
        if (minecraft == null || minecraft.player == null) return -1;
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            if (!minecraft.player.getInventory().getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        int themeNeonColor = 0xFFD455FF; // 마법 마젠타-보라
        int secondaryNeonColor = 0xFF00C8FF; // 사파이어 블루

        if (showHelp) {
            // 도움말 활성화 시에는 슬롯, 아이템 그리드를 그리지 않고 검은 판넬과 네온 보더만 그려서 뚫고 보임 완벽 예방!
            graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0E0D);
            graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA140F19);
            graphics.fill(x, y, x + imageWidth, y + 1, themeNeonColor);
            graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, secondaryNeonColor);
            graphics.fill(x, y, x + 1, y + imageHeight, themeNeonColor);
            graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, secondaryNeonColor);
            return;
        }
        
        // 1. 칠흑 배경
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0E0D);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA140F19); // 딥 퍼플 내벽
        
        // 2. 외곽 보라/블루 네온 테두리 선
        graphics.fill(x, y, x + imageWidth, y + 1, themeNeonColor);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, secondaryNeonColor);
        graphics.fill(x, y, x + 1, y + imageHeight, themeNeonColor);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, secondaryNeonColor);
        
        // 3. 상단 헤더 프레임
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 20, 0xFF0F0D13);
        drawCustomBorder(graphics, x + 1, y + 1, imageWidth - 2, 20, 0xFF2A1E2F);

        // 4. 구획 헥스테크 판넬화
        framedPanel(graphics, x + 18, y + 52, x + 196, y + 190, 0xFF2A1E2F, 0xFF0F0E13);
        framedPanel(graphics, x + 200, y + 52, x + imageWidth - 18, y + 190, 0xFF2A1E2F, 0xFF0F0E13);
        framedPanel(graphics, x + 18, y + 194, x + imageWidth - 18, y + imageHeight - 18, 0xFF2A1E2F, 0xFF0F0E13);

        // 5. 미리보기 아이콘 구역
        framedPanel(graphics, x + PREVIEW_X, y + PREVIEW_Y, x + PREVIEW_X + 48, y + PREVIEW_Y + 48, themeNeonColor, 0xFF0A0C0A);
        drawCyberAccents(graphics, x + PREVIEW_X, y + PREVIEW_Y, 48, 48, secondaryNeonColor);

        ItemStack stack = getSelectedStack();
        if (!stack.isEmpty() && !isAnimating) {
            graphics.renderItem(stack, x + PREVIEW_X + 16, y + PREVIEW_Y + 16);
            graphics.renderItemDecorations(font, stack, x + PREVIEW_X + 16, y + PREVIEW_Y + 16);
        }
        
        if (isAnimating) {
            drawReforgeAnimation(graphics, x + PREVIEW_X + 24, y + PREVIEW_Y + 24, stack);
        }
        
        renderInventoryGrid(graphics);
        renderSlots(graphics, x, y, stack);
        
        renderAutoReforgePanel(graphics, mouseX, mouseY);

        // 6. 충격 펄스 전체화면 화이트아웃 플래시
        if (animationFlashTicks > 0) {
            int alpha = (int) (animationFlashTicks / 6.0F * 130);
            graphics.fill(x, y, x + imageWidth, y + imageHeight, (alpha << 24) | 0xFFFFFF);
        }
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

    private void drawLockIcon(GuiGraphics graphics, int x, int y) {
        // Lock body (Gold metallic)
        graphics.fill(x, y + 5, x + 11, y + 12, 0xFFD4AF37);
        graphics.fill(x + 1, y + 6, x + 10, y + 11, 0xFFF5D77F);
        // Shackle (Silver)
        graphics.fill(x + 3, y + 1, x + 8, y + 5, 0xFFC0C0C0);
        graphics.fill(x + 4, y + 2, x + 7, y + 5, 0xFF0A0C0A);
        // Keyhole (Black)
        graphics.fill(x + 5, y + 8, x + 6, y + 10, 0xFF111111);
    }

    private int getPulsingGoldColor() {
        float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.005D) + 1.0D) / 2.0F;
        int r = (int) (0xFF - (0xFF - 0xCC) * pulse);
        int g = (int) (0xD7 - (0xD7 - 0x99) * pulse);
        int b = (int) (0x00 + (0x55 - 0x00) * pulse);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawSparkle(GuiGraphics graphics, int slotX, int slotY) {
        long time = System.currentTimeMillis();
        float progress = (time % 2000) / 2000.0F;
        int perimeter = 54 * 2 + 36 * 2;
        int currentDist = (int) (progress * perimeter);
        
        int sparkleX = slotX;
        int sparkleY = slotY;
        if (currentDist < 54) {
            sparkleX = slotX + currentDist;
            sparkleY = slotY;
        } else if (currentDist < 54 + 36) {
            sparkleX = slotX + 54;
            sparkleY = slotY + (currentDist - 54);
        } else if (currentDist < 54 * 2 + 36) {
            sparkleX = slotX + 54 - (currentDist - (54 + 36));
            sparkleY = slotY + 36;
        } else {
            sparkleX = slotX;
            sparkleY = slotY + 36 - (currentDist - (54 * 2 + 36));
        }
        
        graphics.fill(sparkleX - 1, sparkleY, sparkleX + 2, sparkleY + 1, 0xFFFFFFFF);
        graphics.fill(sparkleX, sparkleY - 1, sparkleX + 1, sparkleY + 2, 0xFFFFFFFF);
    }

    private void framedPanel(GuiGraphics graphics, int left, int top, int right, int bottom, int border, int fill) {
        graphics.fill(left, top, right, bottom, 0xFF050505);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, border);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, fill);
        graphics.fill(left + 2, top + 2, right - 2, top + 3, 0x22FFFFFF);
    }

    private void drawReforgeAnimation(GuiGraphics graphics, int centerX, int centerY, ItemStack stack) {
        if (reforgeAnimationTicks < 24) {
            float progress = reforgeAnimationTicks / 24.0F;
            int numParticles = 12;
            for (int i = 0; i < numParticles; i++) {
                double angle = (double) i / numParticles * Math.PI * 2 + (reforgeAnimationTicks * 0.35F);
                double radius = 30.0 * (1.0F - progress);
                int px = centerX + (int) (Math.cos(angle) * radius);
                int py = centerY + (int) (Math.sin(angle) * radius);
                
                int pColor = (i % 2 == 0) ? 0xFFD455FF : 0xFF00C8FF; // 보라/시안 교차 에너지 입자
                graphics.fill(px - 1, py - 1, px + 2, py + 2, pColor);
            }
            int alpha = (int) (40 + Math.sin(reforgeAnimationTicks * 0.5F) * 35);
            graphics.fill(centerX - 10, centerY - 10, centerX + 10, centerY + 10, (alpha << 24) | 0xFFD455FF);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, centerX - 8, centerY - 8);
            }
        } else {
            float shockProgress = (reforgeAnimationTicks - 24) / 4.0F;
            int radius = (int) (4.0F + shockProgress * 32.0F);
            int flashAlpha = (int) ((1.0F - shockProgress) * 200);
            drawCustomBorder(graphics, centerX - radius, centerY - radius, radius * 2, radius * 2, (flashAlpha << 24) | 0xFF00C8FF);
            int sparkCount = 16;
            for (int i = 0; i < sparkCount; i++) {
                double angle = (double) i / sparkCount * Math.PI * 2;
                double dist = radius * 0.95D;
                int sx = centerX + (int) (Math.cos(angle) * dist);
                int sy = centerY + (int) (Math.sin(angle) * dist);
                graphics.fill(sx - 1, sy - 1, sx + 1, sy + 1, 0xFFD455FF);
            }
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, centerX - 8, centerY - 8);
                graphics.renderItemDecorations(font, stack, centerX - 8, centerY - 8);
            }
        }
    }

    private void renderSlots(GuiGraphics graphics, int x, int y, ItemStack stack) {
        ListTag list = stack.getOrCreateTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
        int unlocked = ReforgeService.getUnlockedCount(stack);
        
        for (int i = 0; i < 3; i++) {
            int slotX = x + 24 + i * 58;
            int slotY = y + 122;
            
            boolean isLegendary = false;
            if (i < list.size()) {
                CompoundTag tag = list.getCompound(i);
                String modId = tag.getString("ModifierId");
                if (!modId.equals("none")) {
                    ReforgeService.Rarity rarity = ReforgeService.Rarity.safe(tag.getString("Rarity"));
                    if (rarity == ReforgeService.Rarity.LEGENDARY) {
                        isLegendary = true;
                    }
                }
            }

            int border = (i < unlocked) ? (isLegendary ? getPulsingGoldColor() : 0xFFD455FF) : 0xFF2A1E2F;
            framedPanel(graphics, slotX, slotY, slotX + 54, slotY + 36, border, 0xFF0A0C0A);
            
            if (i < list.size()) {
                CompoundTag tag = list.getCompound(i);
                boolean locked = tag.getBoolean("Locked");
                
                if (isAnimating && !locked) {
                    // 고대 룬 룰렛 스피너 작동
                    String[] runes = {"✦", "✴", "✧", "❈", "★", "🔮", "🌌", "❂", "✥"};
                    int runeIndex = (reforgeAnimationTicks + i * 5) % runes.length;
                    int[] colors = {0xFFD455FF, 0xFF00C8FF, 0xFFFF55FF, 0xFF55FFFF, 0xFFFFFF55, 0xFFFF5555};
                    int color = colors[(reforgeAnimationTicks / 2 + i) % colors.length];
                    
                    graphics.drawCenteredString(font, runes[runeIndex], slotX + 27, slotY + 14, color);
                } else {
                    String modId = tag.getString("ModifierId");
                    if (!modId.equals("none")) {
                        ReforgeService.Rarity rarity = ReforgeService.Rarity.safe(tag.getString("Rarity"));
                        
                        // Modifier pool 에서 Modifier를 찾아 번역명 획득
                        ReforgeService.Modifier foundMod = null;
                        for (ReforgeService.Modifier m : ReforgeService.getPool(stack)) {
                            if (m.id().equals(modId)) {
                                foundMod = m;
                                break;
                            }
                        }
                        
                        Component displayName;
                        if (foundMod != null) {
                            displayName = Component.translatable(foundMod.translationKey());
                        } else {
                            displayName = Component.literal(modId);
                        }
                        
                        graphics.drawCenteredString(font, displayName, slotX + 27, slotY + 4, rarity.color.getColor());
                        
                        // 소수점 1자리까지 표시
                        String val = String.format(Locale.ROOT, "%.1f%%", tag.getDouble("Value") * 100.0);
                        graphics.drawCenteredString(font, val, slotX + 27, slotY + 16, 0xFFFFFFFF);
                        
                        if (locked) {
                            drawLockIcon(graphics, slotX + 38, slotY + 4);
                        }
                    } else {
                        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.reforge_empty"), slotX + 27, slotY + 14, 0xFF444444);
                    }
                }
            } else {
                if (i < unlocked) {
                    graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.reforge_empty"), slotX + 27, slotY + 14, 0xFF444444);
                } else {
                    graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.reforge_locked"), slotX + 27, slotY + 14, 0xFF222222);
                }
            }

            if (isLegendary && i < unlocked && !isAnimating) {
                drawSparkle(graphics, slotX, slotY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (showHelp) return;
        graphics.drawCenteredString(font, title, imageWidth / 2, 10, 0xFFF2E3BC);
        ItemStack stack = getSelectedStack();
        Component selectedName = stack.isEmpty() ? Component.translatable("gui.nogeon_economy_land.none") : stack.getHoverName();
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.reforge_selected", selectedName), 84, 76, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.reforge_roll_cost", CREDIT_FORMAT.format(ReforgeService.getRollCost(stack))), 28, 202, 0xFFFFD56A, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.reforge_unlock_cost", CREDIT_FORMAT.format(ReforgeService.getUnlockCost(stack))), 28, 214, 0xFFFFD56A, false);
        
        if (this.status != null) {
            graphics.drawString(font, this.status, 196, 228, 0xFF8ED79E, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderSlotTooltips(graphics, mouseX, mouseY);
        if (showHelp) {
            renderHelpOverlay(graphics);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderHelpOverlay(GuiGraphics graphics) {
        int w = 340;
        int h = 210;
        int x = leftPos + (imageWidth - w) / 2;
        int y = topPos + (imageHeight - h) / 2;
        
        // 1. 네온 퍼플 판넬
        framedPanel(graphics, x, y, x + w, y + h, 0xFF00C8FF, 0xFA0A0C0A);
        graphics.drawCenteredString(font, "✦ 장비별 재련 세부 등급표 ✦", x + w / 2, y + 10, 0xFF00FFFF);
        
        // 2. 드롭다운 라벨
        graphics.drawString(font, "장비군", x + 15, y + 25, 0xFFAAAAAA, false);
        if (selectedEquipType == 6) {
            graphics.drawString(font, "도구 종류", x + 110, y + 25, 0xFFAAAAAA, false);
            graphics.drawString(font, "세부 스탯 옵션 선택", x + 200, y + 25, 0xFFAAAAAA, false);
        } else {
            graphics.drawString(font, "세부 스탯 옵션 선택", x + 120, y + 25, 0xFFAAAAAA, false);
        }

        // 3. 기대 수치 계산을 위한 스탯 풀 결정
        List<ReforgeService.Modifier> pool = List.of();
        switch (selectedEquipType) {
            case 0 -> pool = ReforgeService.MELEE_MODS; // Melee
            case 1 -> pool = ReforgeService.BOW_MODS; // Bow
            case 2 -> pool = ReforgeService.RANGE_MODS; // Gun
            case 3 -> pool = ReforgeService.ARMOR_MODS; // Armor
            case 4 -> pool = ReforgeService.MAGIC_MODS; // Magic
            case 5 -> pool = ReforgeService.CURIO_MODS; // Accessories
            case 6 -> {
                switch (selectedToolType) {
                    case 0 -> pool = ReforgeService.getPool(new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
                    case 1 -> pool = ReforgeService.getPool(new ItemStack(net.minecraft.world.item.Items.DIAMOND_AXE));
                    case 2 -> pool = ReforgeService.getPool(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SHOVEL));
                    case 3 -> pool = ReforgeService.getPool(new ItemStack(net.minecraft.world.item.Items.DIAMOND_HOE));
                    case 4 -> pool = ReforgeService.FISHING_ROD_MODS;
                }
            }
        }
        
        if (pool == null || pool.isEmpty()) {
            pool = List.of(new ReforgeService.Modifier("melee_damage", null, 0.05, "reforge.mod.melee_damage", "reforge.desc.melee_damage"));
        }
        
        // 인덱스 아웃 오브 바운드 완벽 방지
        if (selectedStatIndex >= pool.size()) {
            selectedStatIndex = 0;
        }

        ReforgeService.Modifier selectedMod = pool.get(selectedStatIndex);
        double baseVal = selectedMod.baseValue();

        // 4. 등급표 렌더링
        int rowY = y + 74;
        graphics.fill(x + 12, rowY - 4, x + w - 12, rowY - 3, 0x44FFFFFF); // 헤더 구분선
        
        graphics.drawString(font, "등급", x + 20, rowY, 0xFFAAAAAA, false);
        graphics.drawString(font, "확률", x + 70, rowY, 0xFFAAAAAA, false);
        
        String statTitle = Component.translatable(selectedMod.translationKey()).getString();
        graphics.drawString(font, "선택 스탯: " + statTitle + " 범위 (Min ~ Max)", x + 115, rowY, 0xFF00FFCC, false);
        
        rowY += 14;
        graphics.fill(x + 12, rowY - 4, x + w - 12, rowY - 3, 0x22FFFFFF); // 서브선
        
        for (ReforgeService.Rarity rarity : ReforgeService.Rarity.values()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.reforge_rarity." + rarity.name().toLowerCase(Locale.ROOT)), x + 20, rowY, rarity.color.getColor(), false);
            graphics.drawString(font, (int)(rarity.weight * 100) + "%", x + 70, rowY, 0xFFFFFFFF, false);
            
            double min = baseVal * rarity.statMultiplier * 0.8 * 100.0;
            double max = baseVal * rarity.statMultiplier * 1.2 * 100.0;
            String range = String.format("%.1f%% ~ %.1f%%", min, max);
            graphics.drawString(font, range, x + 115, rowY, 0xFFFFFF55, false);
            
            rowY += 15;
        }
        
        graphics.fill(x + 12, rowY - 2, x + w - 12, rowY - 1, 0x22FFFFFF);
        
        // 5. 하단 옵션 설명 (선택된 스탯의 상세 가이드 정보)
        String optionDetails = "설명: " + Component.translatable(selectedMod.descriptionKey()).getString();
        if (optionDetails.length() > 50) {
            optionDetails = optionDetails.substring(0, 48) + "..";
        }
        graphics.drawString(font, optionDetails, x + 16, y + h - 30, 0xFF98A49C, false);
        graphics.drawCenteredString(font, "§7[화면 빈 공간을 클릭하면 닫힙니다]", x + w / 2, y + h - 14, 0xFF888888);

        // 6. 드롭다운 그리기 (z-index 처리를 위해 표가 다 그려진 후 드롭다운을 덮어씌움)
        drawDropdowns(graphics, x, y, pool);
    }

    private void drawDropdowns(GuiGraphics graphics, int x, int y, List<ReforgeService.Modifier> pool) {
        // 1) 첫 번째 장비군 드롭다운 박스
        int ex = x + 15;
        int ey = y + 36;
        int ew = (selectedEquipType == 6) ? 90 : 100;
        int eh = 16;

        graphics.fill(ex, ey, ex + ew, ey + eh, 0xFF2A1E2F);
        graphics.fill(ex + 1, ey + 1, ex + ew - 1, ey + eh - 1, 0xFF0A0C0A);
        graphics.drawString(font, EQUIP_NAMES[selectedEquipType], ex + 6, ey + 4, 0xFFFFFFFF, false);
        graphics.drawString(font, equipDropdownOpen ? "▲" : "▼", ex + ew - 12, ey + 4, 0xFF888888, false);

        // 2) 두 번째 세부 도구 드롭다운 박스
        int tx = x + 110;
        int ty = y + 36;
        int tw = 85;
        int th = 16;
        if (selectedEquipType == 6) {
            graphics.fill(tx, ty, tx + tw, ty + th, 0xFF2A1E2F);
            graphics.fill(tx + 1, ty + 1, tx + tw - 1, ty + th - 1, 0xFF0A0C0A);
            graphics.drawString(font, TOOL_NAMES[selectedToolType], tx + 6, ty + 4, 0xFFFFFFFF, false);
            graphics.drawString(font, toolDropdownOpen ? "▲" : "▼", tx + tw - 12, ty + 4, 0xFF888888, false);
        }

        // 3) 세 번째 스탯 선택 드롭다운 박스
        int sx = (selectedEquipType == 6) ? x + 200 : x + 120;
        int sy = y + 36;
        int sw = (selectedEquipType == 6) ? 125 : 205;
        int sh = 16;

        graphics.fill(sx, sy, sx + sw, sy + sh, 0xFF2A1E2F);
        graphics.fill(sx + 1, sy + 1, sx + sw - 1, sy + sh - 1, 0xFF0A0C0A);
        String statTitle = Component.translatable(pool.get(selectedStatIndex).translationKey()).getString();
        if (statTitle.length() > (selectedEquipType == 6 ? 8 : 14)) {
            statTitle = statTitle.substring(0, (selectedEquipType == 6 ? 7 : 13)) + "..";
        }
        graphics.drawString(font, statTitle, sx + 6, sy + 4, 0xFF00FFCC, false);
        graphics.drawString(font, statDropdownOpen ? "▲" : "▼", sx + sw - 12, sy + 4, 0xFF888888, false);

        // --- 옵션 리스트 렌더링 (가장 높은 z-index로 나중에 드롭다운 덮어쓰기) ---
        if (equipDropdownOpen) {
            int ox = ex;
            int oy = ey + eh;
            int ow = ew;
            int oh = eh * EQUIP_NAMES.length;
            graphics.fill(ox, oy, ox + ow, oy + oh, 0xFF2A1E2F);
            graphics.fill(ox + 1, oy + 1, ox + ow - 1, oy + oh - 1, 0xFF140F19);
            for (int i = 0; i < EQUIP_NAMES.length; i++) {
                int itemY = oy + i * eh;
                int color = (i == selectedEquipType) ? 0xFF00FFFF : 0xFFDDDDDD;
                graphics.drawString(font, EQUIP_NAMES[i], ox + 6, itemY + 4, color, false);
                if (i < EQUIP_NAMES.length - 1) {
                    graphics.fill(ox + 4, itemY + eh - 1, ox + ow - 4, itemY + eh, 0x11FFFFFF);
                }
            }
        }

        if (selectedEquipType == 6 && toolDropdownOpen) {
            int ox = tx;
            int oy = ty + th;
            int ow = tw;
            int oh = th * TOOL_NAMES.length;
            graphics.fill(ox, oy, ox + ow, oy + oh, 0xFF2A1E2F);
            graphics.fill(ox + 1, oy + 1, ox + ow - 1, oy + oh - 1, 0xFF140F19);
            for (int i = 0; i < TOOL_NAMES.length; i++) {
                int itemY = oy + i * th;
                int color = (i == selectedToolType) ? 0xFF00FFFF : 0xFFDDDDDD;
                graphics.drawString(font, TOOL_NAMES[i], ox + 6, itemY + 4, color, false);
                if (i < TOOL_NAMES.length - 1) {
                    graphics.fill(ox + 4, itemY + th - 1, ox + ow - 4, itemY + th, 0x11FFFFFF);
                }
            }
        }

        if (statDropdownOpen) {
            int ox = sx;
            int oy = sy + sh;
            int ow = sw;
            int oh = sh * pool.size();
            graphics.fill(ox, oy, ox + ow, oy + oh, 0xFF2A1E2F);
            graphics.fill(ox + 1, oy + 1, ox + ow - 1, oy + oh - 1, 0xFF140F19);
            for (int i = 0; i < pool.size(); i++) {
                int itemY = oy + i * sh;
                int color = (i == selectedStatIndex) ? 0xFF00FFFF : 0xFFDDDDDD;
                String itemTitle = Component.translatable(pool.get(i).translationKey()).getString();
                if (itemTitle.length() > (selectedEquipType == 6 ? 8 : 14)) {
                    itemTitle = itemTitle.substring(0, (selectedEquipType == 6 ? 7 : 13)) + "..";
                }
                graphics.drawString(font, itemTitle, ox + 6, itemY + 4, color, false);
                if (i < pool.size() - 1) {
                    graphics.fill(ox + 4, itemY + sh - 1, ox + ow - 4, itemY + sh, 0x11FFFFFF);
                }
            }
        }
    }

    private void renderSlotTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isAnimating) return; // 애니메이션 도중에는 툴팁 차단
        ItemStack stack = getSelectedStack();
        if (stack.isEmpty()) return;
        ListTag list = stack.getOrCreateTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
        List<ReforgeService.Modifier> pool = ReforgeService.getPool(stack);

        for (int i = 0; i < 3; i++) {
            int slotX = leftPos + 24 + i * 58;
            int slotY = topPos + 122;
            if (mouseX >= slotX && mouseX <= slotX + 54 && mouseY >= slotY && mouseY <= slotY + 36) {
                if (i < list.size()) {
                    CompoundTag tag = list.getCompound(i);
                    String modId = tag.getString("ModifierId");
                    if (!modId.equals("none")) {
                        for (ReforgeService.Modifier mod : pool) {
                            if (mod.id().equals(modId)) {
                                List<Component> tooltip = new ArrayList<>();
                                ReforgeService.Rarity rarity = ReforgeService.Rarity.safe(tag.getString("Rarity"));
                                tooltip.add(Component.translatable(mod.translationKey()).withStyle(ChatFormatting.WHITE));
                                tooltip.add(Component.literal("[" + rarity.name() + "]").withStyle(rarity.color));
                                tooltip.add(Component.empty());
                                tooltip.add(Component.translatable(mod.descriptionKey()).withStyle(ChatFormatting.GRAY));
                                String valStr = String.format(Locale.ROOT, "%.1f%%", tag.getDouble("Value") * 100.0);
                                tooltip.add(Component.literal("수치: +" + valStr).withStyle(ChatFormatting.YELLOW));
                                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                                break;
                            }
                        }
                    } else {
                        graphics.renderTooltip(font, Component.literal("재련되지 않은 슬롯입니다."), mouseX, mouseY);
                    }
                } else {
                    graphics.renderTooltip(font, Component.literal("잠긴 슬롯입니다. 해제가 필요합니다."), mouseX, mouseY);
                }
            }
        }
    }

    private void renderInventoryGrid(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) return;
        graphics.fill(leftPos + INVENTORY_X - 4, topPos + INVENTORY_Y - 4, leftPos + INVENTORY_X + 9 * SLOT_SIZE + 4, topPos + INVENTORY_Y + 3 * SLOT_SIZE + 4, 0xFF0A0C0A);
        graphics.fill(leftPos + INVENTORY_X - 4, topPos + INVENTORY_Y + 58, leftPos + INVENTORY_X + 9 * SLOT_SIZE + 4, topPos + INVENTORY_Y + 62 + SLOT_SIZE, 0xFF0A0C0A);

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
        
        int cellBorder = selected ? 0xFFD455FF : 0xFF2A1E2F;
        int cellBg = selected ? 0xFF231728 : 0xFF0E110F;
        
        graphics.fill(left, top, left + SLOT_SIZE, top + SLOT_SIZE, cellBorder);
        graphics.fill(left + 1, top + 1, left + SLOT_SIZE - 1, top + SLOT_SIZE - 1, cellBg);
        
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, left + 1, top + 1);
            graphics.renderItemDecorations(font, stack, left + 1, top + 1);
        }
        
        if (selected) {
            graphics.fill(left, top, left + SLOT_SIZE, top + 1, 0xFF00C8FF);
            graphics.fill(left, top + SLOT_SIZE - 1, left + SLOT_SIZE, top + SLOT_SIZE, 0xFF00C8FF);
            graphics.fill(left, top, left + 1, top + SLOT_SIZE, 0xFF00C8FF);
            graphics.fill(left + SLOT_SIZE - 1, top, left + SLOT_SIZE, top + SLOT_SIZE, 0xFF00C8FF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showHelp) {
            int w = 340;
            int h = 210;
            int x = leftPos + (imageWidth - w) / 2;
            int y = topPos + (imageHeight - h) / 2;

            int ex = x + 15;
            int ey = y + 36;
            int ew = (selectedEquipType == 6) ? 90 : 100;
            int eh = 16;

            int tx = x + 110;
            int ty = y + 36;
            int tw = 85;
            int th = 16;

            int sx = (selectedEquipType == 6) ? x + 200 : x + 120;
            int sy = y + 36;
            int sw = (selectedEquipType == 6) ? 125 : 205;
            int sh = 16;

            // 1. 첫 번째 장비군 드롭다운 버튼 클릭
            if (mouseX >= ex && mouseX <= ex + ew && mouseY >= ey && mouseY <= ey + eh) {
                equipDropdownOpen = !equipDropdownOpen;
                toolDropdownOpen = false;
                statDropdownOpen = false;
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                }
                return true;
            }

            // 2. 첫 번째 장비군 옵션 클릭
            if (equipDropdownOpen) {
                int oy = ey + eh;
                for (int i = 0; i < EQUIP_NAMES.length; i++) {
                    int itemY = oy + i * eh;
                    if (mouseX >= ex && mouseX <= ex + ew && mouseY >= itemY && mouseY <= itemY + eh) {
                        selectedEquipType = i;
                        selectedStatIndex = 0; // 스탯 인덱스 자동 리셋
                        equipDropdownOpen = false;
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                        }
                        return true;
                    }
                }
            }

            // 3. 두 번째 세부 도구 드롭다운 버튼 클릭
            if (selectedEquipType == 6 && mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + th) {
                toolDropdownOpen = !toolDropdownOpen;
                equipDropdownOpen = false;
                statDropdownOpen = false;
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                }
                return true;
            }

            // 4. 두 번째 세부 도구 옵션 클릭
            if (selectedEquipType == 6 && toolDropdownOpen) {
                int oy = ty + th;
                for (int i = 0; i < TOOL_NAMES.length; i++) {
                    int itemY = oy + i * th;
                    if (mouseX >= tx && mouseX <= tx + tw && mouseY >= itemY && mouseY <= itemY + th) {
                        selectedToolType = i;
                        selectedStatIndex = 0; // 스탯 인덱스 자동 리셋
                        toolDropdownOpen = false;
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                        }
                        return true;
                    }
                }
            }

            // 5. 세 번째 스탯 선택 드롭다운 버튼 클릭
            if (mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh) {
                statDropdownOpen = !statDropdownOpen;
                equipDropdownOpen = false;
                toolDropdownOpen = false;
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                }
                return true;
            }

            // 6. 세 번째 스탯 옵션 클릭
            if (statDropdownOpen) {
                List<ReforgeService.Modifier> pool = List.of();
                switch (selectedEquipType) {
                    case 0 -> pool = ReforgeService.MELEE_MODS;
                    case 1 -> pool = ReforgeService.BOW_MODS;
                    case 2 -> pool = ReforgeService.RANGE_MODS;
                    case 3 -> pool = ReforgeService.ARMOR_MODS;
                    case 4 -> pool = ReforgeService.MAGIC_MODS;
                    case 5 -> pool = ReforgeService.CURIO_MODS;
                    case 6 -> {
                        switch (selectedToolType) {
                            case 0 -> pool = ReforgeService.getPool(new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
                            case 1 -> pool = ReforgeService.getPool(new ItemStack(net.minecraft.world.item.Items.DIAMOND_AXE));
                            case 2 -> pool = ReforgeService.getPool(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SHOVEL));
                            case 3 -> pool = ReforgeService.getPool(new ItemStack(net.minecraft.world.item.Items.DIAMOND_HOE));
                            case 4 -> pool = ReforgeService.FISHING_ROD_MODS;
                        }
                    }
                }
                if (pool != null && !pool.isEmpty()) {
                    int oy = sy + sh;
                    for (int i = 0; i < pool.size(); i++) {
                        int itemY = oy + i * sh;
                        if (mouseX >= sx && mouseX <= sx + sw && mouseY >= itemY && mouseY <= itemY + sh) {
                            selectedStatIndex = i;
                            statDropdownOpen = false;
                            if (minecraft != null && minecraft.player != null) {
                                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                            }
                            return true;
                        }
                    }
                }
            }

            // 이외의 영역을 클릭했을 시 도움말 닫기
            showHelp = false;
            equipDropdownOpen = false;
            toolDropdownOpen = false;
            statDropdownOpen = false;
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
            }
            refreshButtons();
            return true;
        }
        if (isAnimating) return false; // 애니메이션 도중 클릭 차단
        
        if (!showHelp && button == 0) {
            int x = leftPos;
            int y = topPos;
            int panelLeft = x - 135;
            int panelWidth = 130;
            int panelTop = y + 18;
            int panelHeight = imageHeight - 36;
            
            if (mouseX >= panelLeft && mouseX <= panelLeft + panelWidth && mouseY >= panelTop && mouseY <= panelTop + panelHeight) {
                ItemStack stack = getSelectedStack();
                int unlocked = ReforgeService.getUnlockedCount(stack);
                
                for (int i = 0; i < 3; i++) {
                    int slotY = panelTop + 14 + i * 53;
                    if (i < unlocked) {
                        int optY = slotY + 10;
                        if (mouseX >= panelLeft + 8 && mouseX <= panelLeft + 20 && mouseY >= optY && mouseY <= optY + 12) {
                            cycleOption(i, false);
                            if (minecraft != null && minecraft.player != null) {
                                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                            }
                            return true;
                        }
                        if (mouseX >= panelLeft + panelWidth - 20 && mouseX <= panelLeft + panelWidth - 8 && mouseY >= optY && mouseY <= optY + 12) {
                            cycleOption(i, true);
                            if (minecraft != null && minecraft.player != null) {
                                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                            }
                            return true;
                        }
                        
                        int rarY = slotY + 22;
                        if (mouseX >= panelLeft + 8 && mouseX <= panelLeft + 20 && mouseY >= rarY && mouseY <= rarY + 12) {
                            cycleRarity(i, false);
                            if (minecraft != null && minecraft.player != null) {
                                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                            }
                            return true;
                        }
                        if (mouseX >= panelLeft + panelWidth - 20 && mouseX <= panelLeft + panelWidth - 8 && mouseY >= rarY && mouseY <= rarY + 12) {
                            cycleRarity(i, true);
                            if (minecraft != null && minecraft.player != null) {
                                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                            }
                            return true;
                        }

                        int valY = slotY + 34;
                        if (mouseX >= panelLeft + 8 && mouseX <= panelLeft + 20 && mouseY >= valY && mouseY <= valY + 12) {
                            cycleValue(i, false);
                            if (minecraft != null && minecraft.player != null) {
                                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                            }
                            return true;
                        }
                        if (mouseX >= panelLeft + 84 && mouseX <= panelLeft + 96 && mouseY >= valY && mouseY <= valY + 12) {
                            cycleValue(i, true);
                            if (minecraft != null && minecraft.player != null) {
                                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                            }
                            return true;
                        }
                    }
                }
                
                int btnY = panelTop + 174;
                if (mouseX >= panelLeft + 10 && mouseX <= panelLeft + panelWidth - 10 && mouseY >= btnY && mouseY <= btnY + 20) {
                    if (unlocked > 0) {
                        autoReforgeActive = !autoReforgeActive;
                        if (autoReforgeActive) {
                            autoReforgeTimer = 0;
                        }
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
                        }
                    }
                    return true;
                }
                
                if (super.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                return true;
            }
        }

        if (button == 0) {
            int inventorySlot = inventorySlotAt(mouseX, mouseY);
            if (inventorySlot >= 0 && minecraft != null && !minecraft.player.getInventory().getItem(inventorySlot).isEmpty()) {
                selectedSlot = inventorySlot;
                autoReforgeActive = false;
                refreshButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        int relativeX = Mth.floor(mouseX) - leftPos;
        int relativeY = Mth.floor(mouseY) - topPos;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                if (insideBox(relativeX, relativeY, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + row * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE)) {
                    return 9 + row * 9 + column;
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            if (insideBox(relativeX, relativeY, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + 62, SLOT_SIZE, SLOT_SIZE)) {
                return column;
            }
        }
        return -1;
    }

    private boolean insideBox(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private ItemStack getSelectedStack() {
        if (minecraft == null || minecraft.player == null || selectedSlot < 0 || selectedSlot >= minecraft.player.getInventory().getContainerSize()) return ItemStack.EMPTY;
        return minecraft.player.getInventory().getItem(selectedSlot);
    }

    @Override
    public void onClose() {
        autoReforgeActive = false;
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox field : valueFields) {
            if (field != null && field.isFocused()) {
                if (keyCode == 256) { // ESC
                    field.setFocused(false);
                    return true;
                }
                return field.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox field : valueFields) {
            if (field != null && field.isFocused()) {
                return field.charTyped(codePoint, modifiers);
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void drawSmallButton(GuiGraphics graphics, int bx, int by, int bw, int bh, String text, boolean hovered, boolean active) {
        int border = active ? (hovered ? 0xFF00C8FF : 0xFF2A1E2F) : 0xFF1C1320;
        int fill = active ? (hovered ? 0xFF140F19 : 0xFF0A0C0A) : 0xFF050505;
        int textColor = active ? (hovered ? 0xFF00FFFF : 0xFFDDDDDD) : 0xFF555555;
        
        graphics.fill(bx, by, bx + bw, by + bh, border);
        graphics.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, fill);
        graphics.drawCenteredString(font, text, bx + bw / 2, by + bh / 2 - 4, textColor);
    }

    private void renderAutoReforgePanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int panelLeft = x - 135;
        int panelWidth = 130;
        int panelTop = y + 18;
        int panelHeight = imageHeight - 36;
        
        framedPanel(graphics, panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF2A1E2F, 0xFF0F0E13);
        
        graphics.drawCenteredString(font, "자동 재련 설정", panelLeft + panelWidth / 2, panelTop + 6, 0xFFE8E1C4);
        graphics.fill(panelLeft + 8, panelTop + 16, panelLeft + panelWidth - 8, panelTop + 17, 0x44FFFFFF);
        
        ItemStack stack = getSelectedStack();
        List<ReforgeService.Modifier> pool = ReforgeService.getPool(stack);
        int unlocked = ReforgeService.getUnlockedCount(stack);
        
        validateAutoTargets();
        
        for (int i = 0; i < 3; i++) {
            int slotY = panelTop + 14 + i * 53;
            
            String slotName = "슬롯 " + (i + 1);
            int titleColor = (i < unlocked) ? 0xFF00C8FF : 0xFF555555;
            graphics.drawString(font, slotName, panelLeft + 10, slotY, titleColor, false);
            if (i >= unlocked) {
                graphics.drawString(font, "(잠김)", panelLeft + 45, slotY, 0xFF555555, false);
            }
            
            boolean active = (i < unlocked) && !isAnimating;
            
            // 1. 옵션 선택 행
            int optY = slotY + 10;
            boolean leftOptHovered = mouseX >= panelLeft + 8 && mouseX <= panelLeft + 20 && mouseY >= optY && mouseY <= optY + 12;
            boolean rightOptHovered = mouseX >= panelLeft + panelWidth - 20 && mouseX <= panelLeft + panelWidth - 8 && mouseY >= optY && mouseY <= optY + 12;
            
            drawSmallButton(graphics, panelLeft + 8, optY, 12, 12, "<", leftOptHovered, active);
            drawSmallButton(graphics, panelLeft + panelWidth - 20, optY, 12, 12, ">", rightOptHovered, active);
            
            String optName = "아무거나";
            if (!autoTargetOption[i].equals("any")) {
                ReforgeService.Modifier foundMod = null;
                for (ReforgeService.Modifier m : pool) {
                    if (m.id().equals(autoTargetOption[i])) {
                        foundMod = m;
                        break;
                    }
                }
                if (foundMod != null) {
                    optName = Component.translatable(foundMod.translationKey()).getString();
                } else {
                    optName = autoTargetOption[i];
                }
            }
            if (optName.length() > 9) {
                optName = optName.substring(0, 8) + "..";
            }
            graphics.drawCenteredString(font, optName, panelLeft + panelWidth / 2, optY + 2, active ? 0xFFFFFFFF : 0xFF555555);
            
            // 2. 등급 선택 행
            int rarY = slotY + 22;
            boolean leftRarHovered = mouseX >= panelLeft + 8 && mouseX <= panelLeft + 20 && mouseY >= rarY && mouseY <= rarY + 12;
            boolean rightRarHovered = mouseX >= panelLeft + panelWidth - 20 && mouseX <= panelLeft + panelWidth - 8 && mouseY >= rarY && mouseY <= rarY + 12;
            
            drawSmallButton(graphics, panelLeft + 8, rarY, 12, 12, "<", leftRarHovered, active);
            drawSmallButton(graphics, panelLeft + panelWidth - 20, rarY, 12, 12, ">", rightRarHovered, active);
            
            String rarName = "아무거나";
            int rarColor = 0xFF888888;
            if (autoTargetRarity[i] != null) {
                rarName = Component.translatable("gui.nogeon_economy_land.reforge_rarity." + autoTargetRarity[i].name().toLowerCase(Locale.ROOT)).getString();
                rarColor = autoTargetRarity[i].color.getColor();
            }
            graphics.drawCenteredString(font, rarName, panelLeft + panelWidth / 2, rarY + 2, active ? rarColor : 0xFF555555);

            // 3. 수치(%) 선택 행
            int valY = slotY + 34;
            boolean leftValHovered = mouseX >= panelLeft + 8 && mouseX <= panelLeft + 20 && mouseY >= valY && mouseY <= valY + 12;
            boolean rightValHovered = mouseX >= panelLeft + 84 && mouseX <= panelLeft + 96 && mouseY >= valY && mouseY <= valY + 12;
            
            drawSmallButton(graphics, panelLeft + 8, valY, 12, 12, "<", leftValHovered, active);
            drawSmallButton(graphics, panelLeft + 84, valY, 12, 12, ">", rightValHovered, active);
            
            graphics.drawString(font, "% 이상", panelLeft + 98, valY + 2, active ? 0xFFE8E1C4 : 0xFF555555, false);
        }
        
        int btnY = panelTop + 174;
        boolean btnHovered = mouseX >= panelLeft + 10 && mouseX <= panelLeft + panelWidth - 10 && mouseY >= btnY && mouseY <= btnY + 20;
        boolean btnActive = unlocked > 0 && !isAnimating;
        
        int border = btnActive ? (btnHovered ? 0xFF00C8FF : (autoReforgeActive ? 0xFFFF5555 : 0xFFD455FF)) : 0xFF1C1320;
        int fill = btnActive ? (btnHovered ? 0xFF140F19 : 0xFF0A0C0A) : 0xFF050505;
        int textColor = btnActive ? (btnHovered ? (autoReforgeActive ? 0xFFFF8888 : 0xFF00FFFF) : (autoReforgeActive ? 0xFFFF5555 : 0xFFE8E1C4)) : 0xFF555555;
        String btnText = autoReforgeActive ? "자동 재련 중지" : "자동 재련 시작";
        
        graphics.fill(panelLeft + 10, btnY, panelLeft + panelWidth - 10, btnY + 20, border);
        graphics.fill(panelLeft + 11, btnY + 1, panelLeft + panelWidth - 11, btnY + 19, fill);
        graphics.drawCenteredString(font, btnText, panelLeft + panelWidth / 2, btnY + 6, textColor);
        
        int statusY = panelTop + 198;
        if (autoReforgeActive) {
            graphics.drawCenteredString(font, "자동 진행 중...", panelLeft + panelWidth / 2, statusY, 0xFF55FF55);
        } else {
            graphics.drawCenteredString(font, "대기 중", panelLeft + panelWidth / 2, statusY, 0xFF888888);
        }
    }

    private void validateAutoTargets() {
        ItemStack stack = getSelectedStack();
        List<ReforgeService.Modifier> pool = ReforgeService.getPool(stack);
        for (int i = 0; i < 3; i++) {
            if (autoTargetOption[i] == null) {
                autoTargetOption[i] = "any";
            }
            if (!autoTargetOption[i].equals("any")) {
                boolean found = false;
                for (ReforgeService.Modifier mod : pool) {
                    if (mod.id().equals(autoTargetOption[i])) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    autoTargetOption[i] = "any";
                }
            }
        }
    }

    private List<String> getOptionList(List<ReforgeService.Modifier> pool) {
        List<String> list = new ArrayList<>();
        list.add("any");
        for (ReforgeService.Modifier mod : pool) {
            list.add(mod.id());
        }
        return list;
    }

    private List<ReforgeService.Rarity> getRarityList() {
        List<ReforgeService.Rarity> list = new ArrayList<>();
        list.add(null);
        for (ReforgeService.Rarity r : ReforgeService.Rarity.values()) {
            list.add(r);
        }
        return list;
    }

    private void cycleOption(int slotIndex, boolean forward) {
        ItemStack stack = getSelectedStack();
        List<ReforgeService.Modifier> pool = ReforgeService.getPool(stack);
        List<String> options = getOptionList(pool);
        String current = autoTargetOption[slotIndex];
        int idx = options.indexOf(current);
        if (idx < 0) idx = 0;
        
        if (forward) {
            idx = (idx + 1) % options.size();
        } else {
            idx = (idx - 1 + options.size()) % options.size();
        }
        autoTargetOption[slotIndex] = options.get(idx);
    }

    private void cycleRarity(int slotIndex, boolean forward) {
        List<ReforgeService.Rarity> rarities = getRarityList();
        ReforgeService.Rarity current = autoTargetRarity[slotIndex];
        int idx = rarities.indexOf(current);
        if (idx < 0) idx = 0;
        
        if (forward) {
            idx = (idx + 1) % rarities.size();
        } else {
            idx = (idx - 1 + rarities.size()) % rarities.size();
        }
        autoTargetRarity[slotIndex] = rarities.get(idx);
    }

    private void cycleValue(int slotIndex, boolean forward) {
        double current = autoTargetValue[slotIndex];
        int pct = (int) Math.round(current * 100.0);
        if (forward) {
            pct = (pct + 1) % 31;
        } else {
            pct = (pct - 1 + 31) % 31;
        }
        autoTargetValue[slotIndex] = pct / 100.0;
    }

    private CompoundTag getReforgeSlotTag(ListTag list, int slotIndex) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (tag.getInt("SlotIndex") == slotIndex) {
                return tag;
            }
        }
        return null;
    }

    private static class AutoReforgeTarget {
        final String option;
        final ReforgeService.Rarity rarity;
        final double value;
        final int originalIndex;

        AutoReforgeTarget(String option, ReforgeService.Rarity rarity, double value, int originalIndex) {
            this.option = option;
            this.rarity = rarity;
            this.value = value;
            this.originalIndex = originalIndex;
        }

        boolean matches(String modId, ReforgeService.Rarity rarity, double value) {
            boolean matchOpt = this.option.equals("any") || modId.equals(this.option);
            boolean matchRar = this.rarity == null || rarity.ordinal() >= this.rarity.ordinal();
            boolean matchVal = this.value <= 0.0001 || value >= this.value;
            return matchOpt && matchRar && matchVal;
        }
    }
}
