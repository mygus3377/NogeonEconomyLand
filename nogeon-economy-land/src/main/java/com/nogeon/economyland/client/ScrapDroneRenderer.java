package com.nogeon.economyland.client;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.ScrapDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ScrapDroneRenderer extends MobRenderer<ScrapDroneEntity, ScrapDroneModel> {
    private static final ResourceLocation TEXTURE_NORMAL =
        new ResourceLocation(NoGeonEconomyLand.MOD_ID, "textures/entity/scrap_drone_normal.png");
    private static final ResourceLocation TEXTURE_LOW =
        new ResourceLocation(NoGeonEconomyLand.MOD_ID, "textures/entity/scrap_drone_low.png");
    private static final ResourceLocation TEXTURE_COMBAT =
        new ResourceLocation(NoGeonEconomyLand.MOD_ID, "textures/entity/scrap_drone_combat.png");
    private static final ResourceLocation TEXTURE_HAPPY =
        new ResourceLocation(NoGeonEconomyLand.MOD_ID, "textures/entity/scrap_drone_happy.png");
    private static final ResourceLocation TEXTURE_WORRIED =
        new ResourceLocation(NoGeonEconomyLand.MOD_ID, "textures/entity/scrap_drone_worried.png");

    public ScrapDroneRenderer(EntityRendererProvider.Context context) {
        super(context, new ScrapDroneModel(context.bakeLayer(ScrapDroneModel.LAYER)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(ScrapDroneEntity entity) {
        int exp = entity.getExpression();
        if (exp == 1) {
            return TEXTURE_LOW;
        } else if (exp == 2) {
            return TEXTURE_COMBAT;
        } else if (exp == 3) {
            return TEXTURE_HAPPY;
        } else if (exp == 4) {
            return TEXTURE_WORRIED;
        }
        return TEXTURE_NORMAL;
    }

    @Override
    protected float getFlipDegrees(ScrapDroneEntity entity) {
        return 0.0F;
    }

    @Override
    public void render(ScrapDroneEntity entity, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() == entity && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        net.minecraft.world.item.ItemStack gun = entity.getEquipGun();
        if (gun != null && !gun.isEmpty()) {
            poseStack.pushPose();
            
            // 드론 하단 중앙에 총기를 배치하기 위해 translate
            poseStack.translate(0.0F, -0.35F, 0.0F);

            // 센서 스킬 레벨에 따른 스캔 범위 설정
            double range = 12.0D + (entity.getSensorLevel() == 0 ? 0 : (entity.getSensorLevel() - 1) * 5.0D);
            net.minecraft.world.entity.LivingEntity target = null;
            double minDist = Double.MAX_VALUE;

            java.util.List<net.minecraft.world.entity.LivingEntity> list = entity.level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                entity.getBoundingBox().inflate(range),
                e -> e != entity && e.isAlive() && e instanceof net.minecraft.world.entity.monster.Monster
            );

            for (net.minecraft.world.entity.LivingEntity le : list) {
                double dist = entity.distanceToSqr(le);
                if (dist < minDist) {
                    minDist = dist;
                    target = le;
                }
            }

            float yaw;
            float pitch;

            if (target != null) {
                // 타겟과의 상대 좌표 벡터를 기준으로 조준 회전값 산출
                double dx = target.getX() - entity.getX();
                double dy = (target.getY() + target.getEyeHeight() / 2.0D) - (entity.getY() - 0.35D);
                double dz = target.getZ() - entity.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);

                yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
                pitch = (float) (Math.atan2(dy, distance) * 180.0D / Math.PI);
            } else {
                // 타겟이 없는 경우 드론의 시선 각도를 그대로 쳐다봅니다.
                yaw = entity.getViewYRot(partialTicks);
                pitch = -entity.getViewXRot(partialTicks);
            }

            // 회전 적용 (총구가 앞을 향하도록 180도 보정)
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw + 180.0F));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));

            // 드론 크기에 비례하도록 0.6배 축소
            poseStack.scale(0.6F, 0.6F, 0.6F);

            // 실제 장착된 총기 아이템을 입체적인 3D 렌더링으로 드론 밑면에 투영
            net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
                gun,
                net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
            );

            poseStack.popPose();
        }
    }
}
