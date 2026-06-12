package com.nogeon.economyland;

import com.nogeon.economyland.command.ModCommands;
import com.nogeon.economyland.entity.ModEntities;
import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.item.ReloadBoostHandler;
import com.nogeon.economyland.job.JobEvents;
import com.nogeon.economyland.land.LandEvents;
import com.nogeon.economyland.land.FtbChunksIntegration;
import com.nogeon.economyland.lottery.LotteryEvents;
import com.nogeon.economyland.menu.ModMenus;
import com.nogeon.economyland.network.MinerChargePacket;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.player.HomeTeleportService;
import com.nogeon.economyland.player.InventoryKeepService;
import com.nogeon.economyland.player.PlayerSyncEvents;
import com.nogeon.economyland.player.TemperatureAccessoryEvents;
import com.nogeon.economyland.shop.ItemLockEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(NoGeonEconomyLand.MOD_ID)
public final class NoGeonEconomyLand {
    public static final String MOD_ID = "nogeon_economy_land";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static boolean recipeCachesPrimed;

    public NoGeonEconomyLand() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.register(modBus);
        modBus.addListener(ModEntities::registerAttributes);
        ModItems.register(modBus);
        ModMenus.register(modBus);
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(ModCommands::register);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, JobEvents::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onBlockPlace);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onBreakSpeed);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onLeftClickBlock);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onFarmlandTrample);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onItemFished);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onItemCrafted);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onItemSmelted);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onLivingHeal);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onLivingDrops);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onLivingKnockBack);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onItemUseFinish);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onRightClickItem);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, JobEvents::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onGunShoot);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onWorldTick);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onCheckSpawn);
        MinecraftForge.EVENT_BUS.addListener(JobEvents::onCropGrowPre);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, InventoryKeepService::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(InventoryKeepService::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(InventoryKeepService::onPlayerRevived);
        MinecraftForge.EVENT_BUS.addListener(InventoryKeepService::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, InventoryKeepService::onLivingDrops);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, InventoryKeepService::onExperienceDrop);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ItemLockEvents::onItemToss);
        MinecraftForge.EVENT_BUS.addListener(LandEvents::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(LandEvents::onBlockPlace);
        MinecraftForge.EVENT_BUS.addListener(LandEvents::onMobSpawn);
        MinecraftForge.EVENT_BUS.addListener(LandEvents::onFarmlandTrample);
        MinecraftForge.EVENT_BUS.addListener(LandEvents::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(LandEvents::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(LandEvents::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LandEvents::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LandEvents::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LandEvents::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(LandEvents::onAttackEntity);
        MinecraftForge.EVENT_BUS.addListener(LandEvents::onExplosionDetonate);
        MinecraftForge.EVENT_BUS.addListener(TemperatureAccessoryEvents::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TemperatureAccessoryEvents::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TemperatureAccessoryEvents::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TemperatureAccessoryEvents::onPlayEntitySound);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TemperatureAccessoryEvents::onPlayPositionSound);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TemperatureAccessoryEvents::onEffectApplicable);
        MinecraftForge.EVENT_BUS.addListener(LotteryEvents::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(HomeTeleportService::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(PlayerSyncEvents::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(PlayerSyncEvents::onStartTracking);
        MinecraftForge.EVENT_BUS.addListener(PlayerSyncEvents::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(ReloadBoostHandler::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(MinerChargePacket::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(NoGeonEconomyLand::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(NoGeonEconomyLand::onDatapackSync);
        MinecraftForge.EVENT_BUS.addListener(com.nogeon.economyland.entity.ScrapDroneEntity::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(com.nogeon.economyland.entity.ScrapDroneEntity::onLivingHurt);
        MinecraftForge.EVENT_BUS.register(com.nogeon.economyland.player.CorpseCompatListener.class);
        MinecraftForge.EVENT_BUS.register(com.nogeon.economyland.player.FrostwardRingCompatListener.class);
        FtbChunksIntegration.init();
        LOGGER.info("NoGeon Economy Land initialized for Forge");
    }

    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        net.minecraft.server.MinecraftServer server = event.getServer();
        if (server != null) {
            prepopulateRecipeCaches(server);
        }
    }

    public static void onDatapackSync(net.minecraftforge.event.OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        net.minecraft.server.MinecraftServer server = event.getPlayerList() != null ? event.getPlayerList().getServer() : null;
        if (server != null) {
            recipeCachesPrimed = false;
            prepopulateRecipeCaches(server);
        }
    }

    private static void prepopulateRecipeCaches(net.minecraft.server.MinecraftServer server) {
        if (recipeCachesPrimed) {
            return;
        }
        try {
            LOGGER.info("Pre-populating recipe ingredient caches on main thread to prevent Netty thread HashMap corruption...");
            int count = 0;
            for (net.minecraft.world.item.crafting.Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
                for (net.minecraft.world.item.crafting.Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient != null) {
                        ingredient.getItems(); // Triggers caching of matching item stacks on main thread
                        count++;
                    }
                }
            }
            LOGGER.info("Successfully pre-populated {} recipe ingredients.", count);
            recipeCachesPrimed = true;
        } catch (Exception e) {
            LOGGER.error("Failed to pre-populate recipe ingredients", e);
        }
    }
}
