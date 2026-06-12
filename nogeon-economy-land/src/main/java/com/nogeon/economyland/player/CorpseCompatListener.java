package com.nogeon.economyland.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import de.maxhenkel.corpse.entities.CorpseEntity;
import de.maxhenkel.corpse.corelib.death.PlayerDeathEvent;

import java.lang.reflect.Field;

public final class CorpseCompatListener {

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof CorpseEntity corpse) {
            corpse.getCorpseUUID().ifPresent(ownerUuid -> {
                if (InventoryKeepService.ACTIVE_KEEP_PLAYERS.contains(ownerUuid)) {
                    event.setCanceled(true);
                    System.out.println("[NoGeonEconomyLand] Prevented CorpseEntity spawn for player: " + ownerUuid);
                }
            });
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onCorpsePlayerDeath(PlayerDeathEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null && InventoryKeepService.ACTIVE_KEEP_PLAYERS.contains(player.getUUID())) {
            try {
                Field storeDeathField = PlayerDeathEvent.class.getDeclaredField("storeDeath");
                storeDeathField.setAccessible(true);
                storeDeathField.setBoolean(event, false);

                Field removeDropsField = PlayerDeathEvent.class.getDeclaredField("removeDrops");
                removeDropsField.setAccessible(true);
                removeDropsField.setBoolean(event, false);

                System.out.println("[NoGeonEconomyLand] Disabled Corpse database storage & drop removal via reflection for: " + player.getName().getString());
            } catch (Exception e) {
                System.err.println("[NoGeonEconomyLand] Failed to reflect PlayerDeathEvent storeDeath / removeDrops fields");
                e.printStackTrace();
            }
        }
    }
}
