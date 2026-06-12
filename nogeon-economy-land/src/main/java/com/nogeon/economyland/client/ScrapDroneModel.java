package com.nogeon.economyland.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.ScrapDroneEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class ScrapDroneModel extends EntityModel<ScrapDroneEntity> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(new ResourceLocation(NoGeonEconomyLand.MOD_ID, "scrap_drone"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart propeller;
    private final ModelPart chest;
    private final ModelPart gunMount;
    private final ModelPart antenna;
    private final ModelPart booster;

    public ScrapDroneModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.propeller = root.getChild("propeller");
        this.chest = root.getChild("chest");
        this.gunMount = root.getChild("gun_mount");
        this.antenna = root.getChild("antenna");
        this.booster = root.getChild("booster");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Detailed metallic core body with extruded visor, thrusters, and landing hook
        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F) // Central core
            .texOffs(24, 16).addBox(-2.0F, -4.0F, -3.5F, 4.0F, 2.0F, 0.5F) // Extruded front visor
            .texOffs(0, 20).addBox(3.0F, -5.0F, -2.0F, 2.0F, 4.0F, 4.0F) // Left side jet thruster
            .texOffs(16, 20).addBox(-5.0F, -5.0F, -2.0F, 2.0F, 4.0F, 4.0F) // Right side jet thruster
            .texOffs(32, 6).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F), // Lower hanging hook/ring
            PartPose.offset(0.0F, 24.0F, 0.0F));

        // Propeller shaft and blades (spins together)
        root.addOrReplaceChild("propeller", CubeListBuilder.create()
            .texOffs(32, 0).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 2.0F, 1.0F) // Propeller axle shaft
            .texOffs(0, 12).addBox(-6.0F, -8.5F, -0.5F, 12.0F, 0.5F, 1.0F), // Aerodynamic blades
            PartPose.offset(0.0F, 24.0F, 0.0F));

        // [UPGRADE] Chest/backpack storage compartment
        root.addOrReplaceChild("chest", CubeListBuilder.create()
            .texOffs(0, 28).addBox(-2.0F, -5.0F, 3.0F, 4.0F, 4.0F, 2.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));

        // [UPGRADE] Side gun barrel/receiver mount
        root.addOrReplaceChild("gun_mount", CubeListBuilder.create()
            .texOffs(12, 28).addBox(-4.5F, -2.0F, -4.0F, 1.0F, 1.0F, 4.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));

        // [UPGRADE] Blinking wireless transmission antenna
        root.addOrReplaceChild("antenna", CubeListBuilder.create()
            .texOffs(24, 28).addBox(-2.5F, -9.0F, -2.5F, 1.0F, 3.0F, 1.0F) // antenna rod
            .texOffs(28, 28).addBox(-3.0F, -10.0F, -3.0F, 2.0F, 1.0F, 2.0F), // telemetry receiver bulb
            PartPose.offset(0.0F, 24.0F, 0.0F));

        // [UPGRADE] Lower active dual plasma boosters
        root.addOrReplaceChild("booster", CubeListBuilder.create()
            .texOffs(36, 28).addBox(1.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F) // Left exhaust nozzle
            .texOffs(44, 28).addBox(-2.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F), // Right exhaust nozzle
            PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    private float currentDeathProgress = 0.0F;

    @Override
    public void setupAnim(ScrapDroneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Spin the propeller blades continuously, scaled by current charge level (0% to 100%)
        float chargeFactor = entity.getCharge() / 100.0F;
        float spinRate = 0.05F + 0.55F * chargeFactor;
        this.propeller.yRot = ageInTicks * spinRate;

        // Sync active upgrade model visual visibilities dynamically
        this.chest.visible = entity.hasUpgradeInventory();
        this.gunMount.visible = entity.hasUpgradeInventory() && entity.getEquipGun().isEmpty();
        this.antenna.visible = entity.hasUpgradeTransmitter();
        this.booster.visible = entity.hasUpgradeBooster();

        if (entity.deathTime > 0) {
            this.currentDeathProgress = Math.min(1.0F, entity.deathTime / 35.0F);
        } else {
            this.currentDeathProgress = 0.0F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (currentDeathProgress > 0.0F) {
            float tint = 1.0F - currentDeathProgress;
            red *= tint;
            green *= tint;
            blue *= tint;
        }
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

