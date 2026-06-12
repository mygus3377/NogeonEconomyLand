package com.nogeon.economyland.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.FarmerScarecrowEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class FarmerScarecrowModel extends EntityModel<FarmerScarecrowEntity> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(new ResourceLocation(NoGeonEconomyLand.MOD_ID, "farmer_scarecrow"), "main");

    private final ModelPart root;

    public FarmerScarecrowModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("pole", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-1.0F, -24.0F, -1.0F, 2.0F, 24.0F, 2.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("arms", CubeListBuilder.create()
            .texOffs(8, 0).addBox(-15.0F, -18.0F, -1.0F, 30.0F, 2.0F, 2.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 26).addBox(-6.0F, -20.0F, -3.0F, 12.0F, 12.0F, 6.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(24, 4).addBox(-5.0F, -31.0F, -5.0F, 10.0F, 10.0F, 10.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("hat_brim", CubeListBuilder.create()
            .texOffs(0, 44).addBox(-8.0F, -32.0F, -8.0F, 16.0F, 2.0F, 16.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("hat_top", CubeListBuilder.create()
            .texOffs(36, 26).addBox(-5.0F, -36.0F, -5.0F, 10.0F, 4.0F, 10.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FarmerScarecrowEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
