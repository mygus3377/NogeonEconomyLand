package com.nogeon.economyland.client;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.FarmerScarecrowEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class FarmerScarecrowRenderer extends MobRenderer<FarmerScarecrowEntity, FarmerScarecrowModel> {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation(NoGeonEconomyLand.MOD_ID, "textures/entity/farmer_scarecrow.png");

    public FarmerScarecrowRenderer(EntityRendererProvider.Context context) {
        super(context, new FarmerScarecrowModel(context.bakeLayer(FarmerScarecrowModel.LAYER)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(FarmerScarecrowEntity entity) {
        return TEXTURE;
    }
}
