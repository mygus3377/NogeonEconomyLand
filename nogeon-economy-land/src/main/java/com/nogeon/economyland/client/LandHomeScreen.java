package com.nogeon.economyland.client;

import com.nogeon.economyland.land.LandFlag;
import com.nogeon.economyland.menu.HomeSummary;
import com.nogeon.economyland.menu.LandHomeMenu;
import com.nogeon.economyland.menu.LandSummary;
import com.nogeon.economyland.network.HomeActionPacket;
import com.nogeon.economyland.network.LandFlagPacket;
import com.nogeon.economyland.network.LandKickPacket;
import com.nogeon.economyland.network.LandPermissionPacket;
import com.nogeon.economyland.network.LandSellPacket;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenAdminLandPacket;
import com.nogeon.economyland.network.OpenWalletPacket;
import com.nogeon.economyland.network.UpdateMemoPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.Map;

public final class LandHomeScreen extends AbstractContainerScreen<LandHomeMenu> {
    private static final int VISIBLE_HOME_ROWS = 5;
    private EditBox homeName;
    private EditBox playerName;
    private EditBox memoBox;
    private final Map<LandFlag, Button> flagButtons = new java.util.EnumMap<>(LandFlag.class);
    private int landIndex;
    private int homeScroll;
    private String memoTarget = "land";
    private String memoTargetId = "";
    private boolean suppressMemoSync;
    private boolean showPermissionPanel = false;

    public LandHomeScreen(LandHomeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 400;
        imageHeight = 266;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        this.imageWidth = this.showPermissionPanel ? 484 : 400;
        super.init();
        landIndex = Math.max(0, Math.min(landIndex, Math.max(0, menu.lands().size() - 1)));
        boolean hasLand = !menu.lands().isEmpty();
        if (hasLand && "land".equals(memoTarget)) {
            memoTargetId = String.valueOf(currentLandId());
        }

        homeName = new EditBox(font, leftPos + 22, topPos + 44, 146, 18, Component.translatable("gui.nogeon_economy_land.home_name"));
        homeName.setMaxLength(20);
        homeName.setValue("home");
        addRenderableWidget(homeName);
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.save_current"),
            button -> ModNetwork.CHANNEL.sendToServer(new HomeActionPacket("save", homeName.getValue().trim())))
            .bounds(leftPos + 174, topPos + 43, 78, 20)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.wallet_tab"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenWalletPacket()))
            .bounds(leftPos + 22, topPos + 204, 64, 20)
            .build());
        if (menu.operator()) {
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.admin_land"),
                button -> ModNetwork.CHANNEL.sendToServer(new OpenAdminLandPacket()))
                .bounds(leftPos + 92, topPos + 204, 92, 20)
                .build());
        }

        String prevName = playerName != null ? playerName.getValue() : "";
        playerName = new EditBox(font, leftPos + 278, topPos + 172, 116, 18, Component.translatable("gui.nogeon_economy_land.land_player"));
        playerName.setMaxLength(16);
        if (!prevName.isEmpty()) {
            playerName.setValue(prevName);
        } else if (!menu.knownPlayers().isEmpty()) {
            playerName.setValue(menu.knownPlayers().get(0));
        }
        addRenderableWidget(playerName);

        memoBox = new EditBox(font, leftPos + 278, topPos + 148, 116, 18, Component.translatable("gui.nogeon_economy_land.memo"));
        memoBox.setMaxLength(32);
        memoBox.setResponder(this::onMemoChanged);
        addRenderableWidget(memoBox);
        updateMemoBox();

        Button previousLandButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("<"),
            button -> shiftLand(-1))
            .bounds(leftPos + 276, topPos + 36, 20, 18)
            .build());
        Button nextLandButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal(">"),
            button -> shiftLand(1))
            .bounds(leftPos + 374, topPos + 36, 20, 18)
            .build());
        previousLandButton.active = menu.lands().size() > 1;
        nextLandButton.active = menu.lands().size() > 1;

        // ON/OFF 스위치를 우측(356)으로 밀고 가로폭(36)으로 축소하여 텍스트 공간을 대폭 확보
        int flagX = leftPos + 356;
        int flagY = topPos + 82;
        flagButtons.clear();
        int flagRow = 0;
        for (LandFlag flag : LandFlag.values()) {
            Button btn = HextechButton.hextechBuilder(Component.literal(""), button -> toggleFlag(currentLandId(), flag))
                .bounds(flagX, flagY + flagRow * 16, 36, 14)
                .build();
            addRenderableWidget(btn);
            flagButtons.put(flag, btn);
            flagRow++;
        }
        updateFlagButtons();

        // 조작계 버튼들의 크기를 높이 20으로 통일하여 누르기 쉽고 유려하게 배치
        Button interactButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.land_allow_interact"),
            button -> sendPermission(currentLandId(), "interact"))
            .bounds(leftPos + 278, topPos + 196, 56, 20)
            .build());
        Button buildButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.land_allow_build"),
            button -> sendPermission(currentLandId(), "build"))
            .bounds(leftPos + 338, topPos + 196, 56, 20)
            .build());
        Button togglePermissionPanelButton = addRenderableWidget(HextechButton.hextechBuilder(
            Component.literal(showPermissionPanel ? "목록 닫기" : "목록 보기"),
            button -> {
                this.showPermissionPanel = !this.showPermissionPanel;
                this.init(this.minecraft, this.width, this.height);
            })
            .bounds(leftPos + 278, topPos + 218, 56, 20)
            .build());
        Button kickButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.land_kick_player"),
            button -> sendKick(currentLandId()))
            .bounds(leftPos + 338, topPos + 218, 56, 20)
            .build());
        Button sellButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.land_sell"),
            button -> sendSell(currentLandId()))
            .bounds(leftPos + 278, topPos + 240, 116, 20)
            .danger(true)
            .build());
        interactButton.active = hasLand;
        buildButton.active = hasLand;
        togglePermissionPanelButton.active = hasLand;
        kickButton.active = hasLand;
        sellButton.active = hasLand;

        if (showPermissionPanel && hasLand) {
            LandSummary land = currentLand();
            if (land != null && land.permissions() != null && !land.permissions().isEmpty()) {
                int i = 0;
                for (Map.Entry<String, String> entry : land.permissions().entrySet()) {
                    if (36 + i * 20 > 230) break;
                    String targetName = entry.getKey();
                    int rowIdx = i;
                    addRenderableWidget(HextechButton.hextechBuilder(Component.literal("X"),
                        button -> {
                            ModNetwork.CHANNEL.sendToServer(new LandPermissionPacket(currentLandId(), targetName, "none"));
                            this.init(this.minecraft, this.width, this.height);
                        })
                        .bounds(leftPos + 462, topPos + 37 + rowIdx * 20, 14, 14)
                        .danger(true)
                        .build());
                    i++;
                }
            }
        }
        playerName.setEditable(hasLand);

        homeScroll = Mth.clamp(homeScroll, 0, maxHomeScroll());
        for (int row = 0; row < Math.min(VISIBLE_HOME_ROWS, menu.homes().size() - homeScroll); row++) {
            HomeSummary home = menu.homes().get(homeScroll + row);
            int y = topPos + 84 + row * 20;
            addRenderableWidget(HextechButton.hextechBuilder(Component.literal("M"),
                button -> selectHomeMemo(home))
                .bounds(leftPos + 154, y - 2, 18, 18)
                .build());
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.go"),
                button -> ModNetwork.CHANNEL.sendToServer(new HomeActionPacket("go", home.name())))
                .bounds(leftPos + 174, y - 2, 34, 18)
                .build());
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.delete"),
                button -> ModNetwork.CHANNEL.sendToServer(new HomeActionPacket("delete", home.name())))
                .bounds(leftPos + 212, y - 2, 40, 18)
                .danger(true)
                .build());
        }
    }

    private void toggleFlag(int landId, LandFlag flag) {
        LandSummary land = currentLand();
        if (land != null) {
            boolean current = land.flags().getOrDefault(flag.id(), flag.defaultValue());
            ModNetwork.CHANNEL.sendToServer(new LandFlagPacket(landId, flag.id(), !current));
        }
    }

    private void selectHomeMemo(HomeSummary home) {
        memoTarget = "home";
        memoTargetId = home.name();
        updateMemoBox();
    }

    private void updateMemoBox() {
        if (memoBox == null) return;
        if ("home".equals(memoTarget)) {
            for (HomeSummary home : menu.homes()) {
                if (home.name().equals(memoTargetId)) {
                    setMemoBoxValue(home.memo());
                    return;
                }
            }
        } else {
            LandSummary land = currentLand();
            if (land != null) {
                memoTargetId = String.valueOf(land.id());
                setMemoBoxValue(land.memo());
            } else {
                setMemoBoxValue("");
            }
        }
    }

    private void onMemoChanged(String value) {
        if (suppressMemoSync) {
            return;
        }
        String trimmed = value.trim();
        syncLocalMemo(trimmed);
        if (!memoTargetId.isBlank()) {
            ModNetwork.CHANNEL.sendToServer(new UpdateMemoPacket(memoTarget, memoTargetId, trimmed));
        }
    }

    private void setMemoBoxValue(String value) {
        suppressMemoSync = true;
        memoBox.setValue(value);
        suppressMemoSync = false;
    }

    private void syncLocalMemo(String memo) {
        if ("home".equals(memoTarget)) {
            for (int index = 0; index < menu.homes().size(); index++) {
                HomeSummary home = menu.homes().get(index);
                if (home.name().equals(memoTargetId)) {
                    menu.homes().set(index, new HomeSummary(home.name(), home.world(), home.x(), home.y(), home.z(), memo));
                    return;
                }
            }
            return;
        }

        if (memoTargetId.isBlank()) {
            return;
        }
        for (int index = 0; index < menu.lands().size(); index++) {
            LandSummary land = menu.lands().get(index);
            if (String.valueOf(land.id()).equals(memoTargetId)) {
                menu.lands().set(index, new LandSummary(land.id(), land.typeKey(), land.world(), land.blocks(), land.x(), land.y(), land.z(), memo, land.flags(), land.permissions()));
                return;
            }
        }
    }

    private void sendPermission(int landId, String permission) {
        if (landId > 0 && playerName != null && !playerName.getValue().trim().isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new LandPermissionPacket(landId, playerName.getValue().trim(), permission));
        }
    }

    private void sendKick(int landId) {
        if (landId > 0 && playerName != null && !playerName.getValue().trim().isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new LandKickPacket(landId, playerName.getValue().trim()));
        }
    }

    private void sendSell(int landId) {
        if (landId > 0) {
            ModNetwork.CHANNEL.sendToServer(new LandSellPacket(landId));
        }
    }

    private void updateFlagButtons() {
        LandSummary land = currentLand();
        boolean hasLand = land != null;
        for (Map.Entry<LandFlag, Button> entry : flagButtons.entrySet()) {
            LandFlag flag = entry.getKey();
            Button btn = entry.getValue();
            boolean active = hasLand && land.flags().getOrDefault(flag.id(), flag.defaultValue());
            btn.setMessage(Component.literal(active ? "ON" : "OFF"));
            btn.active = hasLand;
        }
    }

    private void shiftLand(int delta) {
        if (menu.lands().isEmpty()) {
            return;
        }
        landIndex = (landIndex + delta + menu.lands().size()) % menu.lands().size();
        memoTarget = "land";
        updateMemoBox();
        updateFlagButtons();
    }

    private int currentLandId() {
        LandSummary land = currentLand();
        return land == null ? -1 : land.id();
    }

    private LandSummary currentLand() {
        if (menu.lands().isEmpty()) {
            return null;
        }
        landIndex = Math.max(0, Math.min(landIndex, menu.lands().size() - 1));
        return menu.lands().get(landIndex);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        // 1. 프리미엄 헥스테크 미드나이트-다크 & 시안 네온 그라데이션 라인 테두리
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); // 칠흑
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); // 미드나이트 그린 내벽
        
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // 상단 Cyan 네온
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 하단 Blue 네온
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); // 좌측
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 우측

        // 2. 내부 레이아웃 분할
        // 좌상단 홈 생성 체임버
        graphics.fill(x + 16, y + 34, x + 258, y + 68, 0xFF0E1311);
        drawCustomBorder(graphics, x + 16, y + 34, 242, 34, 0xFF1B2C27);

        // 좌하단 홈 목록 대시보드
        graphics.fill(x + 16, y + 76, x + 258, y + 198, 0xFF111715);
        drawCustomBorder(graphics, x + 16, y + 76, 242, 122, 0xFF22312A);

        // 우측 영지 및 컨트롤 판넬 일체형 통합 (상하 쪼개지던 단절 현상 완벽 해소)
        graphics.fill(x + 272, y + 14, x + 400, y + 262, 0xFF0E1311);
        drawCustomBorder(graphics, x + 272, y + 14, 128, 248, 0xFF1B2C27);

        // 신규 [ 마공학 권한 승인 목록 판넬 ]
        if (showPermissionPanel) {
            graphics.fill(x + 400, y + 14, x + 480, y + 262, 0xFF0E1311);
            drawCustomBorder(graphics, x + 400, y + 14, 80, 248, 0xFF1B2C27);
            graphics.fill(x + 400, y + 14, x + 402, y + 262, 0xFF00FFCC); // 승인 판넬 네온 가이드 액센트
        }
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= leftPos + 16 && mouseX < leftPos + 258 && mouseY >= topPos + 76 && mouseY < topPos + 198 && maxHomeScroll() > 0) {
            int previous = homeScroll;
            homeScroll = Mth.clamp(homeScroll + (delta < 0.0D ? 1 : -1), 0, maxHomeScroll());
            if (homeScroll != previous) {
                init();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2 - 24, 8, 0xFF00FFCC);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.home_name"), 22, 30, 0xFF769B8E, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_status"), 278, 20, 0xFF769B8E, false);
        
        LandSummary land = currentLand();
        if (land != null) {
            // 깨진 번역 키 대신 실제 플레이어가 결정한 영지 이름(memo)을 최우선으로 유려하게 표출
            String displayName = land.memo();
            if (displayName == null || displayName.trim().isEmpty()) {
                String typeKor = "기본";
                if (land.typeKey().contains("normal")) typeKor = "일반";
                else if (land.typeKey().contains("industrial")) typeKor = "산업";
                else if (land.typeKey().contains("admin")) typeKor = "관리자";
                displayName = "[" + typeKor + "] 토지 #" + land.id();
            }
            graphics.drawString(font, font.plainSubstrByWidth(displayName, 90), 304, 38, 0xFFE8E1C4, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_page", landIndex + 1, menu.lands().size()), 304, 48, 0xFF98A49C, false);
            graphics.drawString(font, "#" + land.id() + "  " + land.blocks() + "B", 304, 58, 0xFF98A49C, false);
            graphics.drawString(font, shortWorld(land.world()) + " " + land.x() + ", " + land.y() + ", " + land.z(), 278, 68, 0xFF7E887D, false);

            int flagIdx = 0;
            for (LandFlag flag : LandFlag.values()) {
                // 텍스트 자르기 너비를 78px로 확장하여 "몬스터 소환 방지" 등의 한글 문구가 절대 중간에 잘리지 않도록 보호
                drawClippedString(graphics, Component.translatable(flag.translationKey()), 276, 85 + flagIdx * 16, 78, 0xFF98A49C);
                flagIdx++;
            }

            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.memo").append(": ").append(memoTarget.equals("land") ? "" : memoTargetId), 278, 134, 0xFF98A49C, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_no_owned"), 278, 54, 0xFF98A49C, false);
        }

        // 우측 영지 권한 승인 목록 판넬 그리기
        if (showPermissionPanel) {
            graphics.drawCenteredString(font, "권한 허가 목록", 440, 20, 0xFF00FFCC);
            if (land != null && land.permissions() != null && !land.permissions().isEmpty()) {
                int i = 0;
                for (Map.Entry<String, String> entry : land.permissions().entrySet()) {
                    if (36 + i * 20 > 230) break; // 오버플로우 방지
                    String name = entry.getKey();
                    String type = entry.getValue();
                    String typeKor = "build".equals(type) ? "건축" : "상호";
                    int typeColor = "build".equals(type) ? 0xFF55FF55 : 0xFF55FFFF;
                    
                    String display = font.plainSubstrByWidth(name, 50);
                    // 이름은 윗줄에 렌더링
                    graphics.drawString(font, display, 406, 36 + i * 20, 0xFFE8E1C4, false);
                    // 권한 표시는 아랫줄에 작게 렌더링
                    graphics.drawString(font, "(" + typeKor + ")", 406, 45 + i * 20, typeColor, false);
                    i++;
                }
            } else {
                graphics.drawCenteredString(font, "허가된 플레이어", 440, 110, 0xFF4A6057);
                graphics.drawCenteredString(font, "없음", 440, 122, 0xFF4A6057);
            }
        }

        homeScroll = Mth.clamp(homeScroll, 0, maxHomeScroll());
        for (int row = 0; row < Math.min(VISIBLE_HOME_ROWS, menu.homes().size() - homeScroll); row++) {
            HomeSummary home = menu.homes().get(homeScroll + row);
            int y = 84 + row * 20;
            graphics.drawString(font, font.plainSubstrByWidth(home.name(), 40), 24, y, 0xFFE8E1C4, false);
            graphics.drawString(font, font.plainSubstrByWidth(home.memo(), 40), 66, y, 0xFF7E887D, false);
            graphics.drawString(font, font.plainSubstrByWidth(shortWorld(home.world()) + " " + home.x() + "," + home.z(), 45), 108, y, 0xFF98A49C, false);
        }
        if (maxHomeScroll() > 0) {
            graphics.drawString(font, Component.literal((homeScroll + 1) + "/" + (maxHomeScroll() + 1)), 224, 184, 0xFF98A49C, false);
            int trackX = 248;
            int trackTop = 84;
            int trackHeight = 94;
            graphics.fill(trackX, trackTop, trackX + 4, trackTop + trackHeight, 0xFF10140F);
            int handleHeight = Math.max(16, trackHeight * VISIBLE_HOME_ROWS / menu.homes().size());
            int handleTop = trackTop + (trackHeight - handleHeight) * homeScroll / maxHomeScroll();
            graphics.fill(trackX, handleTop, trackX + 4, handleTop + handleHeight, 0xFF8A8268);
        }

        if (menu.homes().isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.no_homes"), 24, 92, 0xFF98A49C, false);
        }

        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_trade_hint"), 22, 184, 0xFF98A49C, false);
    }

    private void drawClippedString(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private String shortWorld(String worldKey) {
        int separator = worldKey.indexOf(':');
        return separator >= 0 && separator + 1 < worldKey.length() ? worldKey.substring(separator + 1) : worldKey;
    }

    private int maxHomeScroll() {
        return Math.max(0, menu.homes().size() - VISIBLE_HOME_ROWS);
    }
}
