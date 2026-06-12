package com.nogeon.economyland.entity;

import com.nogeon.economyland.NoGeonEconomyLand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NoGeonEconomyLand.MOD_ID);

    public static final RegistryObject<EntityType<EconomyTraderEntity>> ECONOMY_TRADER =
        ENTITIES.register("economy_trader", () -> EntityType.Builder
            .of(EconomyTraderEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("economy_trader"));

    public static final RegistryObject<EntityType<FarmerScarecrowEntity>> FARMER_SCARECROW =
        ENTITIES.register("farmer_scarecrow", () -> EntityType.Builder
            .of(FarmerScarecrowEntity::new, MobCategory.MISC)
            .sized(0.7F, 1.8F)
            .clientTrackingRange(8)
            .build("farmer_scarecrow"));

    public static final RegistryObject<EntityType<ScrapDroneEntity>> SCRAP_DRONE =
        ENTITIES.register("scrap_drone", () -> EntityType.Builder
            .of(ScrapDroneEntity::new, MobCategory.MISC)
            .sized(0.4F, 0.4F)
            .clientTrackingRange(8)
            .build("scrap_drone"));

    public static final RegistryObject<EntityType<PortalEntity>> PORTAL_ENTITY =
        ENTITIES.register("portal_entity", () -> EntityType.Builder
            .of(PortalEntity::new, MobCategory.MISC)
            .sized(1.2F, 2.0F)
            .clientTrackingRange(10)
            .build("portal_entity"));

    public static final RegistryObject<EntityType<DroneWitherSkull>> DRONE_WITHER_SKULL =
        ENTITIES.register("drone_wither_skull", () -> EntityType.Builder
            .<DroneWitherSkull>of(DroneWitherSkull::new, MobCategory.MISC)
            .sized(0.3125F, 0.3125F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build("drone_wither_skull"));

    private ModEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ECONOMY_TRADER.get(), EconomyTraderEntity.attributes().build());
        event.put(FARMER_SCARECROW.get(), FarmerScarecrowEntity.attributes().build());
        event.put(SCRAP_DRONE.get(), ScrapDroneEntity.attributes().build());
    }
}
