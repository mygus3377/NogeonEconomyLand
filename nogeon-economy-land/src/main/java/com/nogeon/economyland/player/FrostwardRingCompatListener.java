package com.nogeon.economyland.player;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

public final class FrostwardRingCompatListener {
    private static MobEffect chilledEffect = null;
    private static Item frostwardRingItem = null;
    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        try {
            chilledEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("irons_spellbooks", "chilled"));
            frostwardRingItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "frostward_ring"));
        } catch (Exception e) {
            System.err.println("[NoGeonEconomyLand] Failed to lookup irons_spellbooks chilled or frostward_ring");
            e.printStackTrace();
        }
        initialized = true;
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        init();
        if (chilledEffect == null || frostwardRingItem == null) return;

        LivingEntity entity = event.getEntity();
        if (entity != null) {
            if (event.getEffectInstance().getEffect() == chilledEffect) {
                boolean hasRing = CuriosApi.getCuriosHelper()
                    .findFirstCurio(entity, frostwardRingItem)
                    .isPresent();
                if (hasRing) {
                    event.setResult(Event.Result.DENY);
                    System.out.println("[NoGeonEconomyLand] Blocked chilled effect on player: " + entity.getName().getString());
                }
            }
        }
    }
}
