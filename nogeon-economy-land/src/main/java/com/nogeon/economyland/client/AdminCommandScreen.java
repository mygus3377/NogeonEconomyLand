package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.AdminCommandMenu;
import com.nogeon.economyland.network.AdminCommandExecutePacket;
import com.nogeon.economyland.network.ModNetwork;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AdminCommandScreen extends AbstractContainerScreen<AdminCommandMenu> {
    private static final int PLAYER_ROWS = 8;
    private static final int COMMAND_ROWS = 10;

    private EditBox targetBox;
    private EditBox valueBox;
    private EditBox jobBox;
    private EditBox classBox;
    private EditBox rawBox;
    private int playerOffset;
    private String selectedPlayer = "";

    public AdminCommandScreen(AdminCommandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 420;
        imageHeight = 292;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        targetBox = addBox(leftPos + 24, topPos + 48, 114, 18, "대상");
        valueBox = addBox(leftPos + 146, topPos + 48, 70, 18, "값");
        jobBox = addBox(leftPos + 224, topPos + 48, 74, 18, "직업");
        classBox = addBox(leftPos + 306, topPos + 48, 90, 18, "계급");
        rawBox = addBox(leftPos + 24, topPos + 254, 282, 18, "직접 명령어");
        rawBox.setMaxLength(1024);

        if (!selectedPlayer.isBlank()) {
            targetBox.setValue(selectedPlayer);
        }
        valueBox.setValue("100");
        jobBox.setValue("miner");
        classBox.setValue("BILLIONAIRE");

        addRenderableWidget(Button.builder(Component.literal("실행"), button -> run(rawBox.getValue()))
            .bounds(leftPos + 314, topPos + 254, 82, 18)
            .tooltip(Tooltip.create(Component.literal("입력한 명령어를 OP 권한으로 실행합니다.")))
            .build());

        addRenderableWidget(Button.builder(Component.literal("▲"), button -> scrollPlayers(-1))
            .bounds(leftPos + 144, topPos + 78, 18, 16).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), button -> scrollPlayers(1))
            .bounds(leftPos + 144, topPos + 228, 18, 16).build());

        buildPlayerButtons();
        buildCommandButtons();
    }

    private EditBox addBox(int x, int y, int width, int height, String hint) {
        EditBox box = new EditBox(font, x, y, width, height, Component.literal(hint));
        box.setHint(Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(box);
        return box;
    }

    private void buildPlayerButtons() {
        List<String> players = menu.onlinePlayers();
        for (int row = 0; row < PLAYER_ROWS; row++) {
            int index = playerOffset + row;
            Component label = index < players.size() ? Component.literal(players.get(index)) : Component.literal("-");
            Button button = addRenderableWidget(Button.builder(label, ignored -> {
                if (index < players.size()) {
                    selectPlayer(players.get(index));
                }
            }).bounds(leftPos + 24, topPos + 78 + row * 18, 114, 16).build());
            button.active = index < players.size();
        }
    }

    private void buildCommandButtons() {
        int x = leftPos + 176;
        int y = topPos + 82;
        addCommandButton(x, y, 0, "크레딧 설정", "credit set {player} {value}");
        addCommandButton(x, y, 1, "직업 레벨", "job level set {player} {job} {value}");
        addCommandButton(x, y, 2, "직업 초기화", "job reset {player} {job}");
        addCommandButton(x, y, 3, "전체 직업 초기화", "job resetall {player}");
        addCommandButton(x, y, 4, "계급 설정", "socialclass set {player} {class}");
        addCommandButton(x, y, 5, "스폰 이동", "tp {player} -70 82 830");
        addCommandButton(x, y, 6, "상태 복구", "economyadmin unstuckplayer {player}");
        addCommandButton(x, y, 7, "전체 직업 데이터 초기화", "economyadmin jobs resetall");
        addCommandButton(x, y, 8, "패치 종료", "economyadmin stop patch");
        addCommandButton(x, y, 9, "도움말", "help nogeon");
    }

    private void addCommandButton(int x, int y, int row, String label, String template) {
        int leftRows = (COMMAND_ROWS + 1) / 2;
        int column = row >= leftRows ? 1 : 0;
        int localRow = column == 0 ? row : row - leftRows;
        addRenderableWidget(Button.builder(Component.literal(label), button -> run(applyTemplate(template)))
            .bounds(x + column * 112, y + localRow * 24, 104, 18)
            .tooltip(Tooltip.create(Component.literal("/" + template)))
            .build());
    }

    private void selectPlayer(String name) {
        selectedPlayer = name;
        targetBox.setValue(name);
    }

    private void scrollPlayers(int delta) {
        int max = Math.max(0, menu.onlinePlayers().size() - PLAYER_ROWS);
        playerOffset = Math.max(0, Math.min(max, playerOffset + delta));
        rebuildWidgets();
    }

    private String applyTemplate(String template) {
        return template
            .replace("{player}", targetBox.getValue().trim())
            .replace("{value}", valueBox.getValue().trim())
            .replace("{job}", jobBox.getValue().trim())
            .replace("{class}", classBox.getValue().trim());
    }

    private void run(String command) {
        String normalized = command == null ? "" : command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        rawBox.setValue(normalized);
        ModNetwork.CHANNEL.sendToServer(new AdminCommandExecutePacket(normalized));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xF0111514);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xF019201D);
        panel(graphics, x + 16, y + 38, 392, 38);
        panel(graphics, x + 16, y + 76, 154, 174);
        panel(graphics, x + 172, y + 76, 236, 174);
        panel(graphics, x + 16, y + 248, 392, 32);
    }

    private void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF0D1210);
        graphics.fill(x, y, x + w, y + 1, 0xFF25433A);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF25433A);
        graphics.fill(x, y, x + 1, y + h, 0xFF25433A);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF25433A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFE8E1C4);
        graphics.drawString(font, "대상 선택", 24, 26, 0xFF98D7C5, false);
        graphics.drawString(font, "명령 작업", 176, 26, 0xFF98D7C5, false);
        graphics.drawString(font, "값/레벨", 146, 38, 0xFF98A49C, false);
        graphics.drawString(font, "직업", 224, 38, 0xFF98A49C, false);
        graphics.drawString(font, "계급", 306, 38, 0xFF98A49C, false);
        graphics.drawString(font, "온라인: " + menu.onlinePlayers().size(), 24, 234, 0xFFFFD56A, false);
    }
}
