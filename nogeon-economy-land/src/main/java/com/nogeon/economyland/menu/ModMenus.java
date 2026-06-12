package com.nogeon.economyland.menu;

import com.nogeon.economyland.NoGeonEconomyLand;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, NoGeonEconomyLand.MOD_ID);

    public static final RegistryObject<MenuType<WalletMenu>> WALLET =
        MENUS.register("wallet", () -> IForgeMenuType.create(WalletMenu::new));
    public static final RegistryObject<MenuType<JobChangeMenu>> JOB_CHANGE =
        MENUS.register("job_change", () -> IForgeMenuType.create(JobChangeMenu::new));

    public static final RegistryObject<MenuType<HelpMenu>> HELP =
        MENUS.register("help", () -> IForgeMenuType.create(HelpMenu::new));

    public static final RegistryObject<MenuType<SkillsMenu>> SKILLS =
        MENUS.register("skills", () -> IForgeMenuType.create(SkillsMenu::new));

    public static final RegistryObject<MenuType<LandHomeMenu>> LAND_HOME =
        MENUS.register("land_home", () -> IForgeMenuType.create(LandHomeMenu::new));

    public static final RegistryObject<MenuType<AdminLandMenu>> ADMIN_LAND =
        MENUS.register("admin_land", () -> IForgeMenuType.create(AdminLandMenu::new));

    public static final RegistryObject<MenuType<LandClaimMenu>> LAND_CLAIM =
        MENUS.register("land_claim", () -> IForgeMenuType.create(LandClaimMenu::new));

    public static final RegistryObject<MenuType<ShopMenu>> SHOP =
        MENUS.register("shop", () -> IForgeMenuType.create(ShopMenu::new));

    public static final RegistryObject<MenuType<TraderActionMenu>> TRADER_ACTION =
        MENUS.register("trader_action", () -> IForgeMenuType.create(TraderActionMenu::new));

    public static final RegistryObject<MenuType<LuckExchangeMenu>> LUCK_EXCHANGE =
        MENUS.register("luck_exchange", () -> IForgeMenuType.create(LuckExchangeMenu::new));

    public static final RegistryObject<MenuType<GachaMenu>> GACHA =
        MENUS.register("gacha", () -> IForgeMenuType.create(GachaMenu::new));

    public static final RegistryObject<MenuType<GachaStorageMenu>> GACHA_STORAGE =
        MENUS.register("gacha_storage", () -> IForgeMenuType.create(GachaStorageMenu::new));

    public static final RegistryObject<MenuType<GachaRewardAdminMenu>> GACHA_REWARD_ADMIN =
        MENUS.register("gacha_reward_admin", () -> IForgeMenuType.create(GachaRewardAdminMenu::new));

    public static final RegistryObject<MenuType<AuctionMenu>> AUCTION =
        MENUS.register("auction", () -> IForgeMenuType.create(AuctionMenu::new));

    public static final RegistryObject<MenuType<AdminShopMenu>> ADMIN_SHOP =
        MENUS.register("admin_shop", () -> IForgeMenuType.create(AdminShopMenu::new));

    public static final RegistryObject<MenuType<AdminActionMenu>> ADMIN_ACTION =
        MENUS.register("admin_action", () -> IForgeMenuType.create(AdminActionMenu::new));
    public static final RegistryObject<MenuType<AdminCommandMenu>> ADMIN_COMMAND =
        MENUS.register("admin_command", () -> IForgeMenuType.create(AdminCommandMenu::new));

    public static final RegistryObject<MenuType<TradeBrowserMenu>> TRADE_BROWSER =
        MENUS.register("trade_browser", () -> IForgeMenuType.create(TradeBrowserMenu::new));

    public static final RegistryObject<MenuType<TradeRequestMenu>> TRADE_REQUEST =
        MENUS.register("trade_request", () -> IForgeMenuType.create(TradeRequestMenu::new));

    public static final RegistryObject<MenuType<TradeMenu>> TRADE =
        MENUS.register("trade", () -> IForgeMenuType.create(TradeMenu::new));

    public static final RegistryObject<MenuType<TradeItemMenu>> TRADE_ITEM =
        MENUS.register("trade_item", () -> IForgeMenuType.create(TradeItemMenu::new));

    public static final RegistryObject<MenuType<HighLowMenu>> HIGH_LOW =
        MENUS.register("high_low", () -> IForgeMenuType.create(HighLowMenu::new));

    public static final RegistryObject<MenuType<DiceDuelMenu>> DICE_DUEL =
        MENUS.register("dice_duel", () -> IForgeMenuType.create(DiceDuelMenu::new));

    public static final RegistryObject<MenuType<SlotMachineMenu>> SLOT_MACHINE =
        MENUS.register("slot_machine", () -> IForgeMenuType.create(SlotMachineMenu::new));

    public static final RegistryObject<MenuType<SmithMenu>> SMITH =
        MENUS.register("smith", () -> IForgeMenuType.create(SmithMenu::new));

    public static final RegistryObject<MenuType<DeconstructMenu>> DECONSTRUCT =
        MENUS.register("deconstruct", () -> IForgeMenuType.create(DeconstructMenu::new));

    public static final RegistryObject<MenuType<DroneStorageMenu>> DRONE_STORAGE =
        MENUS.register("drone_storage", () -> IForgeMenuType.create(DroneStorageMenu::new));

    public static final RegistryObject<MenuType<EnhancementScrollMenu>> ENHANCEMENT_SCROLL =
        MENUS.register("enhancement_scroll", () -> IForgeMenuType.create(EnhancementScrollMenu::new));

    public static final RegistryObject<MenuType<ReforgeMenu>> REFORGE =
        MENUS.register("reforge", () -> IForgeMenuType.create(ReforgeMenu::new));

    public static final RegistryObject<MenuType<SocketUpgradeMenu>> SOCKET_UPGRADE =
        MENUS.register("socket_upgrade", () -> IForgeMenuType.create(SocketUpgradeMenu::new));

    public static final RegistryObject<MenuType<ExtendedInventoryMenu>> EXTENDED_INVENTORY =
        MENUS.register("extended_inventory", () -> IForgeMenuType.create(ExtendedInventoryMenu::new));
    public static final RegistryObject<MenuType<CosmeticArmorMenu>> COSMETIC_ARMOR =
        MENUS.register("cosmetic_armor", () -> IForgeMenuType.create(CosmeticArmorMenu::new));
    public static final RegistryObject<MenuType<PortalMenu>> PORTAL =
        MENUS.register("portal", () -> IForgeMenuType.create(PortalMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
