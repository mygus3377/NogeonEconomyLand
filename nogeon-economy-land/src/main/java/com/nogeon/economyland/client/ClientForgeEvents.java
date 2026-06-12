package com.nogeon.economyland.client;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.item.LandDeedItem;
import com.nogeon.economyland.network.LandSelectionClickPacket;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenCosmeticArmorPacket;
import com.nogeon.economyland.network.OpenWalletPacket;
import com.nogeon.economyland.network.OpenExtendedInventoryPacket;
import com.nogeon.economyland.network.ToggleItemLockPacket;
import com.nogeon.economyland.network.JobAbilityKeyPacket;
import net.minecraft.world.level.block.state.BlockState;
import com.nogeon.economyland.shop.ShopItemProtection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.effect.MobEffects;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = NoGeonEconomyLand.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final net.minecraft.resources.ResourceLocation WEAKPOINT_TEX = new net.minecraft.resources.ResourceLocation("nogeon_economy_land", "textures/entity/weakpoint.png");
    private static final String ENHANCE_LEVEL_TAG = "NoGeonEnhanceLevel";
    private static final net.minecraft.resources.ResourceLocation ENHANCE_GLINT_TEX = new net.minecraft.resources.ResourceLocation("nogeon_economy_land", "textures/gui/enhance_glint.png");
    private static final net.minecraft.resources.ResourceLocation ENERGY_AURA_TEX = new net.minecraft.resources.ResourceLocation("nogeon_economy_land", "textures/entity/energy_aura.png");
    private static Button walletInventoryButton;
    private static Button extendedInventoryButton;
    private static Button cosmeticArmorButton;
    private static final List<BlockPos> minerEyeOreCache = new ArrayList<>();
    private static long minerEyeLastScanTick = -1L;
    private static BlockPos minerEyeLastScanCenter = BlockPos.ZERO;
    private static final ThreadLocal<ItemStack> RENDERING_ENHANCE_STACK = ThreadLocal.withInitial(() -> ItemStack.EMPTY);
    private static final java.util.Map<String, RenderType> ENHANCE_GLINT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Deque<LivingEntity> RENDERING_STACK = new java.util.ArrayDeque<>();
    private static boolean isRenderingTooltip = false;
    private static java.lang.reflect.Method renderTooltipInternalMethod = null;

    public static void pushRenderingEntity(LivingEntity entity) {
        RENDERING_STACK.push(entity);
    }

    public static void popRenderingEntity() {
        if (!RENDERING_STACK.isEmpty()) {
            RENDERING_STACK.pop();
        }
    }

    public static LivingEntity getCurrentRenderingEntity() {
        return RENDERING_STACK.peek();
    }

    private ClientForgeEvents() {
    }

    public static void setRenderingEnhanceStack(ItemStack stack) {
        RENDERING_ENHANCE_STACK.set(stack);
    }

    public static void clearRenderingEnhanceStack() {
        RENDERING_ENHANCE_STACK.set(ItemStack.EMPTY);
    }

    public static boolean isRenderingEnhanced() {
        ItemStack stack = RENDERING_ENHANCE_STACK.get();
        if (stack != null && !stack.isEmpty() && stack.hasTag()) {
            return stack.getTag().contains(ENHANCE_LEVEL_TAG) && stack.getTag().getInt(ENHANCE_LEVEL_TAG) > 0;
        }
        return false;
    }

    public static int getRenderingEnhanceLevel() {
        ItemStack stack = RENDERING_ENHANCE_STACK.get();
        if (stack != null && !stack.isEmpty()) {
            return enhanceLevel(stack);
        }
        return 0;
    }

    public static RenderType getEnhanceGlintRenderType(RenderType baseGlint, int level) {
        if (level <= 0 || baseGlint == null) return baseGlint;
        String baseName = ((com.nogeon.economyland.mixin.RenderTypeAccessor) baseGlint).getName();
        if (baseName.startsWith("enhance_")) {
            return baseGlint;
        }
        String key = baseName + "_" + level;
        return ENHANCE_GLINT_CACHE.computeIfAbsent(key, k -> createEnhanceGlint(baseGlint, baseName, level));
    }

    private static RenderType createEnhanceGlint(RenderType baseGlint, String baseName, int level) {
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (level >= 20) {
            double phase = (System.currentTimeMillis() % 2000L) / 2000.0D * Math.PI * 2.0D;
            r = (float) (Math.sin(phase) * 0.4F + 0.6F);
            g = (float) (Math.sin(phase + Math.PI * 2.0D / 3.0D) * 0.4F + 0.6F);
            b = (float) (Math.sin(phase + Math.PI * 4.0D / 3.0D) * 0.4F + 0.6F);
        } else if (level == 19) {
            r = 1.0F; g = 0.85F; b = 0.0F; // 황금
        } else if (level == 18) {
            r = 1.0F; g = 0.05F; b = 0.05F; // 진홍
        } else if (level == 17) {
            r = 0.78F; g = 0.0F; b = 1.0F; // 보라
        } else if (level == 16) {
            r = 0.0F; g = 1.0F; b = 0.80F; // 청록
        } else if (level >= 10) {
            r = 1.0F; g = 0.70F; b = 0.16F; // 노란색
        } else if (level >= 6) {
            r = 0.60F; g = 0.36F; b = 1.0F; // 연보라
        } else {
            r = 0.30F; g = 0.70F; b = 1.0F; // 푸른빛
        }

        net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard transparency = new com.nogeon.economyland.client.EnhanceGlintTransparency(
            "enhance_glint_transparency_" + level + "_" + System.nanoTime(), r, g, b
        );

        boolean isDirect = baseName.contains("direct") || baseName.contains("Direct");
        boolean isEntity = baseName.contains("entity") || baseName.contains("Entity");
        boolean isArmor = baseName.contains("armor") || baseName.contains("Armor");

        net.minecraft.client.renderer.RenderStateShard.ShaderStateShard shader;
        if (isArmor) {
            shader = com.nogeon.economyland.mixin.RenderStateShardAccessor.getArmorEntityGlintShader();
        } else if (isEntity) {
            shader = isDirect ? com.nogeon.economyland.mixin.RenderStateShardAccessor.getEntityGlintDirectShader() : com.nogeon.economyland.mixin.RenderStateShardAccessor.getEntityGlintShader();
        } else {
            shader = isDirect ? com.nogeon.economyland.mixin.RenderStateShardAccessor.getGlintDirectShader() : com.nogeon.economyland.mixin.RenderStateShardAccessor.getGlintShader();
        }

        net.minecraft.client.renderer.RenderStateShard.TextureStateShard texture = new net.minecraft.client.renderer.RenderStateShard.TextureStateShard(
            isArmor ? net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS : new net.minecraft.resources.ResourceLocation("textures/misc/enchanted_glint_item.png"),
            true,
            false
        );

        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(shader)
            .setTextureState(texture)
            .setWriteMaskState(com.nogeon.economyland.mixin.RenderStateShardAccessor.getColorWrite())
            .setCullState(com.nogeon.economyland.mixin.RenderStateShardAccessor.getNoCull())
            .setDepthTestState(com.nogeon.economyland.mixin.RenderStateShardAccessor.getEqual())
            .setTransparencyState(transparency)
            .setTexturingState(com.nogeon.economyland.mixin.RenderStateShardAccessor.getGlintTexturing())
            .createCompositeState(false);

        return com.nogeon.economyland.mixin.RenderTypeAccessor.callCreate(
            "enhance_" + baseName + "_" + level,
            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            state
        );
    }

    public static void setWeaponVfxEnabled(boolean enabled) {
        ClientConfig.weaponVfx = enabled;
        ClientConfig.save();
    }

    public static boolean isWeaponVfxEnabled() {
        return ClientConfig.weaponVfx;
    }

    public static void setArmorVfxEnabled(boolean enabled) {
        ClientConfig.armorVfx = enabled;
        ClientConfig.save();
    }

    public static boolean isArmorVfxEnabled() {
        return ClientConfig.armorVfx;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            ClientLandSelectionData.clear();
            return;
        }
        if (minecraft.screen != null) {
            return;
        }

        while (ClientModEvents.OPEN_ECONOMY_KEY.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new OpenWalletPacket());
        }

        // 광부: 단축키 토글 감지 (V: 우월한 신체, B: 개안)
        while (ClientModEvents.JOB_ABILITY_PRIMARY_KEY.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new JobAbilityKeyPacket(1));
        }
        while (ClientModEvents.JOB_ABILITY_SECONDARY_KEY.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new JobAbilityKeyPacket(2));
        }

        if (ClientMinerData.minerEyeActive() && ClientMinerData.minerEyeRadius() > 0) {
            // Update thread-safe volatile coordinates on main thread every single tick
            ClientMinerData.playerX = minecraft.player.getX();
            ClientMinerData.playerY = minecraft.player.getY();
            ClientMinerData.playerZ = minecraft.player.getZ();
            refreshMinerEyeOreCache(minecraft);
            if (minecraft.player.tickCount % 4 == 0) {
                spawnMinerEyeParticles(minecraft);
            }
        }

        // spawnHeldWeaponBladeVfx(minecraft); // 렌더러 단에서 직접 고정 스폰하므로 기존 틱 스폰 비활성화
        spawnArmorFlowVfx(minecraft);
        spawnDroppedItemVfx(minecraft);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }

        walletInventoryButton = Button.builder(
            Component.translatable("button.nogeon_economy_land.open_wallet"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenWalletPacket())
        ).bounds(0, 0, 28, 16).build();

        extendedInventoryButton = Button.builder(
            Component.translatable("button.nogeon_economy_land.open_extended_inventory"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenExtendedInventoryPacket())
        ).bounds(0, 0, 28, 16).build();

        cosmeticArmorButton = Button.builder(
            Component.literal("치장"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenCosmeticArmorPacket())
        ).bounds(0, 0, 28, 16).build();
        positionInventoryButtons(screen);
        event.addListener(walletInventoryButton);
        event.addListener(extendedInventoryButton);
        event.addListener(cosmeticArmorButton);
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof InventoryScreen inventoryScreen) {
            positionInventoryButtons(inventoryScreen);
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || minecraft.player == null) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            int x = screen.getGuiLeft() + slot.x;
            int y = screen.getGuiTop() + slot.y;
            if (ShopItemProtection.isLocked(stack)) {
                event.getGuiGraphics().drawString(minecraft.font, Component.literal("*"), x + 1, y + 1, 0xFFFFD56A, true);
            }
            int level = enhanceLevel(stack);
            if (level > 0) {
                boolean isArmor = isArmor(stack);
                boolean isWeapon = !isArmor;
                if ((!isArmor || isArmorVfxEnabled()) && (!isWeapon || isWeaponVfxEnabled())) {
                    if (ClientConfig.itemVfx) {
                        renderEnhanceIconEffect(event.getGuiGraphics(), minecraft, x, y, level);
                    }
                }
                renderEnhanceBadge(event.getGuiGraphics(), minecraft, x, y, level);
            }
        }
    }

    private static void positionInventoryButtons(InventoryScreen screen) {
        if (walletInventoryButton == null || extendedInventoryButton == null || cosmeticArmorButton == null) {
            return;
        }
        Minecraft minecraft = screen.getMinecraft();
        boolean hasEffects = minecraft.player != null && !minecraft.player.getActiveEffects().isEmpty();

        int buttonWidth = walletInventoryButton.getWidth();
        int rightX = screen.getGuiLeft() + screen.getXSize() + 2;
        int leftX = screen.getGuiLeft() - buttonWidth - 2;
        
        // 포션 효과가 있으면 왼쪽 안전 피신, 없으면 화면 공간 판정에 따라 배치
        int x = hasEffects ? leftX : (rightX + buttonWidth <= screen.width ? rightX : leftX);
        int y = screen.getGuiTop() + 6;
        extendedInventoryButton.setPosition(x, y);
        walletInventoryButton.setPosition(x, y + 18);
        cosmeticArmorButton.setPosition(x, y + 36);
    }

    @SubscribeEvent
    public static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof InventoryScreen screen) || minecraft.player == null || event.getButton() != 0 || !Screen.hasAltDown()) {
            return;
        }
        Slot slot = slotAt(screen, event.getMouseX(), event.getMouseY());
        if (slot == null || !slot.hasItem()) {
            return;
        }
        int inventorySlot = slot.getSlotIndex();
        if (inventorySlot < 0 || inventorySlot >= minecraft.player.getInventory().getContainerSize()) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new ToggleItemLockPacket(inventorySlot));
        event.setCanceled(true);
    }

    private static Slot slotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        for (Slot slot : screen.getMenu().slots) {
            int x = screen.getGuiLeft() + slot.x;
            int y = screen.getGuiTop() + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || !holdsLandDeed(minecraft)) {
            return;
        }
        if (!event.isAttack() && !event.isUseItem()) {
            return;
        }

        BlockPos targetPos = null;
        if (minecraft.hitResult instanceof BlockHitResult blockHit && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            targetPos = blockHit.getBlockPos();
        }

        ModNetwork.CHANNEL.sendToServer(new LandSelectionClickPacket(event.isUseItem(), targetPos));
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        String overlayId = event.getOverlay().id().toString();
        if (overlayId.contains("feathers") || overlayId.contains("flight") || overlayId.contains("elytra")) {
            ItemStack chest = minecraft.player.getItemBySlot(EquipmentSlot.CHEST);
            boolean hasElytra = chest.getItem() instanceof net.minecraft.world.item.ElytraItem || chest.getOrCreateTag().getBoolean("ElytraEffect");
            if (hasElytra || minecraft.player.isFallFlying()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        ClientVideoOverlay.render(graphics, width, height);

        Component text = Component.literal(CREDIT_FORMAT.format(ClientWalletData.credits()) + " C");
        int textWidth = minecraft.font.width(text);
        int bgWidth = textWidth + 8;

        // Position it to the left of the hotbar (which is centered at bottom)
        // Moved further left to avoid overlapping inventory
        int x = width / 2 - 185;
        int y = height - 20;

        graphics.fill(x, y - 2, x + bgWidth, y + 10, 0x88000000);
        graphics.drawString(minecraft.font, text, x + 4, y, 0xFFFFD56A, false);
        renderHotbarEnhanceBadges(graphics, minecraft, width, height);
        renderFisherFlowGauge(graphics, minecraft, width, height);
        renderLandSelectionHud(graphics, minecraft);
    }

    private static void renderFisherFlowGauge(GuiGraphics graphics, Minecraft minecraft, int width, int height) {
        int gauge = ClientFisherData.flowGauge();
        if (gauge <= 0) {
            return;
        }

        int barWidth = 80;
        int barHeight = 4;
        int x = width / 2 - barWidth / 2;
        int y = height - 48; // 에어 바 위쪽 정도 위치

        // 배경
        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xAA000000);
        // 채워진 부분 (푸른색 어장 느낌)
        int filled = (int) (barWidth * (gauge / 100.0D));
        int color = gauge >= 100 ? 0xFF00FFFF : 0xFF00AAFF;
        graphics.fill(x, y, x + filled, y + barHeight, color);

        if (gauge >= 100) {
            Component text = Component.literal("\ubbf8\ub07c \ubfcc\ub9ac\uae30 \uc900\ube44\uc644\ub8cc (")
                .append(ClientModEvents.JOB_ABILITY_PRIMARY_KEY.getTranslatedKeyMessage())
                .append(")");
            int tw = minecraft.font.width(text);
            graphics.drawString(minecraft.font, text, width / 2 - tw / 2, y - 10, 0xFF55FFFF, true);
            return;
        }

        if (gauge >= 100) {
            return;
        } else {
            Component text = Component.literal("흐름 게이지 " + gauge + "%");
            int tw = minecraft.font.width(text);
            graphics.drawString(minecraft.font, text, width / 2 - tw / 2, y - 10, 0xFFAAAAAA, true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        renderMinerEyeXray(event, minecraft);
        renderHunterMark(event, minecraft);

        // 100레벨 사냥꾼 [먹이사슬의 정점] 약점 3D 빌보드 렌더링
        if (minecraft.player != null && minecraft.level != null && ClientHunterData.hunterSenseActive() && ClientHunterData.hunterSenseRadius() > 0) {
            double cameraX = event.getCamera().getPosition().x;
            double cameraY = event.getCamera().getPosition().y;
            double cameraZ = event.getCamera().getPosition().z;

            int radius = ClientHunterData.hunterSenseRadius();
            AABB area = minecraft.player.getBoundingBox().inflate(radius);
            List<LivingEntity> entities = minecraft.level.getEntitiesOfClass(LivingEntity.class, area, e -> e != minecraft.player && e.hasEffect(MobEffects.GLOWING));

            for (LivingEntity victim : entities) {
                long gameTime = minecraft.level.getGameTime();
                long period = gameTime / 60;
                java.util.Random rand = new java.util.Random(victim.getUUID().getMostSignificantBits() ^ victim.getUUID().getLeastSignificantBits() ^ period);
                int weakpointDir = rand.nextInt(6); // 0-3: side, 4: high, 5: low

                double rad = Math.toRadians(victim.getYRot());
                double ox = 0, oz = 0;
                double distance = victim.getBbWidth() * 0.5D + 0.08D; // Pinned sleekly to the skin!
                double height = victim.getBbHeight() * 0.55D;
                switch (weakpointDir) {
                    case 0: // 전방
                        ox = -Math.sin(rad) * distance;
                        oz = Math.cos(rad) * distance;
                        break;
                    case 1: // 후방
                        ox = Math.sin(rad) * distance;
                        oz = -Math.cos(rad) * distance;
                        break;
                    case 2: // 좌측
                        ox = -Math.cos(rad) * distance;
                        oz = -Math.sin(rad) * distance;
                        break;
                    case 3: // 우측
                        ox = Math.cos(rad) * distance;
                        oz = Math.sin(rad) * distance;
                        break;
                    case 4:
                        height = victim.getBbHeight() * 0.88D;
                        break;
                    case 5:
                        height = victim.getBbHeight() * 0.22D;
                        break;
                }

                double wx = victim.getX() + ox;
                double wy = victim.getY() + height;
                double wz = victim.getZ() + oz;

                com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                poseStack.translate(wx - cameraX, wy - cameraY, wz - cameraZ);

                // 카메라 상시 마주보는 빌보드 회전 적용
                poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
                poseStack.scale(0.45F, 0.45F, 0.45F); // Pinned, sharp premium aesthetic

                float pulse = 0.85F + 0.15F * (float) Math.sin((gameTime + victim.getId() * 10) * 0.25D);

                VertexConsumer builder = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(WEAKPOINT_TEX));
                org.joml.Matrix4f mat = poseStack.last().pose();
                float size = 0.55F;

                builder.vertex(mat, -size, -size, 0).color(0.0F, 1.0F, 1.0F, pulse).uv(0.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880).normal(poseStack.last().normal(), 0.0F, 1.0F, 0.0F).endVertex();
                builder.vertex(mat, size, -size, 0).color(0.0F, 1.0F, 1.0F, pulse).uv(1.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880).normal(poseStack.last().normal(), 0.0F, 1.0F, 0.0F).endVertex();
                builder.vertex(mat, size, size, 0).color(0.0F, 1.0F, 1.0F, pulse).uv(1.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880).normal(poseStack.last().normal(), 0.0F, 1.0F, 0.0F).endVertex();
                builder.vertex(mat, -size, size, 0).color(0.0F, 1.0F, 1.0F, pulse).uv(0.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880).normal(poseStack.last().normal(), 0.0F, 1.0F, 0.0F).endVertex();

                poseStack.popPose();
            }
            minecraft.renderBuffers().bufferSource().endBatch(RenderType.entityTranslucent(WEAKPOINT_TEX));
        }

        ClientLandSelectionData.Preview preview = ClientLandSelectionData.preview(minecraft);
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null || minecraft.screen != null || preview == null || !holdsLandDeed(minecraft)) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(-camera.x, -camera.y, -camera.z);

        for (ClientLandSelectionData.Cuboid cuboid : preview.cuboids()) {
            AABB box = new AABB(
                cuboid.min().getX(),
                cuboid.min().getY(),
                cuboid.min().getZ(),
                cuboid.max().getX() + 1.0D,
                cuboid.max().getY() + 1.0D,
                cuboid.max().getZ() + 1.0D
            );
            float r = cuboid.additive() ? 0.96F : 0.96F;
            float g = cuboid.additive() ? 0.81F : 0.20F;
            float b = cuboid.additive() ? 0.33F : 0.20F;
            
            LevelRenderer.addChainedFilledBoxVertices(event.getPoseStack(), bufferSource.getBuffer(RenderType.debugFilledBox()), box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, 0.18F);
            LevelRenderer.renderLineBox(event.getPoseStack(), bufferSource.getBuffer(RenderType.lines()), box, r + 0.02F, g + 0.06F, b + 0.13F, 0.92F);
        }

        // 핫스팟(물고기 떼) 및 어장(미끼 뿌리기) 3D 실린더 영역 표시 렌더링
        BlockPos hotspot = ClientFisherData.hotspotPos();
        if (hotspot != null) {
            double radius = ClientFisherData.hotspotRadius();
            renderGlowingCylinder(event.getPoseStack(), bufferSource, hotspot.getX() + 0.5D, hotspot.getY() + 0.08D, hotspot.getZ() + 0.5D, radius, 0.28D, 0.0F, 0.9F, 1.0F, 0.42F);
            renderGlowingCylinder(event.getPoseStack(), bufferSource, hotspot.getX() + 0.5D, hotspot.getY() + 0.08D, hotspot.getZ() + 0.5D, 0.55D, 2.6D, 0.0F, 0.9F, 1.0F, 0.36F);
        }

        for (java.util.Map.Entry<BlockPos, Double> entry : ClientFisherData.fisheryZones().entrySet()) {
            BlockPos pos = entry.getKey();
            double radius = entry.getValue();
            renderGlowingCylinder(event.getPoseStack(), bufferSource, pos.getX() + 0.5D, pos.getY() + 0.08D, pos.getZ() + 0.5D, radius, 0.32D, 0.15F, 0.65F, 1.0F, 0.48F);
            renderGlowingCylinder(event.getPoseStack(), bufferSource, pos.getX() + 0.5D, pos.getY() + 0.08D, pos.getZ() + 0.5D, 0.65D, 3.2D, 0.15F, 0.65F, 1.0F, 0.40F);
        }

        event.getPoseStack().popPose();
        bufferSource.endBatch();
        bufferSource.endBatch(RenderType.lines());
    }

    private static void renderHunterMark(RenderLevelStageEvent event, Minecraft minecraft) {
        String markedUuid = ClientHunterData.hunterPreyMarkedUUID();
        if (markedUuid.isEmpty() || minecraft.level == null) return;

        Entity markedEntity = null;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity.getStringUUID().equals(markedUuid)) {
                markedEntity = entity;
                break;
            }
        }

        if (markedEntity == null || !markedEntity.isAlive()) return;

        double cameraX = event.getCamera().getPosition().x;
        double cameraY = event.getCamera().getPosition().y;
        double cameraZ = event.getCamera().getPosition().z;

        com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        
        // 몹 머리 위 약간 위쪽
        double tx = markedEntity.getX();
        double ty = markedEntity.getY() + markedEntity.getBbHeight() + 0.6D;
        double tz = markedEntity.getZ();
        
        poseStack.translate(tx - cameraX, ty - cameraY, tz - cameraZ);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.6F, 0.6F, 0.6F);

        float pulse = 0.7F + 0.3F * (float) Math.sin(minecraft.level.getGameTime() * 0.2D);
        
        // 빨간색 강조된 약점 아이콘을 표식 아이콘으로 재활용
        VertexConsumer builder = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(WEAKPOINT_TEX));
        org.joml.Matrix4f mat = poseStack.last().pose();
        float size = 0.3F;

        // 빨간색 강조 (RGB: 1, 0.2, 0.2)
        builder.vertex(mat, -size, -size, 0).color(1.0F, 0.2F, 0.2F, pulse).uv(0.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880).normal(poseStack.last().normal(), 0.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(mat, size, -size, 0).color(1.0F, 0.2F, 0.2F, pulse).uv(1.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880).normal(poseStack.last().normal(), 0.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(mat, size, size, 0).color(1.0F, 0.2F, 0.2F, pulse).uv(1.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880).normal(poseStack.last().normal(), 0.0F, 1.0F, 0.0F).endVertex();
        builder.vertex(mat, -size, size, 0).color(1.0F, 0.2F, 0.2F, pulse).uv(0.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880).normal(poseStack.last().normal(), 0.0F, 1.0F, 0.0F).endVertex();

        poseStack.popPose();
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.entityTranslucent(WEAKPOINT_TEX));
    }

    private static void renderLandSelectionHud(GuiGraphics graphics, Minecraft minecraft) {
        if (minecraft.screen != null || !holdsLandDeed(minecraft)) {
            return;
        }
        ClientLandSelectionData.Preview preview = ClientLandSelectionData.preview(minecraft);
        Component title = preview == null
            ? Component.translatable("gui.nogeon_economy_land.land_mode")
            : Component.translatable("gui.nogeon_economy_land.land_mode").append(" - ").append(Component.translatable(preview.type().translationKey()));
        Component middle = preview == null
            ? Component.translatable("gui.nogeon_economy_land.land_mode_start_hint")
            : Component.translatable("gui.nogeon_economy_land.land_blocks").append(": ").append(CREDIT_FORMAT.format(preview.blocks()))
                .append("   ")
                .append(Component.translatable("gui.nogeon_economy_land.land_price"))
                .append(": ")
                .append(CREDIT_FORMAT.format(preview.price()))
                .append(" C");
        Component bottom = Component.translatable(preview != null && preview.locked()
            ? "gui.nogeon_economy_land.land_mode_confirm_hint"
            : "gui.nogeon_economy_land.land_mode_lock_hint");
        int width = Math.max(minecraft.font.width(title), Math.max(minecraft.font.width(middle), minecraft.font.width(bottom))) + 18;
        int left = (minecraft.getWindow().getGuiScaledWidth() - width) / 2;
        int top = minecraft.getWindow().getGuiScaledHeight() - 58;
        graphics.fill(left, top, left + width, top + 34, 0xB0191712);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 33, 0xD62A241B);
        graphics.drawCenteredString(minecraft.font, title, left + width / 2, top + 5, 0xFFF4E3B0);
        graphics.drawCenteredString(minecraft.font, middle, left + width / 2, top + 15, 0xFFE8E1C4);
        graphics.drawCenteredString(minecraft.font, bottom, left + width / 2, top + 25, 0xFF9FA79A);
    }

    private static boolean holdsLandDeed(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof LandDeedItem;
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof net.minecraft.world.item.BowItem || 
            stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
            stack.getItem() instanceof net.minecraft.world.item.TridentItem) {
            return true;
        }
        String namespace = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        if (namespace.equals("tacz") || name.contains("gun") || name.contains("rifle") || name.contains("pistol") || name.contains("shotgun")) {
            return true;
        }
        return false;
    }

    public static void spawnEnhancedHitVfx(double x, double y, double z, double lookX, double lookZ, int level) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !ClientConfig.weaponVfx || level <= 0) {
            return;
        }
        Vec3 center = new Vec3(x, y, z);
        Vec3 look = new Vec3(lookX, 0.0D, lookZ);
        if (look.lengthSqr() < 0.0001D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
        int highTierLevel = Math.max(0, level - 10);
        int slashCount = 8 + Math.min(16, level + highTierLevel);
        
        if (level >= 16) {
            slashCount = 24 + (level - 16) * 4;
            if (level >= 20) slashCount = 36;
        }
        
        if (ClientConfig.hitVfx) {
            for (int i = 0; i < slashCount; i++) {
                double progress = (double) i / Math.max(1, slashCount - 1);
                double curve = Math.sin(progress * Math.PI);
                
                double widthScale = (level >= 20) ? 2.1D : (level == 19 ? 2.0D : (level == 18 ? 1.9D : (level == 17 ? 1.8D : (level == 16 ? 1.7D : 1.15D))));
                double heightScale = (level >= 20) ? 1.3D : (level == 19 ? 1.25D : (level == 18 ? 1.2D : (level == 17 ? 1.15D : (level == 16 ? 1.1D : 0.75D))));
                double depthScale = (level >= 20) ? -0.32D : (level == 19 ? -0.31D : (level == 18 ? -0.3D : (level == 17 ? -0.28D : (level == 16 ? -0.27D : -0.18D))));
                
                Vec3 pos = center
                    .add(side.scale((progress - 0.5D) * widthScale))
                    .add(0.0D, (progress - 0.5D) * heightScale + curve * (level >= 16 ? 0.35D : 0.2D), 0.0D)
                    .add(look.scale(depthScale + curve * (level >= 16 ? 0.24D : 0.16D)));
                
                minecraft.level.addParticle(dust(level, false), pos.x, pos.y, pos.z, 0.0D, 0.02D, 0.0D);
            }
        }
        
        if (level >= 20) {
            if (ClientConfig.hitVfx) {
                minecraft.level.addParticle(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.5D, center.z, 0.0D, 0.0D, 0.0D);
                minecraft.level.addParticle(ParticleTypes.FLASH, center.x, center.y + 0.5D, center.z, 0.0D, 0.0D, 0.0D);
                for (int i = 0; i < 20; i++) {
                    double rx = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.8D;
                    double ry = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.8D;
                    double rz = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.8D;
                    minecraft.level.addParticle(ParticleTypes.DRAGON_BREATH, center.x + rx, center.y + ry + 0.5D, center.z + rz, rx * 0.08D, 0.03D, rz * 0.08D);
                    minecraft.level.addParticle(ParticleTypes.PORTAL, center.x + rx * 0.6D, center.y + ry * 0.6D + 0.5D, center.z + rz * 0.6D, rx * 0.05D, 0.02D, rz * 0.05D);
                    minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK, center.x + rx, center.y + ry + 0.5D, center.z + rz, rx * 0.1D, 0.05D, rz * 0.1D);
                }
            }
            if (ClientConfig.soundVfx) {
                minecraft.level.playLocalSound(center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.7F, false);
                minecraft.level.playLocalSound(center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.DRAGON_FIREBALL_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F, false);
            }
        } else if (level == 19) {
            if (ClientConfig.hitVfx) {
                minecraft.level.addParticle(ParticleTypes.EXPLOSION, center.x, center.y + 0.5D, center.z, 0.0D, 0.0D, 0.0D);
                minecraft.level.addParticle(ParticleTypes.FLASH, center.x, center.y + 0.5D, center.z, 0.0D, 0.0D, 0.0D);
                for (int i = 0; i < 20; i++) {
                    double rx = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.6D;
                    double ry = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.6D;
                    double rz = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.6D;
                    minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK, center.x + rx, center.y + ry + 0.5D, center.z + rz, rx * 0.1D, 0.05D, rz * 0.1D);
                    minecraft.level.addParticle(ParticleTypes.GLOW, center.x + rx * 0.5D, center.y + ry * 0.5D + 0.5D, center.z + rz * 0.5D, rx * 0.05D, 0.02D, rz * 0.05D);
                }
            }
            if (ClientConfig.soundVfx) {
                minecraft.level.playLocalSound(center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.4F, false);
                minecraft.level.playLocalSound(center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.5F, false);
            }
        } else if (level == 18) {
            if (ClientConfig.hitVfx) {
                minecraft.level.addParticle(ParticleTypes.EXPLOSION, center.x, center.y + 0.5D, center.z, 0.0D, 0.0D, 0.0D);
                for (int i = 0; i < 20; i++) {
                    double rx = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.6D;
                    double ry = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.6D;
                    double rz = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.6D;
                    minecraft.level.addParticle(ParticleTypes.FLAME, center.x + rx, center.y + ry + 0.5D, center.z + rz, rx * 0.1D, 0.05D, rz * 0.1D);
                    minecraft.level.addParticle(ParticleTypes.LAVA, center.x + rx * 0.5D, center.y + ry * 0.5D + 0.5D, center.z + rz * 0.5D, 0, 0.01D, 0);
                }
            }
            if (ClientConfig.soundVfx) {
                minecraft.level.playLocalSound(center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.BLAZE_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.3F, false);
                minecraft.level.playLocalSound(center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.4F, false);
            }
        } else if (level == 17) {
            if (ClientConfig.hitVfx) {
                for (int i = 0; i < 18; i++) {
                    double rx = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.5D;
                    double ry = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.5D;
                    double rz = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.5D;
                    minecraft.level.addParticle(ParticleTypes.PORTAL, center.x + rx, center.y + ry + 0.5D, center.z + rz, rx * 0.05D, 0.02D, rz * 0.05D);
                    minecraft.level.addParticle(ParticleTypes.WITCH, center.x + rx * 0.6D, center.y + ry * 0.6D + 0.5D, center.z + rz * 0.6D, 0, 0.01D, 0);
                }
            }
            if (ClientConfig.soundVfx) {
                minecraft.level.playLocalSound(center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F, false);
                minecraft.level.playLocalSound(center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.6F, false);
            }
        } else if (level == 16) {
            if (ClientConfig.hitVfx) {
                minecraft.level.addParticle(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.5D, center.z, 0.0D, 0.0D, 0.0D);
                for (int i = 0; i < 15; i++) {
                    double rx = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.5D;
                    double ry = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.5D;
                    double rz = (minecraft.level.getRandom().nextDouble() - 0.5D) * 1.5D;
                    minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK, center.x + rx, center.y + ry + 0.5D, center.z + rz, rx * 0.1D, 0.05D, rz * 0.1D);
                    minecraft.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, center.x + rx * 0.5D, center.y + ry * 0.5D + 0.5D, center.z + rz * 0.5D, rx * 0.05D, 0.02D, rz * 0.05D);
                }
            }
            if (ClientConfig.soundVfx) {
                minecraft.level.playLocalSound(center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.TRIDENT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 1.6F, false);
            }
        } else if (level >= 10) {
            if (ClientConfig.hitVfx) {
                for (int i = 0; i < 5 + highTierLevel; i++) {
                    minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        center.x + (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.45D,
                        center.y + (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.45D,
                        center.z + (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.45D,
                        (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.05D,
                        minecraft.level.getRandom().nextDouble() * 0.04D,
                        (minecraft.level.getRandom().nextDouble() - 0.5D) * 0.05D);
                }
            }
        }
    }


    private static void spawnArmorFlowVfx(Minecraft minecraft) {
        // 성능 최적화를 위해 방어구 오라 파티클 스폰 비활성화
    }

    private static void spawnDroppedItemVfx(Minecraft minecraft) {
        // 성능 최적화를 위해 드롭 아이템 파티클 스폰 비활성화
    }

    private static DustParticleOptions dust(int level, boolean armor) {
        if (level >= 20) {
            // 20강: 마법 무지개 그라데이션 광채 (시간에 따라 색상 변화)
            double phase = (System.currentTimeMillis() % 2000L) / 2000.0D * Math.PI * 2.0D;
            float r = (float) (Math.sin(phase) * 0.5D + 0.5D);
            float g = (float) (Math.sin(phase + Math.PI * 2.0D / 3.0D) * 0.5D + 0.5D);
            float b = (float) (Math.sin(phase + Math.PI * 4.0D / 3.0D) * 0.5D + 0.5D);
            return new DustParticleOptions(new Vector3f(r, g, b), 1.2F);
        }
        if (level == 19) {
            // 19강: 황금빛 테마 (Gold)
            return new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.0F), 1.0F);
        }
        if (level == 18) {
            // 18강: 진홍빛 테마 (Crimson)
            return new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.05F), 0.95F);
        }
        if (level == 17) {
            // 17강: 보랏빛/자주 테마 (Purple/Magenta)
            return new DustParticleOptions(new Vector3f(0.75F, 0.0F, 0.95F), 0.9F);
        }
        if (level == 16) {
            // 16강: 청록색 테마 (Cyan/Teal)
            return new DustParticleOptions(new Vector3f(0.0F, 0.95F, 0.85F), 0.85F);
        }
        if (level >= 10) {
            return new DustParticleOptions(armor ? new Vector3f(0.35F, 0.82F, 1.0F) : new Vector3f(1.0F, 0.72F, 0.16F), 0.72F);
        }
        if (level >= 6) {
            return new DustParticleOptions(armor ? new Vector3f(0.38F, 0.95F, 0.66F) : new Vector3f(0.74F, 0.38F, 1.0F), 0.62F);
        }
        return new DustParticleOptions(armor ? new Vector3f(0.76F, 0.92F, 1.0F) : new Vector3f(0.96F, 0.9F, 0.54F), 0.54F);
    }

    private static void renderHotbarEnhanceBadges(GuiGraphics graphics, Minecraft minecraft, int width, int height) {
        if (minecraft.player == null) {
            return;
        }
        int left = width / 2 - 91;
        int top = height - 22;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            int level = enhanceLevel(stack);
            if (level > 0) {
                int x = left + 3 + slot * 20;
                int y = top + 3;
                boolean isArmor = isArmor(stack);
                boolean isWeapon = !isArmor;
                if ((!isArmor || isArmorVfxEnabled()) && (!isWeapon || isWeaponVfxEnabled())) {
                    if (ClientConfig.itemVfx) {
                        renderEnhanceIconEffect(graphics, minecraft, x, y, level);
                    }
                }
                renderEnhanceBadge(graphics, minecraft, x, y, level);
            }
        }
    }

    private static void renderEnhanceIconEffect(GuiGraphics graphics, Minecraft minecraft, int x, int y, int level) {
        float r = 1.0F, g = 1.0F, b = 1.0F, a = 0.75F;
        
        if (level >= 20) {
            // 20강+: 시간에 따라 순환하는 무지개 빛깔 틴트
            double phase = (System.currentTimeMillis() % 2000L) / 2000.0D * Math.PI * 2.0D;
            r = (float) (Math.sin(phase) * 0.4F + 0.6F);
            g = (float) (Math.sin(phase + Math.PI * 2.0D / 3.0D) * 0.4F + 0.6F);
            b = (float) (Math.sin(phase + Math.PI * 4.0D / 3.0D) * 0.4F + 0.6F);
            a = 0.85F;
        } else if (level == 19) {
            r = 1.0F; g = 0.85F; b = 0.0F; a = 0.80F; // 황금
        } else if (level == 18) {
            r = 1.0F; g = 0.05F; b = 0.05F; a = 0.80F; // 진홍
        } else if (level == 17) {
            r = 0.78F; g = 0.0F; b = 1.0F; a = 0.80F; // 보라
        } else if (level == 16) {
            r = 0.0F; g = 1.0F; b = 0.80F; a = 0.80F; // 청록
        } else if (level >= 10) {
            r = 1.0F; g = 0.70F; b = 0.16F; a = 0.70F; // 노란색
        } else if (level >= 6) {
            r = 0.60F; g = 0.36F; b = 1.0F; a = 0.65F; // 연보라
        } else {
            r = 0.30F; g = 0.70F; b = 1.0F; a = 0.60F; // 푸른빛
        }

        // 블렌딩 활성화 및 틴트 적용
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);

        // 시간에 따른 프레임 인덱스 연산 (16개 프레임 순환)
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        int frameIndex = (int) ((tick / 2L + level * 2L) % 16L);
        int u = frameIndex * 128;

        // 슬롯 안쪽(x+1, y+1)에 14x14 크기로 축소 렌더링 (U/V 크기는 128x128)
        graphics.blit(ENHANCE_GLINT_TEX, x + 1, y + 1, 14, 14, (float)u, 0.0F, 128, 128, 2048, 128);

        // 렌더 상태 복구
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private static void renderEnhanceBadge(GuiGraphics graphics, Minecraft minecraft, int x, int y, int level) {
        int color;
        if (level >= 20) {
            // 20강의 경우 뱃지 텍스트가 무지개 그라데이션으로 부드럽게 점멸/변화
            double phase = (System.currentTimeMillis() % 1500L) / 1500.0D * Math.PI * 2.0D;
            int r = (int) (Math.sin(phase) * 60 + 195); // 135~255
            int g = (int) (Math.sin(phase + Math.PI * 2.0D / 3.0D) * 60 + 195);
            int b = (int) (Math.sin(phase + Math.PI * 4.0D / 3.0D) * 60 + 195);
            color = 0xFF000000 | (r << 16) | (g << 8) | b;
        } else if (level == 19) {
            color = 0xFFFFD700; // 황금색
        } else if (level == 18) {
            color = 0xFFEE0000; // 진홍색
        } else if (level == 17) {
            color = 0xFFC700FF; // 보라색
        } else if (level == 16) {
            color = 0xFF00FFCC; // 청록색
        } else if (level >= 10) {
            color = 0xFFFFB21A;
        } else if (level >= 6) {
            color = 0xFFB15CFF;
        } else {
            color = 0xFF7FD7FF;
        }
        graphics.fill(x, y, x + 16, y + 1, color);
        graphics.fill(x, y, x + 1, y + 16, color);
        graphics.fill(x + 12, y + 12, x + 16, y + 16, 0xB0000000);
        String text = "+" + level;
        float scale = 0.5F;
        graphics.pose().pushPose();
        graphics.pose().translate(x + 13.0F, y + 13.0F, 200.0F);
        graphics.pose().scale(scale, scale, scale);
        graphics.drawString(minecraft.font, text, -minecraft.font.width(text), -minecraft.font.lineHeight, color, true);
        graphics.pose().popPose();
    }

    private static int enhanceLevel(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0;
        }
        return Math.max(0, stack.getOrCreateTag().getInt(ENHANCE_LEVEL_TAG));
    }

    private static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }

    private static void spawnMinerEyeParticles(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || minerEyeOreCache.isEmpty()) {
            return;
        }

        int count = Math.min(36, minerEyeOreCache.size());
        for (int i = 0; i < count; i++) {
            BlockPos pos = minerEyeOreCache.get(player.getRandom().nextInt(minerEyeOreCache.size()));
            if (!isOreBlockState(minecraft.level.getBlockState(pos))) {
                continue;
            }
            double px = pos.getX() + 0.5D + (player.getRandom().nextDouble() - 0.5D) * 0.6D;
            double py = pos.getY() + 0.5D + (player.getRandom().nextDouble() - 0.5D) * 0.6D;
            double pz = pos.getZ() + 0.5D + (player.getRandom().nextDouble() - 0.5D) * 0.6D;

            minecraft.level.addParticle(ParticleTypes.GLOW, px, py, pz, 0.0D, 0.0D, 0.0D);
            if (player.getRandom().nextDouble() < 0.4D) {
                minecraft.level.addParticle(ParticleTypes.INSTANT_EFFECT, px, py, pz, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void refreshMinerEyeOreCache(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            minerEyeOreCache.clear();
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        BlockPos center = player.blockPosition();
        if (minerEyeLastScanTick >= 0L && gameTime - minerEyeLastScanTick < 10L && center.distSqr(minerEyeLastScanCenter) < 9.0D) {
            return;
        }
        minerEyeLastScanTick = gameTime;
        minerEyeLastScanCenter = center.immutable();
        minerEyeOreCache.clear();

        int radius = Math.min(32, ClientMinerData.minerEyeRadius());
        int radiusSq = radius * radius;
        List<BlockPos> found = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (isOreBlockState(minecraft.level.getBlockState(pos))) {
                        found.add(pos.immutable());
                    }
                }
            }
        }
        found.sort((first, second) -> Double.compare(first.distSqr(center), second.distSqr(center)));
        int limit = Math.min(1600, found.size());
        for (int i = 0; i < limit; i++) {
            minerEyeOreCache.add(found.get(i));
        }
    }

    private static void renderMinerEyeXray(RenderLevelStageEvent event, Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || !ClientMinerData.minerEyeActive() || ClientMinerData.minerEyeRadius() <= 0 || minerEyeOreCache.isEmpty()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(-camera.x, -camera.y, -camera.z);

        // 깊이 버퍼 테스트를 비활성화하고 블렌딩을 적용하여 완벽한 벽 너머 엑스레이 투시 효과 구현
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);

        int rendered = 0;
        for (BlockPos pos : minerEyeOreCache) {
            BlockState state = minecraft.level.getBlockState(pos);
            if (!isOreBlockState(state)) {
                continue;
            }
            float[] color = oreColor(state);
            AABB box = new AABB(pos).inflate(0.015D);
            
            LevelRenderer.addChainedFilledBoxVertices(event.getPoseStack(), bufferSource.getBuffer(RenderType.debugFilledBox()), box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color[0], color[1], color[2], 0.34F);
            LevelRenderer.renderLineBox(event.getPoseStack(), bufferSource.getBuffer(RenderType.lines()), box, color[0], color[1], color[2], 1.0F);
            
            if (++rendered >= 900) {
                break;
            }
        }

        // 배치 버퍼를 마감하고 드로우 콜을 실행하여 렌더링 완료
        bufferSource.endBatch(RenderType.debugFilledBox());
        bufferSource.endBatch(RenderType.lines());

        // 렌더 상태 복구
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        com.mojang.blaze3d.systems.RenderSystem.enableCull();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        event.getPoseStack().popPose();
    }

    private static float[] oreColor(BlockState state) {
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.contains("diamond")) {
            return new float[] {0.25F, 0.95F, 1.0F};
        }
        if (path.contains("emerald")) {
            return new float[] {0.2F, 1.0F, 0.45F};
        }
        if (path.contains("redstone")) {
            return new float[] {1.0F, 0.15F, 0.12F};
        }
        if (path.contains("lapis")) {
            return new float[] {0.25F, 0.45F, 1.0F};
        }
        if (path.contains("gold")) {
            return new float[] {1.0F, 0.78F, 0.18F};
        }
        if (path.contains("copper")) {
            return new float[] {1.0F, 0.45F, 0.2F};
        }
        if (path.contains("iron")) {
            return new float[] {0.9F, 0.75F, 0.55F};
        }
        if (path.contains("coal")) {
            return new float[] {0.35F, 0.35F, 0.35F};
        }
        if (path.contains("ancient_debris") || path.contains("netherite")) {
            return new float[] {0.95F, 0.45F, 0.22F};
        }
        return new float[] {0.75F, 0.82F, 1.0F};
    }

    private static boolean isOreBlockState(BlockState state) {
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("ore") || path.equals("ancient_debris");
    }

    public static boolean shouldRenderMinerEyeBlock(BlockState state, BlockPos pos) {
        if (!ClientMinerData.minerEyeActive() || ClientMinerData.minerEyeRadius() <= 0) {
            return true;
        }
        return isOreBlockState(state);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.isEmpty() && stack.isEdible() && stack.hasTag()) {
            net.minecraft.nbt.CompoundTag tag = stack.getTag();
            if (tag.contains("CookedByPlayer")) {
                List<Component> tooltips = event.getToolTip();
                
                tooltips.add(Component.literal("§8[👨‍🍳 제작자: " + tag.getString("CookedByPlayer") + "]"));

                if (tag.contains("AgeingLevel")) {
                    int ageingLevel = tag.getInt("AgeingLevel");
                    if (ageingLevel > 0) {
                        tooltips.add(Component.literal("§6⭐ 숙성도: " + ageingLevel + "단계§7 (효율 +" + (ageingLevel * 30) + "%)"));
                    }
                }

                if (tag.contains("RecipeBuffs")) {
                    net.minecraft.nbt.ListTag buffsNbt = tag.getList("RecipeBuffs", net.minecraft.nbt.Tag.TAG_STRING);
                    if (buffsNbt.size() > 0) {
                        tooltips.add(Component.literal("§5🧪 나만의 레시피 특수 비법:"));
                        for (int i = 0; i < buffsNbt.size(); i++) {
                            String buff = buffsNbt.getString(i);
                            switch (buff) {
                                case "DEATH_PREVENTION":
                                    tooltips.add(Component.literal("  §d- 🛡️ 죽음 방지 1회 각인"));
                                    break;
                                case "BOSS_DAMAGE":
                                    tooltips.add(Component.literal("  §c- ⚔️ 보스 처단자 (피해 +20%) 각인"));
                                    break;
                                case "GOLDEN_LUCK":
                                    tooltips.add(Component.literal("  §e- 🍀 황금 행운 각인"));
                                    break;
                                case "HEART_BREATH":
                                    {
                                        int recipeLevel = tag.contains("MasterRecipeLevel") ? tag.getInt("MasterRecipeLevel") : 1;
                                        int percent = 10 + recipeLevel * 10;
                                        tooltips.add(Component.literal("  §d- 💖 대지의 숨결 (최대체력 +" + percent + "%) 각인"));
                                    }
                                    break;
                                case "IMMUNITY":
                                    tooltips.add(Component.literal("  §b- 🧪 절대 면역 각인"));
                                    break;
                                case "STEEL_GUARD":
                                    tooltips.add(Component.literal("  §7- 🏹 강철 수호 (투사체피해 -30%) 각인"));
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void invokeRenderTooltipInternal(GuiGraphics graphics, net.minecraft.client.gui.Font font, List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> components, int x, int y, net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner) {
        if (renderTooltipInternalMethod == null) {
            try {
                // Mojang official 매핑
                renderTooltipInternalMethod = GuiGraphics.class.getDeclaredMethod("renderTooltipInternal", net.minecraft.client.gui.Font.class, List.class, int.class, int.class, net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner.class);
                renderTooltipInternalMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                try {
                    // SRG/Obfuscated 매핑
                    renderTooltipInternalMethod = GuiGraphics.class.getDeclaredMethod("m_280145_", net.minecraft.client.gui.Font.class, List.class, int.class, int.class, net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner.class);
                    renderTooltipInternalMethod.setAccessible(true);
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException("Cannot find renderTooltipInternal in GuiGraphics", ex);
                }
            }
        }
        try {
            renderTooltipInternalMethod.invoke(graphics, font, components, x, y, positioner);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
        if (isRenderingTooltip) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> components = event.getComponents();
        if (components == null || components.isEmpty()) return;
        
        int totalHeight = 0;
        for (var comp : components) {
            totalHeight += comp.getHeight();
        }
        if (components.size() > 1) {
            totalHeight += (components.size() - 1) * 2;
        }
        totalHeight += 12; // 툴팁 위아래 패딩 픽셀 고려
        
        int maxHeight = (int) (screenHeight * 0.85); // 화면 세로 크기의 85% 제한
        if (totalHeight > maxHeight) {
            float scale = (float) maxHeight / totalHeight;
            if (scale < 0.5f) scale = 0.5f; // 지나치게 작아지지 않도록 50% 제한
            
            event.setCanceled(true); // 기본 그리기 취소
            
            isRenderingTooltip = true;
            try {
                GuiGraphics graphics = event.getGraphics();
                PoseStack poseStack = graphics.pose();
                poseStack.pushPose();
                
                int tx = event.getX();
                int ty = event.getY();
                
                // 원점 기준으로 스케일 조정
                poseStack.translate(tx, ty, 0);
                poseStack.scale(scale, scale, 1.0f);
                poseStack.translate(-tx, -ty, 0);
                
                invokeRenderTooltipInternal(
                    graphics,
                    event.getFont(), 
                    components, 
                    tx, 
                    ty,
                    net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE
                );
                
                poseStack.popPose();
            } finally {
                isRenderingTooltip = false;
            }
        }
    }

    private static void renderGlowingCylinder(com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource bufferSource, double cx, double cy, double cz, double radius, double height, float r, float g, float b, float alpha) {
        VertexConsumer fillBuilder = bufferSource.getBuffer(RenderType.debugFilledBox());
        VertexConsumer lineBuilder = bufferSource.getBuffer(RenderType.lines());

        int segments = 48;
        org.joml.Matrix4f mat = poseStack.last().pose();

        for (int i = 0; i < segments; i++) {
            double angle1 = (i / (double) segments) * Math.PI * 2.0;
            double angle2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) (cx + Math.cos(angle1) * radius);
            float z1 = (float) (cz + Math.sin(angle1) * radius);
            float x2 = (float) (cx + Math.cos(angle2) * radius);
            float z2 = (float) (cz + Math.sin(angle2) * radius);

            float yMin = (float) cy;
            float yMax = (float) (cy + height);

            fillBuilder.vertex(mat, x1, yMin, z1).color(r, g, b, alpha).endVertex();
            fillBuilder.vertex(mat, x2, yMin, z2).color(r, g, b, alpha).endVertex();
            fillBuilder.vertex(mat, x2, yMax, z2).color(r, g, b, alpha).endVertex();
            fillBuilder.vertex(mat, x1, yMax, z1).color(r, g, b, alpha).endVertex();
        }

        for (int i = 0; i < segments; i++) {
            double angle1 = (i / (double) segments) * Math.PI * 2.0;
            double angle2 = ((i + 1) / (double) segments) * Math.PI * 2.0;

            float x1 = (float) (cx + Math.cos(angle1) * radius);
            float z1 = (float) (cz + Math.sin(angle1) * radius);
            float x2 = (float) (cx + Math.cos(angle2) * radius);
            float z2 = (float) (cz + Math.sin(angle2) * radius);

            float yMin = (float) cy;
            float yMax = (float) (cy + height);

            float lineAlpha = Math.min(1.0F, alpha * 2.0F);
            lineBuilder.vertex(mat, x1, yMin, z1).color(r, g, b, lineAlpha).normal(0f, -1f, 0f).endVertex();
            lineBuilder.vertex(mat, x2, yMin, z2).color(r, g, b, lineAlpha).normal(0f, -1f, 0f).endVertex();

            lineBuilder.vertex(mat, x1, yMax, z1).color(r, g, b, lineAlpha).normal(0f, 1f, 0f).endVertex();
            lineBuilder.vertex(mat, x2, yMax, z2).color(r, g, b, lineAlpha).normal(0f, 1f, 0f).endVertex();

            if (i % (segments / 4) == 0) {
                lineBuilder.vertex(mat, x1, yMin, z1).color(r, g, b, lineAlpha).normal(x1 - (float)cx, 0f, z1 - (float)cz).endVertex();
                lineBuilder.vertex(mat, x1, yMax, z1).color(r, g, b, lineAlpha).normal(x1 - (float)cx, 0f, z1 - (float)cz).endVertex();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.getVehicle() instanceof com.nogeon.economyland.entity.ScrapDroneEntity) {
            net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> model = event.getRenderer().getModel();
            
            // 다리 각도를 곧게 펴서 쩍벌 방지
            model.leftLeg.xRot = 0.0F;
            model.leftLeg.yRot = 0.0F;
            model.leftLeg.zRot = 0.0F;
            model.rightLeg.xRot = 0.0F;
            model.rightLeg.yRot = 0.0F;
            model.rightLeg.zRot = 0.0F;
            
            // 양팔을 수직 위로 뻗음
            model.leftArm.xRot = -3.0F;
            model.leftArm.yRot = 0.0F;
            model.leftArm.zRot = 0.0F;
            model.rightArm.xRot = -3.0F;
            model.rightArm.yRot = 0.0F;
            model.rightArm.zRot = 0.0F;
        }
    }
}
