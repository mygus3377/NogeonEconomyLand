package com.nogeon.economyland.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nogeon.economyland.entity.EconomyTraderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public final class EconomyTraderRenderer extends MobRenderer<EconomyTraderEntity, VillagerModel<EconomyTraderEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/villager/villager.png");
    private static final int BUBBLE_TEXT = 0xFF4C402F;
    private static final int BUBBLE_BACKGROUND = 0xC84B3C30;

    public EconomyTraderRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EconomyTraderEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(EconomyTraderEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        renderSpeech(entity, poseStack, buffer, packedLight);
    }

    private void renderSpeech(EconomyTraderEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.speech() == null) {
            return;
        }
        Font font = getFont();
        FormattedCharSequence speech = entity.speech().getVisualOrderText();
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.95D, 0.0D);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        float textX = -font.width(speech) / 2.0F;
        float textY = -font.lineHeight - 6.0F;
        int backgroundAlpha = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.35F) * 255.0F) << 24;
        int speechBackground = backgroundAlpha == 0 ? BUBBLE_BACKGROUND : backgroundAlpha | (BUBBLE_BACKGROUND & 0x00FFFFFF);
        font.drawInBatch(speech, textX, textY, BUBBLE_TEXT, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, speechBackground, packedLight);
        font.drawInBatch(speech, textX, textY, BUBBLE_TEXT, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }
}
