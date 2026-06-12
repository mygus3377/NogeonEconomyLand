package com.nogeon.economyland.client;

import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.CompressionActionPacket;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;

public class MechanicalCompressionScreen extends Screen {
    private final int screenWidth = 260;
    private final int screenHeight = 280;
    private int leftPos;
    private int topPos;

    private final int remainingCooldownTicks;
    private int selectedMaterial = 0; // 0: Cobble, 1: Deepslate, 2: Limestone, 3: Raw, 4: Ingot
    private final int[] stockCounts = new int[5];
    private boolean skipAnimation = false;

    // Redesigned Multi-compression controls
    private EditBox quantityBox;
    private boolean isUpdatingQuantity = false;
    private int compressCount = 1;

    // Animation & Gacha State
    private boolean isRolling = false;
    private int rollTicks = 0;
    private int maxRollTicks = 40;
    private ItemStack rolledGem = ItemStack.EMPTY;
    private double rolledPercent = 0.0D;
    private int expGained = 0;
    private int creditsGained = 0;
    
    private int screenShakeTicks = 0;
    private float shakeIntensity = 0.0F;

    private final Random rand = new Random();

    public MechanicalCompressionScreen(int remainingCooldownTicks) {
        super(Component.literal("기계식 압축기"));
        this.remainingCooldownTicks = remainingCooldownTicks;
    }

    private void adjustCompressCount(int amount) {
        int maxPossible = stockCounts[selectedMaterial] / getRequiredCount(selectedMaterial);
        if (maxPossible < 1) maxPossible = 1;
        int next = this.compressCount + amount;
        this.compressCount = Math.max(1, Math.min(next, maxPossible));
        syncQuantityBox();
    }

    private void syncQuantityBox() {
        if (this.quantityBox != null) {
            this.isUpdatingQuantity = true;
            this.quantityBox.setValue(String.valueOf(this.compressCount));
            this.isUpdatingQuantity = false;
        }
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.screenWidth) / 2;
        this.topPos = (this.height - this.screenHeight) / 2;

        this.clearWidgets();
        updateStockCounts();

        int maxPossible = stockCounts[selectedMaterial] / getRequiredCount(selectedMaterial);
        if (maxPossible < 1) maxPossible = 1;
        if (this.compressCount > maxPossible) {
            this.compressCount = maxPossible;
        }

        boolean canCompress = stockCounts[selectedMaterial] >= getRequiredCount(selectedMaterial) && !isRolling;

        if (!isRolling) {
            // Quantity buttons
            this.addRenderableWidget(Button.builder(
                Component.literal("-10"),
                btn -> adjustCompressCount(-10)
            ).bounds(this.leftPos + 10, this.topPos + 200, 24, 16).build());

            this.addRenderableWidget(Button.builder(
                Component.literal("-1"),
                btn -> adjustCompressCount(-1)
            ).bounds(this.leftPos + 38, this.topPos + 200, 20, 16).build());

            // quantity box input
            this.quantityBox = new EditBox(this.font, this.leftPos + 90, this.topPos + 200, 45, 16, Component.literal("수량"));
            this.quantityBox.setValue(String.valueOf(this.compressCount));
            this.quantityBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
            this.quantityBox.setResponder(s -> {
                if (isUpdatingQuantity) return;
                if (s.isEmpty()) {
                    this.compressCount = 1;
                    return;
                }
                try {
                    int val = Integer.parseInt(s);
                    int maxP = stockCounts[selectedMaterial] / getRequiredCount(selectedMaterial);
                    if (maxP < 1) maxP = 1;
                    this.compressCount = Math.max(1, Math.min(val, maxP));
                } catch (NumberFormatException ignored) {}
            });
            this.addRenderableWidget(this.quantityBox);

            this.addRenderableWidget(Button.builder(
                Component.literal("+1"),
                btn -> adjustCompressCount(1)
            ).bounds(this.leftPos + 140, this.topPos + 200, 20, 16).build());

            this.addRenderableWidget(Button.builder(
                Component.literal("+10"),
                btn -> adjustCompressCount(10)
            ).bounds(this.leftPos + 164, this.topPos + 200, 24, 16).build());

            this.addRenderableWidget(Button.builder(
                Component.literal("[최대]"),
                btn -> {
                    int maxP = stockCounts[selectedMaterial] / getRequiredCount(selectedMaterial);
                    this.compressCount = Math.max(1, maxP);
                    syncQuantityBox();
                }
            ).bounds(this.leftPos + 192, this.topPos + 200, 58, 16).build());

            // Action Buttons
            Button compressBtn = Button.builder(
                Component.literal(canCompress ? "§e[ 압축 가동 (" + compressCount + "회) ]" : "§7[재질/재료 부족]"),
                btn -> {
                    if (canCompress) {
                        ModNetwork.CHANNEL.sendToServer(new CompressionActionPacket(selectedMaterial + 1, compressCount));
                    }
                }
            ).bounds(this.leftPos + 10, this.topPos + 228, 240, 20).build();
            compressBtn.active = canCompress;
            this.addRenderableWidget(compressBtn);

            this.addRenderableWidget(Button.builder(
                Component.literal(skipAnimation ? "§a[연출 스킵 ON]" : "§7[연출 스킵 OFF]"),
                btn -> {
                    skipAnimation = !skipAnimation;
                    this.init();
                }
            ).bounds(this.leftPos + 10, this.topPos + 253, 115, 18).build());

            this.addRenderableWidget(Button.builder(
                Component.literal("닫기"),
                btn -> this.onClose()
            ).bounds(this.leftPos + 135, this.topPos + 253, 115, 18).build());
        } else {
            this.addRenderableWidget(Button.builder(
                Component.literal("§6[ 연출 건너뛰기 ]"),
                btn -> {
                    this.rollTicks = this.maxRollTicks;
                    finishRoll();
                }
            ).bounds(this.leftPos + 10, this.topPos + 228, 240, 20).build());
        }
    }

    private void updateStockCounts() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        for (int i = 0; i < 5; i++) {
            stockCounts[i] = 0;
        }

        // Cobblestone
        stockCounts[0] = ExtendedInventoryDelivery.countAllOwned(mc.player, new ItemStack(Items.COBBLESTONE));
        
        // Deepslate
        stockCounts[1] = ExtendedInventoryDelivery.countAllOwned(mc.player, new ItemStack(Items.DEEPSLATE));
        
        // Limestone
        net.minecraft.world.item.Item limestone = BuiltInRegistries.ITEM.get(new ResourceLocation("create:limestone"));
        if (limestone != Items.AIR) {
            stockCounts[2] = ExtendedInventoryDelivery.countAllOwned(mc.player, new ItemStack(limestone));
        }

        // Raw Copper & Zinc
        stockCounts[3] = ExtendedInventoryDelivery.countAllOwned(mc.player, new ItemStack(Items.RAW_COPPER));
        net.minecraft.world.item.Item rawZinc = BuiltInRegistries.ITEM.get(new ResourceLocation("create:raw_zinc"));
        if (rawZinc != Items.AIR) {
            stockCounts[3] += ExtendedInventoryDelivery.countAllOwned(mc.player, new ItemStack(rawZinc));
        }

        // Copper Ingot & Zinc Ingot
        stockCounts[4] = ExtendedInventoryDelivery.countAllOwned(mc.player, new ItemStack(Items.COPPER_INGOT));
        net.minecraft.world.item.Item zincIngot = BuiltInRegistries.ITEM.get(new ResourceLocation("create:zinc_ingot"));
        if (zincIngot != Items.AIR) {
            stockCounts[4] += ExtendedInventoryDelivery.countAllOwned(mc.player, new ItemStack(zincIngot));
        }
    }

    private int getRequiredCount(int material) {
        return switch (material) {
            case 0, 1 -> 500;
            case 2 -> 300;
            case 3, 4 -> 100;
            default -> 999;
        };
    }

    private ItemStack getRowItem(int material) {
        return switch (material) {
            case 0 -> new ItemStack(Items.COBBLESTONE);
            case 1 -> new ItemStack(Items.DEEPSLATE);
            case 2 -> {
                net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("create:limestone"));
                yield item != Items.AIR ? new ItemStack(item) : new ItemStack(Items.STONE);
            }
            case 3 -> new ItemStack(Items.RAW_COPPER);
            case 4 -> new ItemStack(Items.COPPER_INGOT);
            default -> new ItemStack(Items.AIR);
        };
    }

    private String getMaterialName(int material) {
        return switch (material) {
            case 0 -> "조약돌 (500개 필요)";
            case 1 -> "심층암 (500개 필요)";
            case 2 -> "석회암 (300개 필요)";
            case 3 -> "원시 구리/아연 (100개 필요)";
            case 4 -> "구리/아연 주괴 (100개 필요)";
            default -> "";
        };
    }

    public void startRollAnimation(ItemStack resultGem, double percent, int exp, int credits) {
        this.rolledGem = resultGem;
        this.rolledPercent = percent;
        this.expGained = exp;
        this.creditsGained = credits;

        if (skipAnimation) {
            finishRoll();
            return;
        }

        this.isRolling = true;
        this.rollTicks = 0;

        // Configure animations based on roll grades
        if (percent < 150.0D) { // Legendary (Flawless or Perfect)
            this.maxRollTicks = 60; // 3 seconds
            this.screenShakeTicks = 60;
            this.shakeIntensity = 2.8F;
            Minecraft.getInstance().player.playSound(
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F
            );
        } else if (percent < 1500.0D) { // Epic (Flawed or Normal)
            this.maxRollTicks = 40; // 2 seconds
            this.screenShakeTicks = 40;
            this.shakeIntensity = 1.4F;
            Minecraft.getInstance().player.playSound(
                SoundEvents.AMETHYST_CLUSTER_PLACE, 1.0F, 1.2F
            );
        } else { // Common (Cracked or Split)
            this.maxRollTicks = 20; // 1 second
            this.screenShakeTicks = 5;
            this.shakeIntensity = 0.5F;
            Minecraft.getInstance().player.playSound(
                SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.5F
            );
        }

        this.init();
    }

    private void finishRoll() {
        this.isRolling = false;
        this.screenShakeTicks = 0;
        
        // Spawn particles based on rarity
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            double px = mc.player.getX();
            double py = mc.player.getY() + 1.0D;
            double pz = mc.player.getZ();
            
            if (rolledPercent < 150.0D) {
                // Legendary Golden explosion
                mc.level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, px, py, pz, 0, 0.1D, 0);
                mc.level.addParticle(ParticleTypes.END_ROD, px, py, pz, 0, 0.05D, 0);
                mc.player.level().playSound(mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 0.8F);
            } else if (rolledPercent < 1500.0D) {
                // Epic purple aura
                mc.level.addParticle(ParticleTypes.ENCHANT, px, py, pz, 0, 0.05D, 0);
            } else {
                // Common green spark
                mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 0, 0.02D, 0);
            }
        }

        this.init();
    }

    @Override
    public void tick() {
        super.tick();
        updateStockCounts();

        if (isRolling) {
            this.rollTicks++;
            if (this.screenShakeTicks > 0) {
                this.screenShakeTicks--;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null && this.rollTicks % 3 == 0) {
                // Aesthetic steam sparks during roll
                double px = mc.player.getX() + (rand.nextDouble() - 0.5D) * 0.4D;
                double py = mc.player.getY() + 0.8D + (rand.nextDouble() - 0.5D) * 0.4D;
                double pz = mc.player.getZ() + (rand.nextDouble() - 0.5D) * 0.4D;
                if (rolledPercent < 150.0D) {
                    mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, 0.02D, 0);
                } else {
                    mc.level.addParticle(ParticleTypes.CLOUD, px, py, pz, 0, 0.01D, 0);
                }
            }

            if (this.rollTicks >= this.maxRollTicks) {
                finishRoll();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        // Apply screen shake
        graphics.pose().pushPose();
        if (isRolling && screenShakeTicks > 0) {
            float dx = (rand.nextFloat() - 0.5F) * shakeIntensity;
            float dy = (rand.nextFloat() - 0.5F) * shakeIntensity;
            graphics.pose().translate(dx, dy, 0.0F);
        }

        // Draw outer frame
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.screenWidth, this.topPos + this.screenHeight, 0xD0121110);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.screenWidth, this.topPos + 1, 0xFFDFB24E);
        graphics.fill(this.leftPos, this.topPos + this.screenHeight - 1, this.leftPos + this.screenWidth, this.topPos + this.screenHeight, 0xFFDFB24E);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.screenHeight, 0xFFDFB24E);
        graphics.fill(this.leftPos + this.screenWidth - 1, this.topPos, this.leftPos + this.screenWidth, this.topPos + this.screenHeight, 0xFFDFB24E);

        // Render flashing red border hazard lights for Legendary draws
        if (isRolling && rolledPercent < 150.0D) {
            long gameTime = Minecraft.getInstance().level.getGameTime();
            if ((gameTime / 4) % 2 == 0) {
                int flashColor = 0x88FF1111;
                graphics.fill(leftPos - 3, topPos - 3, leftPos + screenWidth + 3, topPos, flashColor);
                graphics.fill(leftPos - 3, topPos + screenHeight, leftPos + screenWidth + 3, topPos + screenHeight + 3, flashColor);
                graphics.fill(leftPos - 3, topPos, leftPos, topPos + screenHeight, flashColor);
                graphics.fill(leftPos + screenWidth, topPos, leftPos + screenWidth + 3, topPos + screenHeight, flashColor);
            }
        }

        // Title
        graphics.drawString(this.font, "⚙§l 기계식 압축기 (공학 전용)", this.leftPos + 12, this.topPos + 12, 0xFFFFF6D3, true);

        // Ultimate Cooldown Status
        if (remainingCooldownTicks > 0) {
            graphics.drawString(this.font, "⚡ " + (remainingCooldownTicks / 20) + "s", this.leftPos + 210, this.topPos + 12, 0xFFFFAA00, true);
        } else {
            graphics.drawString(this.font, "⚡ Ready", this.leftPos + 205, this.topPos + 12, 0xFF55FF55, true);
        }

        graphics.fill(this.leftPos + 10, this.topPos + 24, this.leftPos + this.screenWidth - 10, this.topPos + 25, 0x44FFFFFF);

        // Render 5 Material Rows
        for (int i = 0; i < 5; i++) {
            int rowY = this.topPos + 30 + i * 21;
            boolean isSelected = selectedMaterial == i && !isRolling;
            
            // Draw background bar
            int rowColor = isSelected ? 0x66DFB24E : 0x22FFFFFF;
            graphics.fill(this.leftPos + 10, rowY, this.leftPos + this.screenWidth - 10, rowY + 19, rowColor);

            if (isSelected) {
                // Highlight outlines
                graphics.fill(this.leftPos + 10, rowY, this.leftPos + this.screenWidth - 10, rowY + 1, 0xFFDFB24E);
                graphics.fill(this.leftPos + 10, rowY + 18, this.leftPos + this.screenWidth - 10, rowY + 19, 0xFFDFB24E);
                graphics.fill(this.leftPos + 10, rowY, this.leftPos + 11, rowY + 19, 0xFFDFB24E);
                graphics.fill(this.leftPos + this.screenWidth - 11, rowY, this.leftPos + this.screenWidth - 10, rowY + 19, 0xFFDFB24E);
            }

            // Draw Item Stack icon
            ItemStack stack = getRowItem(i);
            graphics.renderItem(stack, this.leftPos + 15, rowY + 1);

            // Draw Material details
            int required = getRequiredCount(i);
            boolean hasEnough = stockCounts[i] >= required;
            int nameColor = hasEnough ? 0xFFFFFFFF : 0xFFAAAAAA;

            graphics.drawString(this.font, getMaterialName(i), this.leftPos + 38, rowY + 5, nameColor, false);

            // Draw count status
            int countColor = hasEnough ? 0xFF55FF55 : 0xFFFF5555;
            String countText = stockCounts[i] + "개 보유";
            graphics.drawString(this.font, countText, this.leftPos + this.screenWidth - this.font.width(countText) - 15, rowY + 5, countColor, false);
        }

        // Draw separator
        graphics.fill(this.leftPos + 10, this.topPos + 138, this.leftPos + this.screenWidth - 10, this.topPos + 139, 0x44FFFFFF);

        // Lower Animation / Result Box
        graphics.fill(this.leftPos + 10, this.topPos + 142, this.leftPos + this.screenWidth - 10, this.topPos + 192, 0x33000000);

        if (isRolling) {
            // Draw mechanical squeeze compression animation
            double t = (double) rollTicks / maxRollTicks;
            double pistonY = 0;
            if (t < 0.25) {
                pistonY = (t / 0.25) * 16;
            } else if (t < 0.55) {
                pistonY = 16;
            } else {
                pistonY = 16 - ((t - 0.55) / 0.45) * 16;
            }

            // Render gear teeth
            long gameTime = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
            int angle = (int) ((gameTime * 6) % 360);
            
            // Render Piston top
            graphics.fill(this.leftPos + 110, this.topPos + 146, this.leftPos + 150, this.topPos + 152, 0xFF888888);
            // Render descending shaft
            graphics.fill(this.leftPos + 126, this.topPos + 152, this.leftPos + 134, this.topPos + 152 + (int)pistonY, 0xFFBBBBBB);
            // Render heavy weight
            graphics.fill(this.leftPos + 118, this.topPos + 152 + (int)pistonY, this.leftPos + 142, this.topPos + 162 + (int)pistonY, 0xFFDFB24E);

            // Draw spark indicator immediately below the heavy weight (Y: 162 + pistonY)
            if (t >= 0.25 && t <= 0.6) {
                int baseColor = (rolledPercent < 150.0D) ? 0xFFFFD700 : ((rolledPercent < 1500.0D) ? 0xFFD300FF : 0xFFFFFFFF);
                int centerX = this.leftPos + 130;
                int centerY = this.topPos + 162 + (int) pistonY;
                
                // Let the spark burst fluctuate in size and alpha over time
                int pulse = (int) (Math.sin((rollTicks - 0.25 * maxRollTicks) * 0.8) * 3) + 4; // 1 to 7 pixels
                
                // Horizontal bar
                graphics.fill(centerX - pulse, centerY - 1, centerX + pulse + 1, centerY + 2, baseColor);
                // Vertical bar
                graphics.fill(centerX - 1, centerY - pulse, centerX + 2, centerY + pulse + 1, baseColor);
                
                // Outer sparks (diagonal pixels)
                if (pulse > 3) {
                    graphics.fill(centerX - pulse + 2, centerY - pulse + 2, centerX - pulse + 4, centerY - pulse + 4, baseColor);
                    graphics.fill(centerX + pulse - 3, centerY - pulse + 2, centerX + pulse - 1, centerY - pulse + 4, baseColor);
                    graphics.fill(centerX - pulse + 2, centerY + pulse - 3, centerX - pulse + 4, centerY + pulse - 1, baseColor);
                    graphics.fill(centerX + pulse - 3, centerY + pulse - 3, centerX + pulse - 1, centerY + pulse - 1, baseColor);
                }
            }
        } else if (!rolledGem.isEmpty()) {
            // Roll Finished: Show Reward Card!
            graphics.renderItem(rolledGem, this.leftPos + 122, this.topPos + 146);
            
            String gemName = rolledGem.getHoverName().getString();
            int nameWidth = this.font.width(gemName);
            graphics.drawString(this.font, gemName, this.leftPos + (this.screenWidth - nameWidth) / 2, this.topPos + 165, 0xFFFFFFFF, true);
            
            String rewardText = "§aEXP +" + expGained + "   §eCredits +" + creditsGained + " C";
            int rewardWidth = this.font.width(rewardText);
            graphics.drawString(this.font, rewardText, this.leftPos + (this.screenWidth - rewardWidth) / 2, this.topPos + 176, 0xFFFFFFFF, false);
        } else {
            // Idle State before rolling
            graphics.drawCenteredString(this.font, "§7위의 재료를 선택하고 압축해 보석을 획득하세요.", this.leftPos + this.screenWidth / 2, this.topPos + 170, 0xFFFFFFFF);
        }

        // Draw separator between animation box and action buttons
        graphics.fill(this.leftPos + 10, this.topPos + 195, this.leftPos + this.screenWidth - 10, this.topPos + 196, 0x44FFFFFF);

        if (!isRolling) {
            graphics.drawString(this.font, "수량", this.leftPos + 64, this.topPos + 204, 0xFFDFB24E, false);
            graphics.fill(this.leftPos + 10, this.topPos + 222, this.leftPos + this.screenWidth - 10, this.topPos + 223, 0x44FFFFFF);
        }

        graphics.pose().popPose();
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isRolling) {
            for (int i = 0; i < 5; i++) {
                int rowY = this.topPos + 30 + i * 21;
                if (mouseX >= this.leftPos + 10 && mouseX <= this.leftPos + this.screenWidth - 10 && mouseY >= rowY && mouseY <= rowY + 19) {
                    this.selectedMaterial = i;
                    int maxP = stockCounts[selectedMaterial] / getRequiredCount(selectedMaterial);
                    if (maxP < 1) maxP = 1;
                    this.compressCount = Math.min(this.compressCount, maxP);
                    Minecraft.getInstance().player.playSound(
                        SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, 1.0F
                    );
                    this.init();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
