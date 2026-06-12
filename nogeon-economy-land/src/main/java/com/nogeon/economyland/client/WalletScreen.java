package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.WalletMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenAuctionPacket;
import com.nogeon.economyland.network.OpenHelpPacket;
import com.nogeon.economyland.network.OpenLandHomePacket;
import com.nogeon.economyland.network.OpenJobChangePacket;
import com.nogeon.economyland.network.OpenAdminCommandPacket;
import com.nogeon.economyland.network.OpenSkillsPacket;
import com.nogeon.economyland.network.OpenTradeBrowserPacket;
import com.nogeon.economyland.network.SpawnReturnPacket;
import com.nogeon.economyland.network.TogglePeacefulFlagPacket;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class WalletScreen extends AbstractContainerScreen<WalletMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private boolean settingsOpen;
    private boolean statsOpen;
    private boolean playerStatsOpen;

    public WalletScreen(WalletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 264;
        imageHeight = 206;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        
        // ⚙ 설정 버튼
        addRenderableWidget(HextechButton.hextechBuilder(Component.literal("⚙"),
            button -> {
                settingsOpen = !settingsOpen;
                statsOpen = false;
                playerStatsOpen = false;
                rebuildWidgets();
            })
            .bounds(leftPos + 232, topPos + 12, 16, 16)
            .build());
            
        // ? 도움말 버튼
        addRenderableWidget(HextechButton.hextechBuilder(Component.literal("?"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenHelpPacket()))
            .bounds(leftPos + 212, topPos + 12, 16, 16)
            .build());
            
        // 스폰 귀환 버튼
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.spawn_return"),
            button -> ModNetwork.CHANNEL.sendToServer(new SpawnReturnPacket()))
            .bounds(leftPos + 16, topPos + 12, 84, 16)
            .build());
            
        // P 평화 플래그 버튼
        addRenderableWidget(HextechButton.hextechBuilder(Component.literal("P"),
            button -> ModNetwork.CHANNEL.sendToServer(new TogglePeacefulFlagPacket()))
            .tooltip(Tooltip.create(Component.translatable("tooltip.nogeon_economy_land.peaceful_flag")))
            .bounds(leftPos + 192, topPos + 12, 16, 16)
            .build());

        if (!settingsOpen) {
            addRenderableWidget(HextechButton.hextechBuilder(Component.literal("📊"),
                button -> {
                    statsOpen = !statsOpen;
                    playerStatsOpen = false;
                    rebuildWidgets();
                })
                .tooltip(Tooltip.create(Component.literal("강화 통계 보기")))
                .bounds(leftPos + 172, topPos + 12, 16, 16)
                .build());

            addRenderableWidget(HextechButton.hextechBuilder(Component.literal("👤"),
                button -> {
                    playerStatsOpen = !playerStatsOpen;
                    statsOpen = false;
                    rebuildWidgets();
                })
                .tooltip(Tooltip.create(Component.literal("총합 전투 능력치 보기")))
                .bounds(leftPos + 152, topPos + 12, 16, 16)
                .build());
        }

        if (menu.admin()) {
            addRenderableWidget(HextechButton.hextechBuilder(Component.literal("관리"),
                button -> ModNetwork.CHANNEL.sendToServer(new OpenAdminCommandPacket()))
                .tooltip(Tooltip.create(Component.literal("OP 전용 관리자 명령 콘솔")))
                .bounds(leftPos + 104, topPos + 12, 48, 16)
                .build());
        }
            
        if (settingsOpen) {
            initSettingsButtons();
            return;
        }
        
        // 네비게이션 포탈 버튼들 (마공학 매트릭스 게이트)
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.skills"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenSkillsPacket()))
            .bounds(leftPos + 16, topPos + 146, 72, 20)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.land"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenLandHomePacket()))
            .bounds(leftPos + 96, topPos + 146, 72, 20)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.job_change"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenJobChangePacket("")))
            .bounds(leftPos + 176, topPos + 146, 72, 20)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.auction"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenAuctionPacket()))
            .bounds(leftPos + 16, topPos + 170, 112, 20)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenTradeBrowserPacket()))
            .bounds(leftPos + 136, topPos + 170, 112, 20)
            .build());
    }

    private static class SettingOption {
        private final Component label;
        private final java.util.function.BooleanSupplier getter;
        private final java.util.function.Consumer<Boolean> setter;
        private final Component tooltip;

        public SettingOption(Component label, java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter, Component tooltip) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.tooltip = tooltip;
        }
    }

    private final List<SettingOption> settingsList = List.of(
        new SettingOption(
            Component.literal("⚔ 무기 강화 이펙트"),
            () -> ClientConfig.weaponVfx,
            val -> ClientConfig.weaponVfx = val,
            Component.literal("무기에서 방사되는 1인칭 및 3인칭 강화 에너지를 표시합니다.")
        ),
        new SettingOption(
            Component.literal("🛡 갑옷 용오름 오라"),
            () -> ClientConfig.armorVfx,
            val -> ClientConfig.armorVfx = val,
            Component.literal("갑옷 강화에 따른 플레이어 주변의 솟구치는 용오름과 오라를 표시합니다.")
        ),
        new SettingOption(
            Component.literal("💎 인벤 강화 반짝임"),
            () -> ClientConfig.itemVfx,
            val -> ClientConfig.itemVfx = val,
            Component.literal("인벤토리 및 단축바의 강화 아이템 슬롯 뒷배경 광채/반짝임 효과를 표시합니다.")
        ),
        new SettingOption(
            Component.literal("💥 타격 충격파 이펙트"),
            () -> ClientConfig.hitVfx,
            val -> ClientConfig.hitVfx = val,
            Component.literal("16강 이상의 무기로 타격할 때의 에너지 충격파를 표시합니다.")
        ),
        new SettingOption(
            Component.literal("🎵 묵직한 타격 사운드"),
            () -> ClientConfig.soundVfx,
            val -> ClientConfig.soundVfx = val,
            Component.literal("16강 이상의 무기로 타격 시 묵직한 타격 사운드를 재생합니다.")
        )
    );

    private void initSettingsButtons() {
        int index = 0;
        int startY = topPos + 44;
        int spacing = 20;
        
        for (SettingOption option : settingsList) {
            int yPos = startY + index * spacing;
            addRenderableWidget(HextechButton.hextechBuilder(
                vfxMessage(option.label, option.getter.getAsBoolean()),
                button -> {
                    boolean nextVal = !option.getter.getAsBoolean();
                    option.setter.accept(nextVal);
                    ClientConfig.save();
                    button.setMessage(vfxMessage(option.label, nextVal));
                })
                .tooltip(Tooltip.create(option.tooltip))
                .bounds(leftPos + 42, yPos, 180, 16)
                .build());
            index++;
        }
        
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.back"),
            button -> {
                settingsOpen = false;
                playerStatsOpen = false;
                rebuildWidgets();
            })
            .bounds(leftPos + 96, topPos + 172, 72, 18)
            .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        // 1. 헥스테크 프리미엄 네온 테두리 & 배경
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); // 칠흑
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); // 미드나이트 그린 내벽
        
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // 상단 Cyan 네온
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 하단 Blue 네온
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); // 좌측
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 우측

        // 2. 내부 레이아웃 분할
        if (!settingsOpen && !statsOpen && !playerStatsOpen) {
            // 크레딧 넥서스 판넬 (상단)
            graphics.fill(x + 14, y + 34, x + imageWidth - 14, y + 70, 0xFF0E1311);
            drawCustomBorder(graphics, x + 14, y + 34, imageWidth - 28, 36, 0xFF1B2C27);
            graphics.fill(x + 14, y + 34, x + 16, y + 70, 0xFF00FFCC); // 크레딧 고유 액센트 라인
 
            // 성장 데이터 대시보드 판넬 (중단)
            graphics.fill(x + 14, y + 76, x + imageWidth - 14, y + 138, 0xFF111715);
            drawCustomBorder(graphics, x + 14, y + 76, imageWidth - 28, 62, 0xFF22312A);
        } else {
            // 설정 또는 통계/능력치 패널 (더 넓은 영역)
            graphics.fill(x + 14, y + 34, x + imageWidth - 14, y + 166, 0xFF0E1311);
            drawCustomBorder(graphics, x + 14, y + 34, imageWidth - 28, 132, 0xFF1B2C27);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderDowngradeScrollTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 프리미엄 홀로그램 타이틀
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFF00FFCC);
        
        if (settingsOpen) {
            graphics.drawCenteredString(font, Component.translatable("options.title"), imageWidth / 2, 36, 0xFF00FFCC);
            return;
        }
        
        // 1. 크레딧 정보 렌더링
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.credit_balance"), 24, 40, 0xFF769B8E, false);
        graphics.drawString(font, Component.literal(CREDIT_FORMAT.format(menu.credits()) + " ")
            .append(Component.translatable("currency.nogeon_economy_land.credits")), 24, 54, 0xFFFFD56A, false);

        if (statsOpen) {
            // 2. 강화 통계 정보 렌더링
            graphics.drawString(font, Component.literal("대장간 강화 통계").withStyle(ChatFormatting.BOLD), 22, 42, 0xFF00FFCC, false);
            graphics.drawString(font, Component.literal("누적 시도: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(menu.totalEnhanceAttempts() + "회").withStyle(ChatFormatting.WHITE)), 22, 60, 0xFFFFFFFF, false);
            graphics.drawString(font, Component.literal("누적 소모: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(CREDIT_FORMAT.format(menu.totalEnhanceSpent()) + " C").withStyle(ChatFormatting.YELLOW)), 22, 74, 0xFFFFFFFF, false);
            
            graphics.drawString(font, Component.literal("최고 기록: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("+" + menu.highestEnhanceLevel()).withStyle(ChatFormatting.GOLD)), 146, 60, 0xFFFFFFFF, false);
            graphics.drawString(font, Component.literal("실패 횟수: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(menu.totalEnhanceFails() + "회").withStyle(ChatFormatting.RED)), 146, 74, 0xFFFFFFFF, false);
            
            graphics.drawString(font, Component.literal("※ 누적 통계는 대장간 직접 강화 시도 시 집계됩니다.").withStyle(ChatFormatting.DARK_GRAY), 22, 92, 0xFFFFFFFF, false);
            return;
        }
 
        if (playerStatsOpen) {
            graphics.drawString(font, Component.literal("총합 전투 능력치").withStyle(ChatFormatting.BOLD), 22, 42, 0xFF00FFCC, false);
            net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                ItemStack mainHand = player.getMainHandItem();
                String weaponName = mainHand.isEmpty() ? "맨손" : mainHand.getHoverName().getString();
                int enhanceLevel = mainHand.isEmpty() ? 0 : mainHand.getOrCreateTag().getInt("NoGeonEnhanceLevel");
                String enhanceSuffix = enhanceLevel > 0 ? " (+" + enhanceLevel + "강)" : "";
                
                graphics.drawString(font, Component.literal("장착 무기: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(weaponName + enhanceSuffix).withStyle(ChatFormatting.GOLD)), 22, 54, 0xFFFFFFFF, false);
 
                double maxHealth = player.getMaxHealth();
                double armor = player.getArmorValue();
                double armorToughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                double moveSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
                double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
 
                String speedStr = String.format(Locale.US, "%.0f%%", (moveSpeed / 0.1D) * 100.0D);
 
                graphics.drawString(font, Component.literal("체력: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.US, "%.1f", maxHealth)).withStyle(ChatFormatting.RED)), 22, 68, 0xFFFFFFFF, false);
                graphics.drawString(font, Component.literal("공격/속도: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.US, "%.1f", attackDamage)).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format(Locale.US, "%.2f", attackSpeed)).withStyle(ChatFormatting.AQUA)), 22, 82, 0xFFFFFFFF, false);
 
                graphics.drawString(font, Component.literal("방어/강도: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.US, "%.0f", armor)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format(Locale.US, "%.0f", armorToughness)).withStyle(ChatFormatting.DARK_GREEN)), 132, 68, 0xFFFFFFFF, false);
                graphics.drawString(font, Component.literal("이동 속도: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(speedStr).withStyle(ChatFormatting.LIGHT_PURPLE)), 132, 82, 0xFFFFFFFF, false);
 
                // 재련 능력치 합산
                double meleeDmgBonus = getReforgeValueSum(player, "melee_damage") * 100.0D;
                double critChance = getReforgeValueSum(player, "crit_chance") * 100.0D;
                double lifesteal = getReforgeValueSum(player, "lifesteal") * 100.0D;
                double evasion = getReforgeValueSum(player, "evasion") * 100.0D;
 
                graphics.drawString(font, Component.literal("재련 추가 능력치").withStyle(ChatFormatting.UNDERLINE), 22, 100, 0xFF5FA38F, false);
                graphics.drawString(font, Component.literal("피해 증가: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.US, "+%.1f%%", meleeDmgBonus)).withStyle(ChatFormatting.YELLOW)), 22, 114, 0xFFFFFFFF, false);
                graphics.drawString(font, Component.literal("치명타 확률: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.US, "+%.1f%%", critChance)).withStyle(ChatFormatting.GOLD)), 22, 128, 0xFFFFFFFF, false);
 
                graphics.drawString(font, Component.literal("생명력 흡수: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.US, "+%.1f%%", lifesteal)).withStyle(ChatFormatting.DARK_RED)), 132, 114, 0xFFFFFFFF, false);
                graphics.drawString(font, Component.literal("회피율: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.US, "+%.1f%%", evasion)).withStyle(ChatFormatting.DARK_AQUA)), 132, 128, 0xFFFFFFFF, false);
 
                com.tacz.guns.api.item.IGun iGun = com.tacz.guns.api.item.IGun.getIGunOrNull(mainHand);
                if (iGun != null) {
                    net.minecraft.resources.ResourceLocation gunId = iGun.getGunId(mainHand);
                    com.tacz.guns.resource.index.CommonGunIndex index = com.tacz.guns.api.TimelessAPI.getCommonGunIndex(gunId).orElse(null);
                    if (index != null && index.getGunData() != null && index.getGunData().getBulletData() != null) {
                        float gunDamage = index.getGunData().getBulletData().getDamageAmount();
                        graphics.drawString(font, Component.literal("총기 피해량: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(String.format(Locale.US, "%.1f", gunDamage)).withStyle(ChatFormatting.YELLOW)), 22, 146, 0xFFFFFFFF, false);
                    }
                }
            }
            return;
        }

        // 2. 수호 충전 횟수 및 정보
        graphics.drawString(font, Component.literal("보호 및 포인트 정보"), 22, 82, 0xFF5FA38F, false);
        graphics.drawString(font, Component.literal("인벤 보존: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(menu.inventoryKeepCharges())).withStyle(ChatFormatting.GREEN)), 22, 94, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.literal("하락 방지권").withStyle(ChatFormatting.AQUA), 110, 94, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.literal("남은 SP: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(menu.skillPoints())).withStyle(ChatFormatting.GOLD)), 196, 94, 0xFFFFFFFF, false);

        // 3. 직업 정보 & 영지 홈
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.current_job").withStyle(ChatFormatting.GRAY), 22, 108, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable("job.nogeon_economy_land." + menu.jobId())
            .append(" Lv.").append(String.valueOf(menu.jobLevel())).withStyle(ChatFormatting.YELLOW), 84, 108, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.literal("영지 홈: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(menu.homeCount())).withStyle(ChatFormatting.LIGHT_PURPLE)), 174, 108, 0xFFFFFFFF, false);

        // 4. 직업 경험치 게이지바
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.job_exp").withStyle(ChatFormatting.GRAY), 22, 122, 0xFFFFFFFF, false);
        drawExpBar(graphics, 84, 120, 154, 10);
        graphics.drawCenteredString(font, Component.literal(menu.jobExp() + " / " + menu.jobExpToNextLevel()), 161, 121, 0xFFFFFFFF);
    }

    private void drawExpBar(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF080D0B); // 매트릭스 진공 배경
        drawCustomBorder(graphics, x, y, width, height, 0xFF1B2C27); // 네온 가이드라인
        int filled = menu.jobExpToNextLevel() <= 0 ? width - 2 : (int) ((width - 2) * (double) menu.jobExp() / menu.jobExpToNextLevel());
        if (filled > 0) {
            // 청록 네온으로 부드럽게 채워지는 경험치 바
            graphics.fill(x + 1, y + 1, x + 1 + Math.max(0, Math.min(width - 2, filled)), y + height - 1, 0xFF00FFCC);
        }
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void renderDowngradeScrollTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (settingsOpen || statsOpen || playerStatsOpen || !insideBox(mouseX, mouseY, leftPos + 108, topPos + 92, 78, 12)) {
            return;
        }
        graphics.renderComponentTooltip(font, List.of(
            Component.literal("방지권 등록 현황").withStyle(ChatFormatting.GOLD),
            Component.literal("+6~+10 하락방지: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("x" + menu.lowDowngradeScrolls()).withStyle(ChatFormatting.AQUA)),
            Component.literal("+11~+15 하락방지: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("x" + menu.midDowngradeScrolls()).withStyle(ChatFormatting.AQUA)),
            Component.literal("+16~+17 하락방지: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("x" + menu.highDowngradeScrolls()).withStyle(ChatFormatting.GOLD)),
            Component.literal("+18~+20 하락방지: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("x" + menu.highestDowngradeScrolls()).withStyle(ChatFormatting.LIGHT_PURPLE)),
            Component.literal("초기화 방지: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("x" + menu.resetProtectionScrolls()).withStyle(ChatFormatting.GREEN)),
            Component.literal("하락 방지권은 초기화를 막지 못합니다.").withStyle(ChatFormatting.RED)
        ), mouseX, mouseY);
    }

    private static boolean insideBox(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static Component vfxMessage(Component target, boolean enabled) {
        return target.copy().append(" ").append(Component.translatable("gui.nogeon_economy_land.vfx_toggle")).append(": ").append(enabled ? "ON" : "OFF");
    }
 
    private static double getReforgeValueSum(net.minecraft.world.entity.player.Player player, String modifierId) {
        double sum = 0.0D;
        sum += getItemReforgeValue(player.getMainHandItem(), modifierId);
        for (ItemStack armorStack : player.getArmorSlots()) {
            sum += getItemReforgeValue(armorStack, modifierId);
        }
        return sum;
    }
 
    private static double getItemReforgeValue(ItemStack stack, String modifierId) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0.0D;
        }
        double sum = 0.0D;
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(com.nogeon.economyland.item.ReforgeService.REFORGE_TAG, net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag list = tag.getList(com.nogeon.economyland.item.ReforgeService.REFORGE_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                net.minecraft.nbt.CompoundTag slot = list.getCompound(i);
                if (modifierId.equals(slot.getString("ModifierId"))) {
                    sum += slot.getDouble("Value");
                }
            }
        }
        return sum;
    }
}
