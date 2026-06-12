package com.nogeon.economyland.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tfar.classicbar.impl.BarOverlayImpl;
import tfar.classicbar.impl.overlays.mod.Feathers;
import tfar.classicbar.util.Color;
import tfar.classicbar.util.ColorUtils;
import tfar.classicbar.util.ModUtils;

import java.lang.reflect.Method;

@Mixin(value = Feathers.class, remap = false)
public abstract class FeathersMixin extends BarOverlayImpl {

    public FeathersMixin(String name) {
        super(name);
    }

    private int nogeon$getFeathers() {
        try {
            Class<?> helper = Class.forName("com.elenai.feathers.api.FeathersHelper");
            Method method = helper.getMethod("getFeathers");
            return (Integer) method.invoke(null);
        } catch (Exception e) {
            return 0;
        }
    }

    private int nogeon$getMaxFeathers() {
        try {
            Class<?> helper = Class.forName("com.elenai.feathers.api.FeathersHelper");
            Method method = helper.getMethod("getMaxFeathers");
            return (Integer) method.invoke(null);
        } catch (Exception e) {
            return 20;
        }
    }

    /**
     * @author Antigravity
     * @reason Correct the horizontal positioning (getHOffset) for the Feathers bar instead of hardcoded right-side value.
     */
    @Overwrite
    public void renderBar(ForgeGui gui, GuiGraphics graphics, Player player, int width, int height, int offset) {
        int feathers = nogeon$getFeathers();
        int maxFeathers = nogeon$getMaxFeathers();
        
        int xStart = (width / 2) + getHOffset();
        int yStart = height - offset;
        
        GlStateManager._enableBlend();
        Color.reset();
        
        // 배경 렌더링
        renderFullBarBackground(graphics, xStart, yStart);
        
        // 바 너비 계산
        double barWidth = getBarWidth(player);
        double xOffset = rightHandSide() ? (77.0 - barWidth) : 0.0;
        
        // 깃털 게이지 색상 (#22a5f0) 적용
        ColorUtils.hex2Color("#22a5f0").color2Gl();
        renderPartialBar(graphics, xStart + 2.0 + xOffset, yStart + 2, barWidth);
    }

    /**
     * @author Antigravity
     * @reason Correct the horizontal positioning (getIconOffset) for the Feathers icon instead of hardcoded right-side value.
     */
    @Overwrite
    public void renderIcon(GuiGraphics graphics, Player player, int width, int height, int offset) {
        int xStart = (width / 2) + getIconOffset();
        int yStart = height - offset;
        
        ModUtils.drawTexturedModalRect(graphics, xStart, yStart, 82, 34, 9, 9);
    }

    /**
     * @author Antigravity
     * @reason Correct the horizontal positioning (getIconOffset) for the Feathers text instead of hardcoded right-side value.
     */
    @Overwrite
    public void renderText(GuiGraphics graphics, Player player, int width, int height, int offset) {
        int feathers = nogeon$getFeathers();
        int color = Integer.decode("#22a5f0");
        
        int xStart = (width / 2) + getIconOffset();
        int yStart = height - offset;
        
        textHelper(graphics, xStart, yStart, feathers, color);
    }
}
