package com.nogeon.economyland.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.ModEntities;
import com.nogeon.economyland.menu.ModMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = NoGeonEconomyLand.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final ResourceLocation MINER_EYE_XRAY_MODEL = new ResourceLocation(NoGeonEconomyLand.MOD_ID, "block/miner_eye_xray_cube");

    public static final KeyMapping OPEN_ECONOMY_KEY = new KeyMapping(
        "key.nogeon_economy_land.open_economy",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.categories.nogeon_economy_land"
    );

    public static final KeyMapping JOB_ABILITY_PRIMARY_KEY = new KeyMapping(
        "key.nogeon_economy_land.job_ability_primary",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.MOUSE,
        GLFW.GLFW_MOUSE_BUTTON_5,
        "key.categories.nogeon_economy_land"
    );

    public static final KeyMapping JOB_ABILITY_SECONDARY_KEY = new KeyMapping(
        "key.nogeon_economy_land.job_ability_secondary",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.MOUSE,
        GLFW.GLFW_MOUSE_BUTTON_4,
        "key.categories.nogeon_economy_land"
    );

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientConfig.load();
            MenuScreens.register(ModMenus.WALLET.get(), WalletScreen::new);
            MenuScreens.register(ModMenus.ADMIN_COMMAND.get(), AdminCommandScreen::new);
            MenuScreens.register(ModMenus.JOB_CHANGE.get(), JobChangeScreen::new);
            MenuScreens.register(ModMenus.HELP.get(), HelpScreen::new);
            MenuScreens.register(ModMenus.SKILLS.get(), SkillsScreen::new);
            MenuScreens.register(ModMenus.LAND_HOME.get(), LandHomeScreen::new);
            MenuScreens.register(ModMenus.ADMIN_LAND.get(), AdminLandScreen::new);
            MenuScreens.register(ModMenus.LAND_CLAIM.get(), LandClaimScreen::new);
            MenuScreens.register(ModMenus.SHOP.get(), ShopScreen::new);
            MenuScreens.register(ModMenus.TRADER_ACTION.get(), TraderActionScreen::new);
            MenuScreens.register(ModMenus.LUCK_EXCHANGE.get(), LuckExchangeScreen::new);
            MenuScreens.register(ModMenus.GACHA.get(), GachaScreen::new);
            MenuScreens.register(ModMenus.GACHA_STORAGE.get(), GachaStorageScreen::new);
            MenuScreens.register(ModMenus.GACHA_REWARD_ADMIN.get(), GachaRewardAdminScreen::new);
            MenuScreens.register(ModMenus.AUCTION.get(), AuctionScreen::new);
            MenuScreens.register(ModMenus.ADMIN_SHOP.get(), AdminShopScreen::new);
            MenuScreens.register(ModMenus.ADMIN_ACTION.get(), AdminActionScreen::new);
            MenuScreens.register(ModMenus.TRADE_BROWSER.get(), TradeBrowserScreen::new);
            MenuScreens.register(ModMenus.TRADE_REQUEST.get(), TradeRequestScreen::new);
            MenuScreens.register(ModMenus.TRADE.get(), TradeScreen::new);
            MenuScreens.register(ModMenus.TRADE_ITEM.get(), TradeItemScreen::new);
            MenuScreens.register(ModMenus.HIGH_LOW.get(), HighLowScreen::new);
            MenuScreens.register(ModMenus.DICE_DUEL.get(), DiceDuelScreen::new);
            MenuScreens.register(ModMenus.SLOT_MACHINE.get(), SlotMachineScreen::new);
            MenuScreens.register(ModMenus.SMITH.get(), SmithScreen::new);
            MenuScreens.register(ModMenus.DECONSTRUCT.get(), DeconstructScreen::new);
            MenuScreens.register(ModMenus.DRONE_STORAGE.get(), DroneStorageScreen::new);
            MenuScreens.register(ModMenus.ENHANCEMENT_SCROLL.get(), EnhancementScrollScreen::new);
            MenuScreens.register(ModMenus.REFORGE.get(), ReforgeScreen::new);
            MenuScreens.register(ModMenus.SOCKET_UPGRADE.get(), SocketUpgradeScreen::new);
            MenuScreens.register(ModMenus.EXTENDED_INVENTORY.get(), ExtendedInventoryScreen::new);
            MenuScreens.register(ModMenus.COSMETIC_ARMOR.get(), CosmeticArmorScreen::new);
            MenuScreens.register(ModMenus.PORTAL.get(), PortalScreen::new);
            EntityRenderers.register(ModEntities.ECONOMY_TRADER.get(), EconomyTraderRenderer::new);
            EntityRenderers.register(ModEntities.FARMER_SCARECROW.get(), FarmerScarecrowRenderer::new);
            EntityRenderers.register(ModEntities.SCRAP_DRONE.get(), ScrapDroneRenderer::new);
            EntityRenderers.register(ModEntities.PORTAL_ENTITY.get(), PortalRenderer::new);
            EntityRenderers.register(ModEntities.DRONE_WITHER_SKULL.get(), DroneWitherSkullRenderer::new);
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FarmerScarecrowModel.LAYER, FarmerScarecrowModel::createBodyLayer);
        event.registerLayerDefinition(ScrapDroneModel.LAYER, ScrapDroneModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ECONOMY_KEY);
        event.register(JOB_ABILITY_PRIMARY_KEY);
        event.register(JOB_ABILITY_SECONDARY_KEY);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MINER_EYE_XRAY_MODEL);
    }
}
