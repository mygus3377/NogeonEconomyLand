package com.nogeon.economyland.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Unique
    private boolean nogeon$scaled = false;

    @Inject(
        method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
        at = @At("HEAD")
    )
    private void onRenderTooltipInternalHead(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo ci) {
        this.nogeon$scaled = false;
        if (components == null || components.isEmpty()) {
            return;
        }

        int totalHeight = 0;
        int maxWidth = 0;
        for (ClientTooltipComponent comp : components) {
            totalHeight += comp.getHeight();
            int w = comp.getWidth(font);
            if (w > maxWidth) {
                maxWidth = w;
            }
        }
        if (components.size() > 1) {
            totalHeight += (components.size() - 1) * 2;
        }
        int tooltipHeight = totalHeight + 6;
        int tooltipWidth = maxWidth + 12;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        int maxAllowedWidth = (int) (screenWidth * 0.90f);
        int maxHeight = (int) (screenHeight * 0.90f);

        float scaleFactorX = 1.0f;
        if (tooltipWidth > maxAllowedWidth) {
            scaleFactorX = (float) maxAllowedWidth / (float) tooltipWidth;
        }
        
        float scaleFactorY = 1.0f;
        if (tooltipHeight > maxHeight) {
            scaleFactorY = (float) maxHeight / (float) tooltipHeight;
        }

        float scaleFactor = Math.min(scaleFactorX, scaleFactorY);
        if (scaleFactor < 0.999f) {
            if (scaleFactor < 0.4f) {
                scaleFactor = 0.4f;
            }

            GuiGraphics graphics = (GuiGraphics) (Object) this;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0f);
            graphics.pose().scale(scaleFactor, scaleFactor, 1.0f);
            graphics.pose().translate(-x, -y, 0.0f);
            this.nogeon$scaled = true;
        }
    }

    @Inject(
        method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
        at = @At("RETURN")
    )
    private void onRenderTooltipInternalReturn(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo ci) {
        if (this.nogeon$scaled) {
            GuiGraphics graphics = (GuiGraphics) (Object) this;
            graphics.pose().popPose();
            this.nogeon$scaled = false;
        }
    }
}
