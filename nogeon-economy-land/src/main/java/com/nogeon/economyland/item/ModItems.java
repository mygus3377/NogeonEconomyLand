package com.nogeon.economyland.item;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.land.LandType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, NoGeonEconomyLand.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(
            net.minecraft.resources.ResourceKey.createRegistryKey(new net.minecraft.resources.ResourceLocation("minecraft", "creative_mode_tab")),
            NoGeonEconomyLand.MOD_ID
        );

    public static final RegistryObject<Item> ECONOMY_LEDGER =
        ITEMS.register("economy_ledger", () -> new WalletItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GENERAL_TRADER_SPAWNER =
        ITEMS.register("general_trader_spawner", () -> new TraderSpawnerItem(TraderKind.GENERAL, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CROP_TRADER_SPAWNER =
        ITEMS.register("crop_trader_spawner", () -> new TraderSpawnerItem(TraderKind.CROP, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> FISHER_TRADER_SPAWNER =
        ITEMS.register("fisher_trader_spawner", () -> new TraderSpawnerItem(TraderKind.FISHER, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> MINER_TRADER_SPAWNER =
        ITEMS.register("miner_trader_spawner", () -> new TraderSpawnerItem(TraderKind.MINER, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CHEF_TRADER_SPAWNER =
        ITEMS.register("chef_trader_spawner", () -> new TraderSpawnerItem(TraderKind.CHEF, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> LOTTERY_TRADER_SPAWNER =
        ITEMS.register("lottery_trader_spawner", () -> new TraderSpawnerItem(TraderKind.LOTTERY, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> GAMBLER_TRADER_SPAWNER =
        ITEMS.register("gambler_trader_spawner", () -> new TraderSpawnerItem(TraderKind.GAMBLER, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> GACHA_TRADER_SPAWNER =
        ITEMS.register("gacha_trader_spawner", () -> new TraderSpawnerItem(TraderKind.GACHA, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> POTION_TRADER_SPAWNER =
        ITEMS.register("potion_trader_spawner", () -> new TraderSpawnerItem(TraderKind.POTION, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SMITH_TRADER_SPAWNER =
        ITEMS.register("smith_trader_spawner", () -> new TraderSpawnerItem(TraderKind.SMITH, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> ENGINEER_TRADER_SPAWNER =
        ITEMS.register("engineer_trader_spawner", () -> new TraderSpawnerItem(TraderKind.ENGINEER, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> GUN_TRADER_SPAWNER =
        ITEMS.register("gun_trader_spawner", () -> new TraderSpawnerItem(TraderKind.GUN, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> LAND_TRADER_SPAWNER =
        ITEMS.register("land_trader_spawner", () -> new TraderSpawnerItem(TraderKind.LAND, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> AUCTION_TRADER_SPAWNER =
        ITEMS.register("auction_trader_spawner", () -> new TraderSpawnerItem(TraderKind.AUCTION, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> HUNTER_TRADER_SPAWNER =
        ITEMS.register("hunter_trader_spawner", () -> new TraderSpawnerItem(TraderKind.HUNTER, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SHADY_WIZARD_SPAWNER =
        ITEMS.register("shady_wizard_spawner", () -> new ShadyWizardSpawnerItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> FARMER_SCARECROW =
        ITEMS.register("farmer_scarecrow", () -> new FarmerScarecrowItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> BASIC_LAND_DEED =
        ITEMS.register("basic_land_deed", () -> new LandDeedItem(LandType.BASIC, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> NORMAL_LAND_DEED =
        ITEMS.register("normal_land_deed", () -> new LandDeedItem(LandType.NORMAL, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> INDUSTRIAL_LAND_DEED =
        ITEMS.register("industrial_land_deed", () -> new LandDeedItem(LandType.INDUSTRIAL, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> ADMIN_LAND_DEED =
        ITEMS.register("admin_land_deed", () -> new LandDeedItem(LandType.ADMIN, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BASIC_GACHA_TICKET =
        ITEMS.register("basic_gacha_ticket", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> MIDDLE_GACHA_TICKET =
        ITEMS.register("middle_gacha_ticket", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> HIGH_GACHA_TICKET =
        ITEMS.register("high_gacha_ticket", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> LEGEND_GACHA_TICKET =
        ITEMS.register("legend_gacha_ticket", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> UNLUCKY_TOKEN =
        ITEMS.register("unlucky_token", () -> new Item(new Item.Properties().stacksTo(64)) {
            @Override
            public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                tooltip.add(Component.translatable("item.nogeon_economy_land.unlucky_token.tooltip1"));
                tooltip.add(Component.translatable("item.nogeon_economy_land.unlucky_token.tooltip2"));
                super.appendHoverText(stack, level, tooltip, flag);
            }
        });

    public static final RegistryObject<Item> ENHANCEMENT_GUARD_SCROLL =
        ITEMS.register("enhancement_guard_scroll", () -> new EnhancementGuardScrollItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> LOWEST_ENHANCEMENT_DOWNGRADE_SCROLL =
        ITEMS.register("lowest_enhancement_downgrade_scroll", () -> new EnhancementDowngradeProtectionScrollItem("최하급 강화 하락 방지권", 1, 5, ChatFormatting.WHITE, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> LOW_ENHANCEMENT_DOWNGRADE_SCROLL =
        ITEMS.register("low_enhancement_downgrade_scroll", () -> new EnhancementDowngradeProtectionScrollItem("하급 강화 하락 방지권", 6, 10, ChatFormatting.BLUE, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> MID_ENHANCEMENT_DOWNGRADE_SCROLL =
        ITEMS.register("mid_enhancement_downgrade_scroll", () -> new EnhancementDowngradeProtectionScrollItem("중급 강화 하락 방지권", 11, 15, ChatFormatting.BLUE, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> HIGH_ENHANCEMENT_DOWNGRADE_SCROLL =
        ITEMS.register("high_enhancement_downgrade_scroll", () -> new EnhancementDowngradeProtectionScrollItem("상급 강화 하락 방지권", 16, 17, ChatFormatting.GOLD, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> HIGHEST_ENHANCEMENT_DOWNGRADE_SCROLL =
        ITEMS.register("highest_enhancement_downgrade_scroll", () -> new EnhancementDowngradeProtectionScrollItem("최상급 강화 하락 방지권", 18, 20, ChatFormatting.GOLD, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_RESET_PROTECTION_SCROLL =
        ITEMS.register("enhancement_reset_protection_scroll", () -> new EnhancementResetProtectionScrollItem("강화 초기화 방지권", new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_BOOST_SCROLL =
        ITEMS.register("enhancement_boost_scroll", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_1 =
        ITEMS.register("enhancement_scroll_1", () -> new EnhancementScrollItem(1, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_2 =
        ITEMS.register("enhancement_scroll_2", () -> new EnhancementScrollItem(2, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_3 =
        ITEMS.register("enhancement_scroll_3", () -> new EnhancementScrollItem(3, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_4 =
        ITEMS.register("enhancement_scroll_4", () -> new EnhancementScrollItem(4, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_5 =
        ITEMS.register("enhancement_scroll_5", () -> new EnhancementScrollItem(5, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_6 =
        ITEMS.register("enhancement_scroll_6", () -> new EnhancementScrollItem(6, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_7 =
        ITEMS.register("enhancement_scroll_7", () -> new EnhancementScrollItem(7, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_8 =
        ITEMS.register("enhancement_scroll_8", () -> new EnhancementScrollItem(8, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_9 =
        ITEMS.register("enhancement_scroll_9", () -> new EnhancementScrollItem(9, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_10 =
        ITEMS.register("enhancement_scroll_10", () -> new EnhancementScrollItem(10, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_11 =
        ITEMS.register("enhancement_scroll_11", () -> new EnhancementScrollItem(11, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_12 =
        ITEMS.register("enhancement_scroll_12", () -> new EnhancementScrollItem(12, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_13 =
        ITEMS.register("enhancement_scroll_13", () -> new EnhancementScrollItem(13, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_14 =
        ITEMS.register("enhancement_scroll_14", () -> new EnhancementScrollItem(14, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_SCROLL_15 =
        ITEMS.register("enhancement_scroll_15", () -> new EnhancementScrollItem(15, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> INVENTORY_KEEP_SCROLL =
        ITEMS.register("inventory_keep_scroll", () -> new InventoryKeepScrollItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> CRACKED_ENHANCEMENT_GEM =
        ITEMS.register("cracked_enhancement_gem", () -> new EnhancementGemItem(1, 5, ChatFormatting.WHITE, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> SPLIT_ENHANCEMENT_GEM =
        ITEMS.register("split_enhancement_gem", () -> new EnhancementGemItem(2, 10, ChatFormatting.GREEN, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> FLAWED_ENHANCEMENT_GEM =
        ITEMS.register("flawed_enhancement_gem", () -> new EnhancementGemItem(3, 15, ChatFormatting.BLUE, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> ENHANCEMENT_GEM =
        ITEMS.register("enhancement_gem", () -> new EnhancementGemItem(4, 25, ChatFormatting.LIGHT_PURPLE, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> FLAWLESS_ENHANCEMENT_GEM =
        ITEMS.register("flawless_enhancement_gem", () -> new EnhancementGemItem(5, 40, ChatFormatting.GOLD, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> PERFECT_ENHANCEMENT_GEM =
        ITEMS.register("perfect_enhancement_gem", () -> new EnhancementGemItem(6, 100, ChatFormatting.AQUA, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEEPSEA_CRATE_WOOD =
        ITEMS.register("deepsea_crate_wood", () -> new DeepseaCrateItem(1, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEEPSEA_CRATE_STONE =
        ITEMS.register("deepsea_crate_stone", () -> new DeepseaCrateItem(2, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEEPSEA_CRATE_IRON =
        ITEMS.register("deepsea_crate_iron", () -> new DeepseaCrateItem(3, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEEPSEA_CRATE_DIAMOND =
        ITEMS.register("deepsea_crate_diamond", () -> new DeepseaCrateItem(4, new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> PORTAL_SCROLL =
        ITEMS.register("portal_scroll", () -> new PortalScrollItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<CreativeModeTab> MAIN_TAB =
        CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.nogeon_economy_land.main"))
            .icon(() -> ECONOMY_LEDGER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ECONOMY_LEDGER.get());
                output.accept(GENERAL_TRADER_SPAWNER.get());
                output.accept(CROP_TRADER_SPAWNER.get());
                output.accept(FISHER_TRADER_SPAWNER.get());
                output.accept(MINER_TRADER_SPAWNER.get());
                output.accept(CHEF_TRADER_SPAWNER.get());
                output.accept(LOTTERY_TRADER_SPAWNER.get());
                output.accept(GAMBLER_TRADER_SPAWNER.get());
                output.accept(GACHA_TRADER_SPAWNER.get());
                output.accept(POTION_TRADER_SPAWNER.get());
                output.accept(SMITH_TRADER_SPAWNER.get());
                output.accept(ENGINEER_TRADER_SPAWNER.get());
                output.accept(GUN_TRADER_SPAWNER.get());
                output.accept(LAND_TRADER_SPAWNER.get());
                output.accept(AUCTION_TRADER_SPAWNER.get());
                output.accept(HUNTER_TRADER_SPAWNER.get());
                output.accept(SHADY_WIZARD_SPAWNER.get());
                output.accept(FARMER_SCARECROW.get());
                output.accept(BASIC_LAND_DEED.get());
                output.accept(NORMAL_LAND_DEED.get());
                output.accept(INDUSTRIAL_LAND_DEED.get());
                output.accept(ADMIN_LAND_DEED.get());
                output.accept(BASIC_GACHA_TICKET.get());
                output.accept(MIDDLE_GACHA_TICKET.get());
                output.accept(HIGH_GACHA_TICKET.get());
                output.accept(LEGEND_GACHA_TICKET.get());
                output.accept(UNLUCKY_TOKEN.get());
                output.accept(LOW_ENHANCEMENT_DOWNGRADE_SCROLL.get());
                output.accept(MID_ENHANCEMENT_DOWNGRADE_SCROLL.get());
                output.accept(HIGH_ENHANCEMENT_DOWNGRADE_SCROLL.get());
                output.accept(HIGHEST_ENHANCEMENT_DOWNGRADE_SCROLL.get());
                output.accept(ENHANCEMENT_RESET_PROTECTION_SCROLL.get());
                output.accept(ENHANCEMENT_BOOST_SCROLL.get());
                output.accept(ENHANCEMENT_SCROLL_1.get());
                output.accept(ENHANCEMENT_SCROLL_2.get());
                output.accept(ENHANCEMENT_SCROLL_3.get());
                output.accept(ENHANCEMENT_SCROLL_4.get());
                output.accept(ENHANCEMENT_SCROLL_5.get());
                output.accept(ENHANCEMENT_SCROLL_6.get());
                output.accept(ENHANCEMENT_SCROLL_7.get());
                output.accept(ENHANCEMENT_SCROLL_8.get());
                output.accept(ENHANCEMENT_SCROLL_9.get());
                output.accept(ENHANCEMENT_SCROLL_10.get());
                output.accept(ENHANCEMENT_SCROLL_11.get());
                output.accept(ENHANCEMENT_SCROLL_12.get());
                output.accept(ENHANCEMENT_SCROLL_13.get());
                output.accept(ENHANCEMENT_SCROLL_14.get());
                output.accept(ENHANCEMENT_SCROLL_15.get());
                output.accept(INVENTORY_KEEP_SCROLL.get());
                output.accept(CRACKED_ENHANCEMENT_GEM.get());
                output.accept(SPLIT_ENHANCEMENT_GEM.get());
                output.accept(FLAWED_ENHANCEMENT_GEM.get());
                output.accept(ENHANCEMENT_GEM.get());
                output.accept(FLAWLESS_ENHANCEMENT_GEM.get());
                output.accept(PERFECT_ENHANCEMENT_GEM.get());
                output.accept(DEEPSEA_CRATE_WOOD.get());
                output.accept(DEEPSEA_CRATE_STONE.get());
                output.accept(DEEPSEA_CRATE_IRON.get());
                output.accept(DEEPSEA_CRATE_DIAMOND.get());
                output.accept(PORTAL_SCROLL.get());
            })
            .build());

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
