package com.nogeon.economyland.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nogeon.economyland.client.ClientCosmeticArmorData;
import com.nogeon.economyland.client.ClientForgeEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    private static final ThreadLocal<java.util.Map<java.util.UUID, ItemStack[]>> COSMETIC_BACKUPS = 
        ThreadLocal.withInitial(java.util.HashMap::new);

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private void onRenderHead(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        ClientForgeEvents.pushRenderingEntity(entity);

        if (entity.level() != null && entity.level().isClientSide() && entity instanceof Player player) {
            java.util.UUID uuid = player.getUUID();
            if (ClientCosmeticArmorData.has(uuid) && ClientCosmeticArmorData.isVisible(uuid)) {
                ItemStack[] backup = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    backup[i] = player.getInventory().armor.get(i);
                    
                    EquipmentSlot slot = switch (i) {
                        case 0 -> EquipmentSlot.FEET;
                        case 1 -> EquipmentSlot.LEGS;
                        case 2 -> EquipmentSlot.CHEST;
                        case 3 -> EquipmentSlot.HEAD;
                        default -> null;
                    };
                    if (slot != null) {
                        ItemStack cosmeticStack = ClientCosmeticArmorData.itemFor(uuid, slot);
                        player.getInventory().armor.set(i, cosmeticStack);
                    }
                }
                COSMETIC_BACKUPS.get().put(uuid, backup);
            }
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("RETURN")
    )
    private void onRenderReturn(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof Player player) {
            java.util.UUID uuid = player.getUUID();
            ItemStack[] backup = COSMETIC_BACKUPS.get().remove(uuid);
            if (backup != null) {
                for (int i = 0; i < 4; i++) {
                    player.getInventory().armor.set(i, backup[i]);
                }
            }
        }

        ClientForgeEvents.popRenderingEntity();
    }
}
