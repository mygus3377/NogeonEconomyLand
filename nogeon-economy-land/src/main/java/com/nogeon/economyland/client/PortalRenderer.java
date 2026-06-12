package com.nogeon.economyland.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.PortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class PortalRenderer extends EntityRenderer<PortalEntity> {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation(NoGeonEconomyLand.MOD_ID, "textures/entity/portal.png");

    public PortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PortalEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(PortalEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // 포탈이 땅 속에 0.9블록 파묻히지 않도록 위로 이동
        poseStack.translate(0.0D, 0.9D, 0.0D);
        
        // 1. 빌보드 회전 처리 (카메라를 향하도록)
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // 2. 소용돌이 회전 애니메이션 (tickCount 기준)
        float age = entity.tickCount + partialTicks;
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 4.0F)); // 매 프레임 4도씩 회전

        poseStack.scale(1.8F, 1.8F, 1.0F); // 포탈 크기 스케일 조절

        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f poseMatrix = lastPose.pose();
        Matrix3f normalMatrix = lastPose.normal();
        
        // 투명하며 스스로 빛을 발하는 효과 (Emissive + Translucent)
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

        // 쿼드 렌더링
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F);
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, 0.5F, -0.5F, 0.0F, 1.0F, 1.0F);
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, 0.5F, 0.5F, 0.0F, 1.0F, 0.0F);
        vertex(vertexConsumer, poseMatrix, normalMatrix, packedLight, -0.5F, 0.5F, 0.0F, 0.0F, 0.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light, float x, float y, float z, float u, float v) {
        consumer.vertex(pose, x, y, z)
            .color(255, 255, 255, 255)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }
}
