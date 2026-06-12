package com.nogeon.economyland.network;

import com.nogeon.economyland.item.SmithingService;
import com.nogeon.economyland.menu.DeconstructOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.entity.ScrapDroneEntity;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class DeconstructActionPacket {
    private final int slot;
    private final int actionType;
    private final String extraText;
    private final int uiTab;

    public DeconstructActionPacket(int slot) {
        this(slot, 0, "", 0);
    }

    public DeconstructActionPacket(int slot, int actionType) {
        this(slot, actionType, "", 0);
    }

    public DeconstructActionPacket(int slot, int actionType, String extraText) {
        this(slot, actionType, extraText, 0);
    }

    public DeconstructActionPacket(int slot, int actionType, String extraText, int uiTab) {
        this.slot = slot;
        this.actionType = actionType;
        this.extraText = extraText;
        this.uiTab = uiTab;
    }

    public static void encode(DeconstructActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
        buffer.writeVarInt(packet.actionType);
        buffer.writeUtf(packet.extraText);
        buffer.writeVarInt(packet.uiTab);
    }

    public static DeconstructActionPacket decode(FriendlyByteBuf buffer) {
        return new DeconstructActionPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(), buffer.readVarInt());
    }

    public static void handle(DeconstructActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            
            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            int selectedSlot = SmithingService.normalizeSelectedSlot(sender, packet.slot);
            ItemStack stack = sender.getInventory().getItem(selectedSlot);
            Component status = Component.literal("");

            // Find spawned ScrapDroneEntity
            ScrapDroneEntity drone = null;
            AABB searchBox = sender.getBoundingBox().inflate(32.0D);
            List<ScrapDroneEntity> drones = sender.level().getEntitiesOfClass(
                ScrapDroneEntity.class, searchBox,
                d -> d.getOwnerUuid().map(uuid -> uuid.equals(sender.getUUID())).orElse(false)
            );
            if (!drones.isEmpty()) {
                drone = drones.get(0);
            }

            if (packet.actionType == 0) {
                // 1. Deconstruct item
                status = SmithingService.tryDeconstruct(sender, profile, selectedSlot);
            } else if (packet.actionType == 1) {
                // 2. Burn fuel
                if (drone == null) {
                    status = Component.literal("§c주변에 소환된 오토 스크랩 드론이 없습니다.");
                } else if (stack.isEmpty()) {
                    status = Component.literal("§c발전에 사용할 아이템을 선택하세요.");
                } else {
                    String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    double val = drone.getFuelPowerValue(itemId);
                    stack.shrink(1);
                    drone.addCharge(val);
                    
                    sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                        SoundEvents.COPPER_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
                    status = Component.literal("§a[동력 주입] §e+" + val + "% §a동력 충전 완료! (현재: §e" + drone.getCharge() + "%§a)");
                }
            } else if (packet.actionType == 2) {
                // 3. Register auto fuel
                if (stack.isEmpty()) {
                    status = Component.literal("§c등록할 아이템을 선택하세요.");
                } else {
                    String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    sender.getPersistentData().putString("nogeon_engineer_drone_autofuel_item", itemId);
                    status = Component.literal("§a[연료 등록] §e" + stack.getHoverName().getString() + "§a을(를) 자동 소모 연료로 등록했습니다.");
                }
            } else if (packet.actionType == 3) {
                // 4. Clear auto fuel
                sender.getPersistentData().putString("nogeon_engineer_drone_autofuel_item", "");
                status = Component.literal("§e[연료 해제] 자동 연료를 등록 해제했습니다.");
            } else if (packet.actionType == 4) {
                // 5. Repair Drone
                boolean upgInv = sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_inventory");
                boolean upgTrans = sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_transmitter");
                boolean upgBoost = sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_booster");

                int reqIron = 5 + (upgInv ? 5 : 0);
                int reqCopper = 0 + (upgInv ? 2 : 0) + (upgBoost ? 5 : 0);
                int reqGold = 0 + (upgTrans ? 1 : 0);
                int reqRedstone = 0 + (upgTrans ? 1 : 0);
                int reqPiston = 0 + (upgBoost ? 1 : 0);
                int reqCog = 1;

                Item ironItem = Items.IRON_INGOT;
                Item copperItem = Items.COPPER_INGOT;
                Item goldItem = Items.GOLD_INGOT;
                Item redstoneItem = Items.REDSTONE;
                Item pistonItem = Items.PISTON;
                Item cogItem = BuiltInRegistries.ITEM.get(new ResourceLocation("create:cogwheel"));
                if (cogItem == Items.AIR) cogItem = Items.COPPER_INGOT;

                if (countItem(sender, ironItem) < reqIron ||
                    countItem(sender, copperItem) < reqCopper ||
                    countItem(sender, goldItem) < reqGold ||
                    countItem(sender, redstoneItem) < reqRedstone ||
                    countItem(sender, pistonItem) < reqPiston ||
                    countItem(sender, cogItem) < reqCog) {
                    status = Component.literal("§c[수리 실패] 수리에 필요한 재료가 부족합니다.");
                } else {
                    consumeItem(sender, ironItem, reqIron);
                    if (reqCopper > 0) consumeItem(sender, copperItem, reqCopper);
                    if (reqGold > 0) consumeItem(sender, goldItem, reqGold);
                    if (reqRedstone > 0) consumeItem(sender, redstoneItem, reqRedstone);
                    if (reqPiston > 0) consumeItem(sender, pistonItem, reqPiston);
                    consumeItem(sender, cogItem, reqCog);

                    sender.getPersistentData().putBoolean("nogeon_engineer_drone_broken", false);
                    sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
                    status = Component.literal("§a[수리 완료] 드론 복구가 완료되었습니다!");
                }
            } else if (packet.actionType == 5) {
                // 6. Unlock & Upgrade Inventory
                int curLvl = sender.getPersistentData().getInt("nogeon_engineer_drone_upgrade_inventory_level");
                if (curLvl <= 0 && sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_inventory")) {
                    curLvl = 1;
                    sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_inventory_level", 1);
                }
                
                if (curLvl >= 5) {
                    status = Component.literal("§c보관함이 이미 최대 레벨(5)입니다.");
                } else {
                    int reqIron = 0, reqCopper = 0, reqGold = 0, reqPiston = 0, reqDia = 0;
                    int reqCog = 0, reqBrass = 0, reqTube = 0, reqMech = 0, reqSturdy = 0;
                    
                    if (curLvl == 0) {
                        reqIron = 64; reqCopper = 32; reqCog = 10;
                    } else if (curLvl == 1) {
                        reqIron = 128; reqCopper = 64; reqCog = 20; reqBrass = 5;
                    } else if (curLvl == 2) {
                        reqIron = 256; reqCopper = 128; reqCog = 30; reqBrass = 10; reqTube = 5;
                    } else if (curLvl == 3) {
                        reqIron = 512; reqGold = 20; reqDia = 10; reqTube = 10; reqMech = 2;
                    } else if (curLvl == 4) {
                        reqIron = 1024; reqDia = 20; reqMech = 10; reqSturdy = 5;
                    }
                    
                    Item ironItem = Items.IRON_INGOT;
                    Item copperItem = Items.COPPER_INGOT;
                    Item goldItem = Items.GOLD_INGOT;
                    Item pistonItem = Items.PISTON;
                    Item diaItem = Items.DIAMOND;
                    Item cogItem = getCogwheel();
                    Item brassItem = getBrassIngot();
                    Item tubeItem = getElectronTube();
                    Item mechItem = getPrecisionMechanism();
                    Item sturdyItem = getSturdySheet();
                    
                    if (countItem(sender, ironItem) < reqIron || countItem(sender, copperItem) < reqCopper ||
                        countItem(sender, goldItem) < reqGold || countItem(sender, diaItem) < reqDia ||
                        countItem(sender, cogItem) < reqCog || countItem(sender, brassItem) < reqBrass ||
                        countItem(sender, tubeItem) < reqTube || countItem(sender, mechItem) < reqMech ||
                        countItem(sender, sturdyItem) < reqSturdy) {
                        status = Component.literal("§c[업그레이드 실패] 보관함 강화 재료가 부족합니다.");
                    } else {
                        if (reqIron > 0) consumeItem(sender, ironItem, reqIron);
                        if (reqCopper > 0) consumeItem(sender, copperItem, reqCopper);
                        if (reqGold > 0) consumeItem(sender, goldItem, reqGold);
                        if (reqDia > 0) consumeItem(sender, diaItem, reqDia);
                        if (reqCog > 0) consumeItem(sender, cogItem, reqCog);
                        if (reqBrass > 0) consumeItem(sender, brassItem, reqBrass);
                        if (reqTube > 0) consumeItem(sender, tubeItem, reqTube);
                        if (reqMech > 0) consumeItem(sender, mechItem, reqMech);
                        if (reqSturdy > 0) consumeItem(sender, sturdyItem, reqSturdy);
                        
                        int nextLvl = curLvl + 1;
                        sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_inventory_level", nextLvl);
                        sender.getPersistentData().putBoolean("nogeon_engineer_drone_upgrade_inventory", true);
                        
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        if (curLvl == 0) {
                            status = Component.literal("§a[업그레이드] 드론 전용 보관함 1단계(9칸)가 해금되었습니다!");
                        } else {
                            status = Component.literal("§a[업그레이드] 드론 보관함이 " + nextLvl + "단계(" + (nextLvl * 9) + "칸)로 강화되었습니다!");
                        }
                    }
                }
            } else if (packet.actionType == 6) {
                // 7. Unlock & Upgrade Transmitter
                int curLvl = sender.getPersistentData().getInt("nogeon_engineer_drone_upgrade_transmitter_level");
                if (curLvl <= 0 && sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_transmitter")) {
                    curLvl = 1;
                    sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_transmitter_level", 1);
                }
                
                if (curLvl >= 5) {
                    status = Component.literal("§c안테나가 이미 최대 레벨(5)입니다.");
                } else {
                    int reqGold = 0, reqRedstone = 0, reqDia = 0, reqPearl = 0, reqEmer = 0;
                    int reqCog = 0, reqBrass = 0, reqTube = 0, reqMech = 0, reqSturdy = 0;
                    
                    if (curLvl == 0) {
                        reqGold = 32; reqRedstone = 64; reqCog = 10;
                    } else if (curLvl == 1) {
                        reqGold = 64; reqRedstone = 128; reqCog = 20; reqBrass = 10;
                    } else if (curLvl == 2) {
                        reqGold = 128; reqRedstone = 256; reqDia = 5; reqTube = 10;
                    } else if (curLvl == 3) {
                        reqGold = 256; reqRedstone = 512; reqPearl = 10; reqTube = 20; reqMech = 5;
                    } else if (curLvl == 4) {
                        reqGold = 512; reqRedstone = 1024; reqEmer = 20; reqMech = 10; reqSturdy = 5;
                    }
                    
                    Item goldItem = Items.GOLD_INGOT;
                    Item redstoneItem = Items.REDSTONE;
                    Item diaItem = Items.DIAMOND;
                    Item pearlItem = Items.ENDER_PEARL;
                    Item emerItem = Items.EMERALD;
                    Item cogItem = getCogwheel();
                    Item brassItem = getBrassIngot();
                    Item tubeItem = getElectronTube();
                    Item mechItem = getPrecisionMechanism();
                    Item sturdyItem = getSturdySheet();
                    
                    if (countItem(sender, goldItem) < reqGold || countItem(sender, redstoneItem) < reqRedstone ||
                        countItem(sender, diaItem) < reqDia || countItem(sender, pearlItem) < reqPearl ||
                        countItem(sender, emerItem) < reqEmer || countItem(sender, cogItem) < reqCog ||
                        countItem(sender, brassItem) < reqBrass || countItem(sender, tubeItem) < reqTube ||
                        countItem(sender, mechItem) < reqMech || countItem(sender, sturdyItem) < reqSturdy) {
                        status = Component.literal("§c[업그레이드 실패] 안테나 강화 재료가 부족합니다.");
                    } else {
                        if (reqGold > 0) consumeItem(sender, goldItem, reqGold);
                        if (reqRedstone > 0) consumeItem(sender, redstoneItem, reqRedstone);
                        if (reqDia > 0) consumeItem(sender, diaItem, reqDia);
                        if (reqPearl > 0) consumeItem(sender, pearlItem, reqPearl);
                        if (reqEmer > 0) consumeItem(sender, emerItem, reqEmer);
                        if (reqCog > 0) consumeItem(sender, cogItem, reqCog);
                        if (reqBrass > 0) consumeItem(sender, brassItem, reqBrass);
                        if (reqTube > 0) consumeItem(sender, tubeItem, reqTube);
                        if (reqMech > 0) consumeItem(sender, mechItem, reqMech);
                        if (reqSturdy > 0) consumeItem(sender, sturdyItem, reqSturdy);
                        
                        int nextLvl = curLvl + 1;
                        sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_transmitter_level", nextLvl);
                        sender.getPersistentData().putBoolean("nogeon_engineer_drone_upgrade_transmitter", true);
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        status = Component.literal("§a[업그레이드] 안테나가 " + nextLvl + "단계로 강화되었습니다!");
                    }
                }
            } else if (packet.actionType == 7) {
                // 8. Unlock & Upgrade Booster
                int curLvl = sender.getPersistentData().getInt("nogeon_engineer_drone_upgrade_booster_level");
                if (curLvl <= 0 && sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_booster")) {
                    curLvl = 1;
                    sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_booster_level", 1);
                }
                
                if (curLvl >= 5) {
                    status = Component.literal("§c부스터가 이미 최대 레벨(5)입니다.");
                } else {
                    int reqCopper = 0, reqPiston = 0, reqIron = 0, reqDia = 0, reqGold = 0;
                    int reqCog = 0, reqBrass = 0, reqTube = 0, reqMech = 0, reqSturdy = 0;
                    
                    if (curLvl == 0) {
                        reqCopper = 64; reqPiston = 10; reqCog = 10;
                    } else if (curLvl == 1) {
                        reqCopper = 128; reqPiston = 20; reqCog = 20; reqBrass = 10;
                    } else if (curLvl == 2) {
                        reqCopper = 256; reqPiston = 30; reqIron = 100; reqTube = 5;
                    } else if (curLvl == 3) {
                        reqCopper = 512; reqPiston = 40; reqIron = 200; reqTube = 10; reqMech = 5;
                    } else if (curLvl == 4) {
                        reqCopper = 1024; reqPiston = 60; reqDia = 20; reqMech = 10; reqSturdy = 5;
                    }
                    
                    Item copperItem = Items.COPPER_INGOT;
                    Item pistonItem = Items.PISTON;
                    Item ironItem = Items.IRON_INGOT;
                    Item diaItem = Items.DIAMOND;
                    Item goldItem = Items.GOLD_INGOT;
                    Item cogItem = getCogwheel();
                    Item brassItem = getBrassIngot();
                    Item tubeItem = getElectronTube();
                    Item mechItem = getPrecisionMechanism();
                    Item sturdyItem = getSturdySheet();
                    
                    if (countItem(sender, copperItem) < reqCopper || countItem(sender, pistonItem) < reqPiston ||
                        countItem(sender, ironItem) < reqIron || countItem(sender, diaItem) < reqDia ||
                        countItem(sender, goldItem) < reqGold || countItem(sender, cogItem) < reqCog ||
                        countItem(sender, brassItem) < reqBrass || countItem(sender, tubeItem) < reqTube ||
                        countItem(sender, mechItem) < reqMech || countItem(sender, sturdyItem) < reqSturdy) {
                        status = Component.literal("§c[업그레이드 실패] 부스터 강화 재료가 부족합니다.");
                    } else {
                        if (reqCopper > 0) consumeItem(sender, copperItem, reqCopper);
                        if (reqPiston > 0) consumeItem(sender, pistonItem, reqPiston);
                        if (reqIron > 0) consumeItem(sender, ironItem, reqIron);
                        if (reqDia > 0) consumeItem(sender, diaItem, reqDia);
                        if (reqGold > 0) consumeItem(sender, goldItem, reqGold);
                        if (reqCog > 0) consumeItem(sender, cogItem, reqCog);
                        if (reqBrass > 0) consumeItem(sender, brassItem, reqBrass);
                        if (reqTube > 0) consumeItem(sender, tubeItem, reqTube);
                        if (reqMech > 0) consumeItem(sender, mechItem, reqMech);
                        if (reqSturdy > 0) consumeItem(sender, sturdyItem, reqSturdy);
                        
                        int nextLvl = curLvl + 1;
                        sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_booster_level", nextLvl);
                        sender.getPersistentData().putBoolean("nogeon_engineer_drone_upgrade_booster", true);
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        status = Component.literal("§a[업그레이드] 부스터가 " + nextLvl + "단계로 강화되었습니다!");
                    }
                }
            } else if (packet.actionType == 15) {
                // 15. Unlock & Upgrade Sensor
                int curLvl = sender.getPersistentData().getInt("nogeon_engineer_drone_upgrade_sensor_level");
                if (curLvl <= 0 && sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_sensor")) {
                    curLvl = 1;
                    sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_sensor_level", 1);
                }
                
                if (curLvl >= 5) {
                    status = Component.literal("§c센서가 이미 최대 레벨(5)입니다.");
                } else {
                    int reqRedstone = 0, reqCopper = 0, reqLapis = 0, reqGold = 0, reqDia = 0, reqEmer = 0;
                    int reqCog = 0, reqBrass = 0, reqTube = 0, reqMech = 0, reqSturdy = 0;
                    
                    if (curLvl == 0) {
                        reqRedstone = 64; reqCopper = 32; reqCog = 10;
                    } else if (curLvl == 1) {
                        reqRedstone = 128; reqCopper = 64; reqCog = 20; reqBrass = 10;
                    } else if (curLvl == 2) {
                        reqRedstone = 256; reqCopper = 128; reqLapis = 64; reqTube = 5;
                    } else if (curLvl == 3) {
                        reqRedstone = 512; reqCopper = 256; reqLapis = 128; reqTube = 10; reqMech = 5;
                    } else if (curLvl == 4) {
                        reqRedstone = 1024; reqDia = 20; reqEmer = 30; reqMech = 10; reqSturdy = 5;
                    }
                    
                    Item redstoneItem = Items.REDSTONE;
                    Item copperItem = Items.COPPER_INGOT;
                    Item lapisItem = Items.LAPIS_LAZULI;
                    Item goldItem = Items.GOLD_INGOT;
                    Item diaItem = Items.DIAMOND;
                    Item emerItem = Items.EMERALD;
                    Item cogItem = getCogwheel();
                    Item brassItem = getBrassIngot();
                    Item tubeItem = getElectronTube();
                    Item mechItem = getPrecisionMechanism();
                    Item sturdyItem = getSturdySheet();
                    
                    if (countItem(sender, redstoneItem) < reqRedstone || countItem(sender, copperItem) < reqCopper ||
                        countItem(sender, lapisItem) < reqLapis || countItem(sender, goldItem) < reqGold ||
                        countItem(sender, diaItem) < reqDia || countItem(sender, emerItem) < reqEmer ||
                        countItem(sender, cogItem) < reqCog || countItem(sender, brassItem) < reqBrass ||
                        countItem(sender, tubeItem) < reqTube || countItem(sender, mechItem) < reqMech ||
                        countItem(sender, sturdyItem) < reqSturdy) {
                        status = Component.literal("§c[업그레이드 실패] 센서 강화 재료가 부족합니다.");
                    } else {
                        if (reqRedstone > 0) consumeItem(sender, redstoneItem, reqRedstone);
                        if (reqCopper > 0) consumeItem(sender, copperItem, reqCopper);
                        if (reqLapis > 0) consumeItem(sender, lapisItem, reqLapis);
                        if (reqGold > 0) consumeItem(sender, goldItem, reqGold);
                        if (reqDia > 0) consumeItem(sender, diaItem, reqDia);
                        if (reqEmer > 0) consumeItem(sender, emerItem, reqEmer);
                        if (reqCog > 0) consumeItem(sender, cogItem, reqCog);
                        if (reqBrass > 0) consumeItem(sender, brassItem, reqBrass);
                        if (reqTube > 0) consumeItem(sender, tubeItem, reqTube);
                        if (reqMech > 0) consumeItem(sender, mechItem, reqMech);
                        if (reqSturdy > 0) consumeItem(sender, sturdyItem, reqSturdy);
                        
                        int nextLvl = curLvl + 1;
                        sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_sensor_level", nextLvl);
                        sender.getPersistentData().putBoolean("nogeon_engineer_drone_upgrade_sensor", true);
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        status = Component.literal("§a[업그레이드] 센서가 " + nextLvl + "단계로 강화되었습니다!");
                    }
                }
            } else if (packet.actionType == 16) {
                // 16. Unlock & Upgrade Grabber
                int curLvl = sender.getPersistentData().getInt("nogeon_engineer_drone_upgrade_grabber_level");
                if (curLvl <= 0 && sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_grabber")) {
                    curLvl = 1;
                    sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_grabber_level", 1);
                }
                
                if (curLvl >= 5) {
                    status = Component.literal("§c자재 암이 이미 최대 레벨(5)입니다.");
                } else {
                    int reqIron = 0, reqPiston = 0, reqDia = 0, reqEmer = 0;
                    int reqCog = 0, reqBrass = 0, reqTube = 0, reqMech = 0, reqSturdy = 0;
                    
                    if (curLvl == 0) {
                        reqIron = 64; reqPiston = 10; reqCog = 10;
                    } else if (curLvl == 1) {
                        reqIron = 128; reqPiston = 20; reqCog = 20; reqBrass = 10;
                    } else if (curLvl == 2) {
                        reqIron = 256; reqPiston = 30; reqTube = 5;
                    } else if (curLvl == 3) {
                        reqIron = 512; reqPiston = 40; reqTube = 10; reqMech = 5;
                    } else if (curLvl == 4) {
                        reqIron = 1024; reqPiston = 60; reqDia = 10; reqMech = 10; reqSturdy = 5; reqEmer = 30;
                    }
                    
                    Item ironItem = Items.IRON_INGOT;
                    Item pistonItem = Items.PISTON;
                    Item diaItem = Items.DIAMOND;
                    Item emerItem = Items.EMERALD;
                    Item cogItem = getCogwheel();
                    Item brassItem = getBrassIngot();
                    Item tubeItem = getElectronTube();
                    Item mechItem = getPrecisionMechanism();
                    Item sturdyItem = getSturdySheet();
                    
                    if (countItem(sender, ironItem) < reqIron || countItem(sender, pistonItem) < reqPiston ||
                        countItem(sender, diaItem) < reqDia || countItem(sender, emerItem) < reqEmer ||
                        countItem(sender, cogItem) < reqCog || countItem(sender, brassItem) < reqBrass ||
                        countItem(sender, tubeItem) < reqTube || countItem(sender, mechItem) < reqMech ||
                        countItem(sender, sturdyItem) < reqSturdy) {
                        status = Component.literal("§c[업그레이드 실패] 자재 암 강화 재료가 부족합니다.");
                    } else {
                        if (reqIron > 0) consumeItem(sender, ironItem, reqIron);
                        if (reqPiston > 0) consumeItem(sender, pistonItem, reqPiston);
                        if (reqDia > 0) consumeItem(sender, diaItem, reqDia);
                        if (reqEmer > 0) consumeItem(sender, emerItem, reqEmer);
                        if (reqCog > 0) consumeItem(sender, cogItem, reqCog);
                        if (reqBrass > 0) consumeItem(sender, brassItem, reqBrass);
                        if (reqTube > 0) consumeItem(sender, tubeItem, reqTube);
                        if (reqMech > 0) consumeItem(sender, mechItem, reqMech);
                        if (reqSturdy > 0) consumeItem(sender, sturdyItem, reqSturdy);
                        
                        int nextLvl = curLvl + 1;
                        sender.getPersistentData().putInt("nogeon_engineer_drone_upgrade_grabber_level", nextLvl);
                        sender.getPersistentData().putBoolean("nogeon_engineer_drone_upgrade_grabber", true);
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        status = Component.literal("§a[업그레이드] 자재 암이 " + nextLvl + "단계로 강화되었습니다!");
                    }
                }
            } else if (packet.actionType == 8) {
                // 9. Equip selected item to Gun or Ammo
                if (!sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_inventory")) {
                    status = Component.literal("§c먼저 드론 보관함 업그레이드를 완료해야 합니다.");
                } else if (stack.isEmpty()) {
                    status = Component.literal("§c장착할 아이템을 선택하세요.");
                } else {
                    boolean isGun = com.tacz.guns.api.item.IGun.getIGunOrNull(stack) != null;
                    boolean isAmmo = com.tacz.guns.api.item.IAmmo.getIAmmoOrNull(stack) != null;
                    
                    if (isGun) {
                        if (sender.getPersistentData().contains("nogeon_engineer_drone_gun")) {
                            ItemStack equipped = ItemStack.of(sender.getPersistentData().getCompound("nogeon_engineer_drone_gun"));
                            if (sender.getInventory().add(equipped)) {
                                sender.getPersistentData().remove("nogeon_engineer_drone_gun");
                            } else {
                                status = Component.literal("§c인벤토리에 공간이 부족하여 기존 총기를 탈착할 수 없습니다.");
                                DeconstructOpener.open(sender, selectedSlot, status);
                                return;
                            }
                        }
                        
                        ItemStack toEquip = stack.split(1);
                        sender.getPersistentData().put("nogeon_engineer_drone_gun", toEquip.save(new CompoundTag()));
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, 1.2F);
                        status = Component.literal("§a[장갑총 장착] 드론에 §e" + toEquip.getHoverName().getString() + "§a을(를) 장착했습니다.");
                    } else if (isAmmo) {
                        if (sender.getPersistentData().contains("nogeon_engineer_drone_ammo")) {
                            ItemStack equipped = ItemStack.of(sender.getPersistentData().getCompound("nogeon_engineer_drone_ammo"));
                            if (ItemStack.isSameItemSameTags(equipped, stack)) {
                                int total = equipped.getCount() + stack.getCount();
                                int max = equipped.getMaxStackSize();
                                if (total <= max) {
                                    equipped.setCount(total);
                                    stack.setCount(0);
                                    sender.getPersistentData().put("nogeon_engineer_drone_ammo", equipped.save(new CompoundTag()));
                                    status = Component.literal("§a[탄약 추가] 드론 탄약이 §e" + total + "개§a로 증가했습니다.");
                                } else {
                                    equipped.setCount(max);
                                    stack.setCount(total - max);
                                    sender.getPersistentData().put("nogeon_engineer_drone_ammo", equipped.save(new CompoundTag()));
                                    status = Component.literal("§a[탄약 추가] 드론 탄약이 최대로 충전되었습니다.");
                                }
                                sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                                    SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 0.8F, 1.5F);
                                DeconstructOpener.open(sender, selectedSlot, status);
                                return;
                            } else {
                                if (sender.getInventory().add(equipped)) {
                                    sender.getPersistentData().remove("nogeon_engineer_drone_ammo");
                                } else {
                                    status = Component.literal("§c인벤토리에 공간이 부족하여 기존 탄약을 탈착할 수 없습니다.");
                                    DeconstructOpener.open(sender, selectedSlot, status);
                                    return;
                                }
                            }
                        }
                        
                        ItemStack toEquip = stack.split(stack.getCount());
                        sender.getPersistentData().put("nogeon_engineer_drone_ammo", toEquip.save(new CompoundTag()));
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.2F);
                        status = Component.literal("§a[탄약 장착] 드론 탄약 슬롯에 §e" + toEquip.getHoverName().getString() + " " + toEquip.getCount() + "개§a를 장착했습니다.");
                    } else {
                        status = Component.literal("§c장착할 수 없는 아이템입니다. (TACZ 총기 혹은 탄약만 장착 가능)");
                    }
                }
            } else if (packet.actionType == 9) {
                // 10. Unequip Gun
                if (sender.getPersistentData().contains("nogeon_engineer_drone_gun")) {
                    ItemStack equipped = ItemStack.of(sender.getPersistentData().getCompound("nogeon_engineer_drone_gun"));
                    if (sender.getInventory().add(equipped)) {
                        sender.getPersistentData().remove("nogeon_engineer_drone_gun");
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, 0.8F);
                        status = Component.literal("§e[장비 탈착] 드론의 장갑총을 해제했습니다.");
                    } else {
                        status = Component.literal("§c인벤토리에 공간이 부족하여 장갑총을 회수할 수 없습니다.");
                    }
                } else {
                    status = Component.literal("§c장착된 장갑총이 없습니다.");
                }
            } else if (packet.actionType == 10) {
                // 11. Unequip Ammo
                if (sender.getPersistentData().contains("nogeon_engineer_drone_ammo")) {
                    ItemStack equipped = ItemStack.of(sender.getPersistentData().getCompound("nogeon_engineer_drone_ammo"));
                    if (sender.getInventory().add(equipped)) {
                        sender.getPersistentData().remove("nogeon_engineer_drone_ammo");
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 0.8F);
                        status = Component.literal("§e[장비 탈착] 드론의 탄약을 회수했습니다.");
                    } else {
                        status = Component.literal("§c인벤토리에 공간이 부족하여 탄약을 회수할 수 없습니다.");
                    }
                } else {
                    status = Component.literal("§c장착된 탄약이 없습니다.");
                }
            } else if (packet.actionType == 11) {
                // 12. Rename Drone
                String newName = packet.extraText.trim();
                if (newName.isEmpty()) {
                    status = Component.literal("§c드론 이름을 입력해주세요.");
                } else if (newName.length() > 14) {
                    status = Component.literal("§c드론 이름은 최대 14자까지 가능합니다.");
                } else {
                    sender.getPersistentData().putString("nogeon_engineer_drone_name", newName);
                    if (drone != null) {
                        drone.setDroneName(newName);
                    }
                    sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5F, 1.5F);
                    status = Component.literal("§a[이름 변경] 드론 이름이 §e" + newName + "§a(으)로 변경되었습니다.");
                }
            } else if (packet.actionType == 12) {
                // 13. Upgrade Attack
                int curLvl = sender.getPersistentData().getInt("nogeon_engineer_drone_stat_attack");
                if (curLvl <= 0) curLvl = 1;
                if (curLvl >= 5) {
                    status = Component.literal("§c공격력이 이미 최대 레벨(5)입니다.");
                } else {
                    int reqIron = 0, reqCopper = 0, reqGold = 0, reqDia = 0, reqRed = 0, reqPiston = 0;
                    int reqCog = 0, reqBrass = 0, reqTube = 0, reqMech = 0, reqSturdy = 0;

                    if (curLvl == 1) {
                        reqIron = 64; reqCopper = 32; reqCog = 10;
                    } else if (curLvl == 2) {
                        reqIron = 128; reqCopper = 64; reqRed = 64; reqPiston = 10; reqTube = 5;
                    } else if (curLvl == 3) {
                        reqIron = 256; reqCopper = 128; reqRed = 128; reqDia = 10; reqMech = 5;
                    } else if (curLvl == 4) {
                        reqIron = 512; reqDia = 20; reqMech = 10; reqSturdy = 5;
                    }
                    
                    Item ironItem = Items.IRON_INGOT;
                    Item copperItem = Items.COPPER_INGOT;
                    Item goldItem = Items.GOLD_INGOT;
                    Item diaItem = Items.DIAMOND;
                    Item redItem = Items.REDSTONE;
                    Item pistonItem = Items.PISTON;
                    Item cogItem = getCogwheel();
                    Item brassItem = getBrassIngot();
                    Item tubeItem = getElectronTube();
                    Item mechItem = getPrecisionMechanism();
                    Item sturdyItem = getSturdySheet();

                    if (countItem(sender, ironItem) < reqIron || countItem(sender, copperItem) < reqCopper ||
                        countItem(sender, goldItem) < reqGold || countItem(sender, diaItem) < reqDia ||
                        countItem(sender, redItem) < reqRed || countItem(sender, pistonItem) < reqPiston ||
                        countItem(sender, cogItem) < reqCog || countItem(sender, brassItem) < reqBrass ||
                        countItem(sender, tubeItem) < reqTube || countItem(sender, mechItem) < reqMech ||
                        countItem(sender, sturdyItem) < reqSturdy) {
                        status = Component.literal("§c[강화 실패] 강화 재료가 부족합니다.");
                    } else {
                        if (reqIron > 0) consumeItem(sender, ironItem, reqIron);
                        if (reqCopper > 0) consumeItem(sender, copperItem, reqCopper);
                        if (reqGold > 0) consumeItem(sender, goldItem, reqGold);
                        if (reqDia > 0) consumeItem(sender, diaItem, reqDia);
                        if (reqRed > 0) consumeItem(sender, redItem, reqRed);
                        if (reqPiston > 0) consumeItem(sender, pistonItem, reqPiston);
                        if (reqCog > 0) consumeItem(sender, cogItem, reqCog);
                        if (reqBrass > 0) consumeItem(sender, brassItem, reqBrass);
                        if (reqTube > 0) consumeItem(sender, tubeItem, reqTube);
                        if (reqMech > 0) consumeItem(sender, mechItem, reqMech);
                        if (reqSturdy > 0) consumeItem(sender, sturdyItem, reqSturdy);

                        sender.getPersistentData().putInt("nogeon_engineer_drone_stat_attack", curLvl + 1);
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        status = Component.literal("§a[스탯 강화] 드론 공격력 레벨이 §e" + (curLvl + 1) + "§a(으)로 강화되었습니다!");
                    }
                }
            } else if (packet.actionType == 13) {
                // 14. Upgrade Health
                int curLvl = sender.getPersistentData().getInt("nogeon_engineer_drone_stat_health");
                if (curLvl <= 0) curLvl = 1;
                if (curLvl >= 5) {
                    status = Component.literal("§c최대 체력이 이미 최대 레벨(5)입니다.");
                } else {
                    int reqIron = 0, reqCopper = 0, reqGold = 0, reqDia = 0, reqPiston = 0, reqEmer = 0;
                    int reqCog = 0, reqBrass = 0, reqTube = 0, reqMech = 0, reqSturdy = 0;

                    if (curLvl == 1) {
                        reqIron = 64; reqCopper = 32; reqPiston = 10;
                    } else if (curLvl == 2) {
                        reqIron = 128; reqCopper = 64; reqPiston = 20; reqCog = 10; reqBrass = 5;
                    } else if (curLvl == 3) {
                        reqIron = 256; reqCopper = 128; reqPiston = 30; reqDia = 5; reqTube = 10;
                    } else if (curLvl == 4) {
                        reqIron = 512; reqPiston = 50; reqDia = 20; reqMech = 10; reqSturdy = 5; reqEmer = 30;
                    }

                    Item ironItem = Items.IRON_INGOT;
                    Item copperItem = Items.COPPER_INGOT;
                    Item goldItem = Items.GOLD_INGOT;
                    Item pistonItem = Items.PISTON;
                    Item diaItem = Items.DIAMOND;
                    Item emerItem = Items.EMERALD;
                    Item cogItem = getCogwheel();
                    Item brassItem = getBrassIngot();
                    Item tubeItem = getElectronTube();
                    Item mechItem = getPrecisionMechanism();
                    Item sturdyItem = getSturdySheet();

                    if (countItem(sender, ironItem) < reqIron || countItem(sender, copperItem) < reqCopper ||
                        countItem(sender, goldItem) < reqGold || countItem(sender, pistonItem) < reqPiston ||
                        countItem(sender, diaItem) < reqDia || countItem(sender, emerItem) < reqEmer ||
                        countItem(sender, cogItem) < reqCog || countItem(sender, brassItem) < reqBrass ||
                        countItem(sender, tubeItem) < reqTube || countItem(sender, mechItem) < reqMech ||
                        countItem(sender, sturdyItem) < reqSturdy) {
                        status = Component.literal("§c[강화 실패] 강화 재료가 부족합니다.");
                    } else {
                        if (reqIron > 0) consumeItem(sender, ironItem, reqIron);
                        if (reqCopper > 0) consumeItem(sender, copperItem, reqCopper);
                        if (reqGold > 0) consumeItem(sender, goldItem, reqGold);
                        if (reqPiston > 0) consumeItem(sender, pistonItem, reqPiston);
                        if (reqDia > 0) consumeItem(sender, diaItem, reqDia);
                        if (reqEmer > 0) consumeItem(sender, emerItem, reqEmer);
                        if (reqCog > 0) consumeItem(sender, cogItem, reqCog);
                        if (reqBrass > 0) consumeItem(sender, brassItem, reqBrass);
                        if (reqTube > 0) consumeItem(sender, tubeItem, reqTube);
                        if (reqMech > 0) consumeItem(sender, mechItem, reqMech);
                        if (reqSturdy > 0) consumeItem(sender, sturdyItem, reqSturdy);

                        sender.getPersistentData().putInt("nogeon_engineer_drone_stat_health", curLvl + 1);
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        status = Component.literal("§a[스탯 강화] 드론 체력 레벨이 §e" + (curLvl + 1) + "§a(으)로 강화되었습니다!");
                        if (drone != null) {
                            double maxHealth = 20.0D + (curLvl + 1) * 10.0D;
                            drone.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(maxHealth);
                            drone.heal(10.0F);
                        }
                    }
                }
            } else if (packet.actionType == 14) {
                // 15. Upgrade Range
                int curLvl = sender.getPersistentData().getInt("nogeon_engineer_drone_stat_range");
                if (curLvl <= 0) curLvl = 1;
                if (curLvl >= 5) {
                    status = Component.literal("§c진공 자력이 이미 최대 레벨(5)입니다.");
                } else {
                    int reqRedstone = 0, reqLapis = 0, reqPearl = 0, reqDia = 0, reqEmer = 0;
                    int reqCog = 0, reqBrass = 0, reqTube = 0, reqMech = 0, reqSturdy = 0;

                    if (curLvl == 1) {
                        reqRedstone = 64; reqLapis = 32; reqCog = 10;
                    } else if (curLvl == 2) {
                        reqRedstone = 128; reqLapis = 64; reqPearl = 10; reqTube = 5;
                    } else if (curLvl == 3) {
                        reqRedstone = 256; reqLapis = 128; reqPearl = 20; reqDia = 5; reqMech = 5;
                    } else if (curLvl == 4) {
                        reqRedstone = 512; reqLapis = 256; reqPearl = 40; reqMech = 10; reqSturdy = 5; reqEmer = 30;
                    }

                    Item redstoneItem = Items.REDSTONE;
                    Item lapisItem = Items.LAPIS_LAZULI;
                    Item pearlItem = Items.ENDER_PEARL;
                    Item diaItem = Items.DIAMOND;
                    Item emerItem = Items.EMERALD;
                    Item cogItem = getCogwheel();
                    Item brassItem = getBrassIngot();
                    Item tubeItem = getElectronTube();
                    Item mechItem = getPrecisionMechanism();
                    Item sturdyItem = getSturdySheet();

                    if (countItem(sender, redstoneItem) < reqRedstone || countItem(sender, lapisItem) < reqLapis ||
                        countItem(sender, pearlItem) < reqPearl || countItem(sender, diaItem) < reqDia ||
                        countItem(sender, emerItem) < reqEmer || countItem(sender, cogItem) < reqCog ||
                        countItem(sender, brassItem) < reqBrass || countItem(sender, tubeItem) < reqTube ||
                        countItem(sender, mechItem) < reqMech || countItem(sender, sturdyItem) < reqSturdy) {
                        status = Component.literal("§c[강화 실패] 강화 재료가 부족합니다.");
                    } else {
                        if (reqRedstone > 0) consumeItem(sender, redstoneItem, reqRedstone);
                        if (reqLapis > 0) consumeItem(sender, lapisItem, reqLapis);
                        if (reqPearl > 0) consumeItem(sender, pearlItem, reqPearl);
                        if (reqDia > 0) consumeItem(sender, diaItem, reqDia);
                        if (reqEmer > 0) consumeItem(sender, emerItem, reqEmer);
                        if (reqCog > 0) consumeItem(sender, cogItem, reqCog);
                        if (reqBrass > 0) consumeItem(sender, brassItem, reqBrass);
                        if (reqTube > 0) consumeItem(sender, tubeItem, reqTube);
                        if (reqMech > 0) consumeItem(sender, mechItem, reqMech);
                        if (reqSturdy > 0) consumeItem(sender, sturdyItem, reqSturdy);

                        sender.getPersistentData().putInt("nogeon_engineer_drone_stat_range", curLvl + 1);
                        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                        status = Component.literal("§a[스탯 강화] 드론 자력 범위 레벨이 §e" + (curLvl + 1) + "§a(으)로 강화되었습니다!");
                    }
                }
            } else if (packet.actionType == 17) {
                // 17. Open Drone Storage Screen (가상 컨테이너 GUI 오픈)
                if (!sender.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_inventory")) {
                    status = Component.literal("§c보관함이 해금되지 않았습니다.");
                } else {
                    sender.closeContainer();
                    com.nogeon.economyland.menu.DroneStorageOpener.open(sender);
                    return; // 화면이 드론 보관함으로 전환되었으므로 DeconstructOpener.open을 호출하지 않고 빠져나감!
                }
            } else if (packet.actionType == 18) {
                // 18. Toggle Magnet
                boolean current = sender.getPersistentData().getBoolean("nogeon_engineer_drone_magnet_disabled");
                sender.getPersistentData().putBoolean("nogeon_engineer_drone_magnet_disabled", !current);
                if (!current) {
                    status = Component.literal("§e[자석 제어] 드론의 진공 자석 효과를 §c비활성화§e했습니다.");
                } else {
                    status = Component.literal("§e[자석 제어] 드론의 진공 자석 효과를 §a활성화§e했습니다.");
                }
                sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, !current ? 0.8F : 1.2F);
            }
            
            SyncCreditsPacket.send(sender, profile.credits());
            SyncPlayerInventoryPacket.send(sender);
            sender.inventoryMenu.broadcastChanges();
            state.setDirty();
            
            // Keep Deconstruct GUI open with updated status message
            DeconstructOpener.open(sender, selectedSlot, status, packet.uiTab);
        });
        context.setPacketHandled(true);
    }

    private static Item getCreateItem(String path, Item fallback) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("create", path));
        return item == Items.AIR ? fallback : item;
    }

    private static Item getElectronTube() {
        return getCreateItem("electron_tube", Items.COMPARATOR);
    }
    
    private static Item getPrecisionMechanism() {
        return getCreateItem("precision_mechanism", Items.CLOCK);
    }
    
    private static Item getBrassIngot() {
        return getCreateItem("brass_ingot", Items.GOLD_INGOT);
    }
    
    private static Item getSturdySheet() {
        return getCreateItem("sturdy_sheet", Items.NETHERITE_INGOT);
    }
    
    private static Item getCogwheel() {
        return getCreateItem("cogwheel", Items.COPPER_INGOT);
    }
    
    private static Item getLargeCogwheel() {
        return getCreateItem("large_cogwheel", Items.IRON_INGOT);
    }

    private static int countItem(ServerPlayer player, Item targetItem) {
        if (targetItem == Items.AIR) {
            return 0;
        }
        return com.nogeon.economyland.player.ExtendedInventoryDelivery.countAllOwned(player, new ItemStack(targetItem));
    }

    private static void consumeItem(ServerPlayer player, Item targetItem, int amount) {
        if (targetItem == Items.AIR || amount <= 0) {
            return;
        }
        com.nogeon.economyland.player.ExtendedInventoryDelivery.consumeAllOwned(player, new ItemStack(targetItem), amount);
    }
}
