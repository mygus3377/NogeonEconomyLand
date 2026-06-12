package com.nogeon.economyland.job;

import com.nogeon.economyland.item.SmithingService;
import com.nogeon.economyland.item.SmithEvents;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.entity.FarmerScarecrowEntity;
import com.nogeon.economyland.land.LandRegion;
import com.nogeon.economyland.land.LandType;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import com.nogeon.economyland.player.JobProgress;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.player.SkillNodeStat;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.state.EconomyState;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.resources.ResourceLocation;

public final class JobEvents {
    private static final ThreadLocal<Boolean> isChainMining = ThreadLocal.withInitial(() -> false);
    private static final java.util.UUID HEART_BREATH_UUID = java.util.UUID.fromString("c00b411d-ca5e-40cd-a78b-d468139167b1");
    private static final java.util.UUID ENGINEER_REACH_UUID = java.util.UUID.fromString("6a04e38e-897d-411a-826c-d238cbbe16b2");
    private static final String MINER_EYE_HEALTH_CAP_TAG = "nogeon_miner_eye_health_cap";

    public static final java.util.Map<BlockPos, Integer> FERTILE_SOILS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<BlockPos, FisheryZone> FISHERY_ZONES = new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.List<HarvestCheck> PENDING_HARVEST_CHECKS = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static class HarvestCheck {
        final ServerPlayer player;
        final ServerLevel level;
        final BlockPos pos;
        final BlockState oldState;
        final long gameTime;

        HarvestCheck(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState oldState, long gameTime) {
            this.player = player;
            this.level = level;
            this.pos = pos;
            this.oldState = oldState;
            this.gameTime = gameTime;
        }
    }

    public static class FisheryZone {
        public final BlockPos pos;
        public int ticksRemaining;
        public final int skillLevel;
        public final java.util.UUID owner;

        public FisheryZone(BlockPos pos, int ticksRemaining, int skillLevel, java.util.UUID owner) {
            this.pos = pos;
            this.ticksRemaining = ticksRemaining;
            this.skillLevel = skillLevel;
            this.owner = owner;
        }
    }

    public static BlockPos waterBlockForFishingHook(ServerLevel level, net.minecraft.world.entity.projectile.FishingHook hook) {
        if (hook == null) {
            return null;
        }
        BlockPos pos = hook.blockPosition();
        if (level.getFluidState(pos).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER) && isFishingWaterBody(level, pos)) {
            return pos;
        }
        BlockPos below = pos.below();
        if (level.getFluidState(below).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER) && isFishingWaterBody(level, below)) {
            return below;
        }
        return null;
    }

    public static boolean isFishingWaterBody(ServerLevel level, BlockPos center) {
        if (!level.getFluidState(center).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER)) {
            return false;
        }
        int openWater = 0;
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.offset(x, 0, z);
                if (level.getFluidState(pos).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER)
                    && level.getBlockState(pos.above()).isAir()) {
                    openWater++;
                }
            }
        }
        return openWater >= 24;
    }

    public static void applyHunterSenseGlow(ServerPlayer player, int radius) {
        AABB area = player.getBoundingBox().inflate(radius, radius, radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && !(e instanceof Player));
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
        }
    }

    public static void clearHunterSenseGlow(ServerPlayer player, int radius) {
        AABB area = player.getBoundingBox().inflate(radius, radius, radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && !(e instanceof Player));
        for (LivingEntity target : targets) {
            target.removeEffect(MobEffects.GLOWING);
        }
    }

    public static int hunterWeakpointIndex(LivingEntity victim) {
        long period = victim.level().getGameTime() / 60L;
        return new java.util.Random(victim.getUUID().getMostSignificantBits() ^ victim.getUUID().getLeastSignificantBits() ^ period).nextInt(6);
    }

    public static net.minecraft.world.phys.Vec3 hunterWeakpointPosition(LivingEntity victim, int index) {
        double rad = Math.toRadians(victim.getYRot());
        double distance = victim.getBbWidth() * 0.5D + 0.08D; // Pinned sleekly to the skin!
        double ox = 0.0D;
        double oz = 0.0D;
        double y = victim.getY() + victim.getBbHeight() * 0.55D;
        switch (index) {
            case 0 -> {
                ox = -Math.sin(rad) * distance;
                oz = Math.cos(rad) * distance;
            }
            case 1 -> {
                ox = Math.sin(rad) * distance;
                oz = -Math.cos(rad) * distance;
            }
            case 2 -> {
                ox = -Math.cos(rad) * distance;
                oz = -Math.sin(rad) * distance;
            }
            case 3 -> {
                ox = Math.cos(rad) * distance;
                oz = Math.sin(rad) * distance;
            }
            case 4 -> y = victim.getY() + victim.getBbHeight() * 0.88D;
            default -> y = victim.getY() + victim.getBbHeight() * 0.22D;
        }
        return new net.minecraft.world.phys.Vec3(victim.getX() + ox, y, victim.getZ() + oz);
    }

    private static boolean hitHunterWeakpoint(ServerPlayer player, LivingEntity victim, int weakpointIndex) {
        net.minecraft.world.phys.Vec3 weakpoint = hunterWeakpointPosition(victim, weakpointIndex);
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle().normalize();
        net.minecraft.world.phys.Vec3 toWeakpoint = weakpoint.subtract(eye);
        double alongLook = toWeakpoint.dot(look);
        if (alongLook > 0.0D && alongLook < 7.5D) {
            double distanceToRay = toWeakpoint.subtract(look.scale(alongLook)).length();
            if (distanceToRay <= Math.max(0.22D, victim.getBbWidth() * 0.15D)) {
                return true;
            }
        }
        return false;
    }

    private JobEvents() {
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getPersistentData().getBoolean("nogeon_suppress_miner_break_event")) {
            return;
        }
        if (!canRewardBlockBreak(player, event.getPos())) {
            return;
        }

        BlockState state = event.getState();
        if (isFarmerHarvestAllowed(state, player.serverLevel(), event.getPos())) {
            EconomyState economy = EconomyState.get(player.server);
            PlayerProfile profile = economy.profile(player.getUUID());
            if (profile.selectedJob() == JobType.FARMER) {
                event.setCanceled(true);
                applyFarmerPerks(player, state, event.getPos(), false);
                int exp = farmerCropExp(state);
                addExp(player, JobType.FARMER, exp);
                grantActivityCredits(profile, economy, JobType.FARMER, Math.max(20L, exp * 3L));
            }
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (tool.is(ItemTags.PICKAXES)) {
            PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
            if (profile.selectedJob() == JobType.ENGINEER) {
                String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                boolean isCreateOre = blockId.contains("zinc") || blockId.contains("copper") || blockId.contains("limestone");
                if (isCreateOre && !isPlayerPlacedResourceBlock(player.serverLevel(), event.getPos())) {
                    int exp = 8;
                    int credits = 40;
                    addExp(player, JobType.ENGINEER, exp);
                    profile.addCredits(credits);
                    EconomyState.get(player.server).setDirty();
                    player.displayClientMessage(Component.literal("§6[공학 채광] §f산업 광물 채굴: §a+" + exp + " EXP§f / §e+" + credits + " C§f를 획득했습니다!"), true);
                }
            } else if (profile.selectedJob() == JobType.MINER) {
                int stoneSkinLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_STONE_SKIN);
                
                // 50레벨 [우월한 신체] (연쇄 채굴) 작동
                if (stoneSkinLevel > 0 && profile.minerBodyActive()) {
                    handleMinerBreakEvents(player, state, event.getPos());
                    performChainMine(player, state, event.getPos(), stoneSkinLevel);
                } else {
                    handleMinerBreakEvents(player, state, event.getPos());
                }
            }
        }
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (isTrackedPlacedResourceBlock(event.getPlacedBlock()) || isStemFruitBlock(event.getPlacedBlock())) {
            EconomyState.get(player.server).markPlayerPlacedResourceBlock(level.dimension(), event.getPos());
        }
    }

    public static void handleMinerBreakEvents(ServerPlayer player, BlockState state, BlockPos pos) {
        handleMinerBreakEvents(player, state, pos, true);
    }

    public static void handleMinerBreakEvents(ServerPlayer player, BlockState state, BlockPos pos, boolean allowRockBonus) {
        if (!canRewardBlockBreak(player, pos)) {
            return;
        }
        if (isPlayerPlacedResourceBlock(player.serverLevel(), pos)) {
            return;
        }
        boolean isOre = isOreBlock(state);
        boolean isRock = isRockBlock(state);
        if (isOre || isRock) {
            EconomyState economy = EconomyState.get(player.server);
            PlayerProfile profile = economy.profile(player.getUUID());
            applyMinerPerks(player, state, pos);
            if (isOre) {
                int exp = minerBlockExp(state, true);
                addExp(player, JobType.MINER, exp);
                grantActivityCredits(profile, economy, JobType.MINER, Math.max(25L, exp * 8L));
                triggerDroneExpression(player, 3, 40); // Happy expression
            } else {
                // 25레벨 [광물 사냥꾼] 작동
                int veinStrikeLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_VEIN_STRIKE);
                if (allowRockBonus && profile.selectedJob() == JobType.MINER && veinStrikeLevel > 0) {
                    applyOreHunter(player, pos, veinStrikeLevel);
                }
                addExp(player, JobType.MINER, minerBlockExp(state, false)); // 암석 채굴 시 경험치 부여
                grantActivityCredits(profile, economy, JobType.MINER, 3L);
            }
        }
    }

    private static boolean canRewardBlockBreak(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        EconomyState state = EconomyState.get(level.getServer());
        LandRegion exactLand = state.landAt(level.dimension(), pos);
        if (exactLand != null && exactLand.type().protectedLand()) {
            return exactLand.type() != LandType.ADMIN && exactLand.canBuild(player.getUUID());
        }
        LandRegion columnLand = state.landColumnAt(level.dimension(), pos);
        return columnLand == null || columnLand.type() != LandType.ADMIN;
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        
        // 서버사이드 속도 보정
        ServerPlayer serverPlayer = (ServerPlayer) player;
        EconomyState state = EconomyState.get(serverPlayer.server);
        PlayerProfile profile = state.profile(serverPlayer.getUUID());
        if (profile.selectedJob() == JobType.MINER) {
            int skillLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_TUNNEL_ROUTINE);
            if (skillLevel > 0) {
                // 작은노드: 채광 속도 증가 (레벨당 8%)
                event.setNewSpeed(event.getOriginalSpeed() * (1.0F + (float) scaledPercent(skillLevel, 0.02D, 0.60D)));
            }
        }
    }

    public static void syncFisherDataToPlayer(ServerPlayer player) {
        CompoundTag playerNbt = player.getPersistentData();
        int gauge = playerNbt.getInt("nogeon_fisher_flow_gauge");
        
        BlockPos hotspotPos = null;
        double hotspotRadius = 0.0D;
        if (playerNbt.contains("nogeon_hotspot_x")) {
            hotspotPos = new BlockPos(
                playerNbt.getInt("nogeon_hotspot_x"),
                playerNbt.getInt("nogeon_hotspot_y"),
                playerNbt.getInt("nogeon_hotspot_z")
            );
            PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
            int lureTuningLevel = profile.job(JobType.FISHER).nodeLevel(SkillNode.FISHER_LURE_TUNING);
            hotspotRadius = Math.min(7.5D, 3.5D + lureTuningLevel * 0.45D);
        }
        
        java.util.Map<BlockPos, Double> zones = new java.util.HashMap<>();
        for (java.util.Map.Entry<BlockPos, FisheryZone> entry : FISHERY_ZONES.entrySet()) {
            FisheryZone zone = entry.getValue();
            double radius = Math.min(18.0D, 5.0D + zone.skillLevel * 1.4D);
            zones.put(entry.getKey(), radius);
        }
        
        com.nogeon.economyland.network.ModNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new com.nogeon.economyland.network.SyncFisherDataPacket(gauge, hotspotPos, hotspotRadius, zones)
        );
    }

    private static void syncMinerEyeState(ServerPlayer player, PlayerProfile profile, boolean active, int radius) {
        com.nogeon.economyland.network.ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new com.nogeon.economyland.network.SyncMinerAbilityPacket(profile.minerBodyActive(), active, active ? radius : 0));
    }

    private static void disableMinerEye(ServerPlayer player, EconomyState state, PlayerProfile profile, Component message) {
        profile.setMinerEyeActive(false);
        state.setDirty();
        player.getPersistentData().remove(MINER_EYE_HEALTH_CAP_TAG);
        syncMinerEyeState(player, profile, false, 0);
        if (message != null) {
            player.displayClientMessage(message, true);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    private static void clampMinerEyeHealing(ServerPlayer player) {
        CompoundTag nbt = player.getPersistentData();
        float current = player.getHealth();
        if (nbt.contains(MINER_EYE_HEALTH_CAP_TAG)) {
            float cap = Math.max(1.0F, Math.min(nbt.getFloat(MINER_EYE_HEALTH_CAP_TAG), player.getMaxHealth()));
            if (current > cap) {
                player.setHealth(cap);
                current = cap;
            }
        }
        nbt.putFloat(MINER_EYE_HEALTH_CAP_TAG, Math.min(current, player.getMaxHealth()));
    }

    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        if (profile.minerEyeActive()) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (player.getPersistentData().contains("nogeon_heart_breath_ticks")) {
            event.setAmount(event.getAmount() * 1.3F);
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player.level().isClientSide) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.player;
        if (player.tickCount % 20 != 0) {
            return; // 1초에 한 번만 실행
        }

        // 1. Drone expression reset timer countdown
        if (player.getPersistentData().contains("nogeon_drone_expr_reset_ticks")) {
            int ticks = player.getPersistentData().getInt("nogeon_drone_expr_reset_ticks");
            if (ticks > 0) {
                player.getPersistentData().putInt("nogeon_drone_expr_reset_ticks", ticks - 20);
            } else {
                player.getPersistentData().remove("nogeon_drone_expr_reset_ticks");
                player.getPersistentData().remove("nogeon_drone_expr_saved_id");
                
                AABB searchBox = player.getBoundingBox().inflate(32.0D);
                List<com.nogeon.economyland.entity.ScrapDroneEntity> drones = player.level().getEntitiesOfClass(
                    com.nogeon.economyland.entity.ScrapDroneEntity.class, searchBox,
                    d -> d.getOwnerUuid().map(uuid -> uuid.equals(player.getUUID())).orElse(false)
                );
                if (!drones.isEmpty()) {
                    drones.get(0).setExpression(0);
                }
            }
        }

        // 2. Robotic Grabber Reach Distance Attribute Modifier
        java.util.UUID grabberModifierId = java.util.UUID.fromString("6a4cf8ea-3c4f-4d9f-a89e-8c38fa2c300f");
        net.minecraft.world.entity.ai.attributes.AttributeInstance reachAttr = player.getAttribute(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());
        if (reachAttr != null) {
            boolean hasGrabber = player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_grabber");
            int grabberLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_grabber_level");
            if (grabberLvl <= 0 && hasGrabber) {
                grabberLvl = 1;
            }
            boolean droneBroken = player.getPersistentData().getBoolean("nogeon_engineer_drone_broken");
            boolean droneActive = false;
            AABB searchBox = player.getBoundingBox().inflate(32.0D);
            List<com.nogeon.economyland.entity.ScrapDroneEntity> drones = player.level().getEntitiesOfClass(
                com.nogeon.economyland.entity.ScrapDroneEntity.class, searchBox,
                d -> d.getOwnerUuid().map(uuid -> uuid.equals(player.getUUID())).orElse(false)
            );
            if (!drones.isEmpty() && drones.get(0).getCharge() > 0 && !droneBroken) {
                droneActive = true;
            }

            if (grabberLvl > 0 && droneActive) {
                double reachVal = 1.0D + (grabberLvl - 1) * 1.0D; // scales from +1.0 to +5.0 blocks
                net.minecraft.world.entity.ai.attributes.AttributeModifier activeMod = reachAttr.getModifier(grabberModifierId);
                if (activeMod == null || activeMod.getAmount() != reachVal) {
                    if (activeMod != null) {
                        reachAttr.removeModifier(grabberModifierId);
                    }
                    reachAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        grabberModifierId, "Robotic Grabber Upgrade", reachVal, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                    ));
                }
            } else {
                if (reachAttr.getModifier(grabberModifierId) != null) {
                    reachAttr.removeModifier(grabberModifierId);
                }
            }
        }

        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());

        boolean stateChanged = stopMismatchedActiveSkills(player, profile);
        if (stateChanged) {
            state.setDirty();
        }

        if (player.tickCount % 60 == 0) {
            markCookOutputsFirstHolder(player, profile);
        }

        if (profile.selectedJob() == JobType.MINER) {
            // 작은노드: 채굴 속도 증가 패시브 (곡괭이 쥐고 있으면 성급함 효과 갱신하여 클라이언트 동기화 완료)
            int tunnelRoutineLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_TUNNEL_ROUTINE);
            if (tunnelRoutineLevel > 0 && player.getMainHandItem().is(ItemTags.PICKAXES)) {
                int amplifier = Math.min(3, Math.max(0, tunnelRoutineLevel / 8));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 39, amplifier, false, false));
            }
        }

        if (profile.selectedJob() == JobType.ENGINEER) {
            boolean nearCreate = false;
            int industrialCreateCount = 0;
            if (player.tickCount % 20 == 0) {
                BlockPos pos = player.blockPosition();
                for (int dx = -8; dx <= 8; dx++) {
                    for (int dy = -4; dy <= 4; dy++) {
                        for (int dz = -8; dz <= 8; dz++) {
                            BlockPos target = pos.offset(dx, dy, dz);
                            BlockState bState = player.level().getBlockState(target);
                            if (!bState.isAir()) {
                                String blockId = BuiltInRegistries.BLOCK.getKey(bState.getBlock()).toString();
                                boolean isMachineBlock = blockId.startsWith("create:")
                                    || blockId.startsWith("create_dd:")
                                    || blockId.startsWith("createaddition:")
                                    || blockId.startsWith("create_new_age:")
                                    || blockId.startsWith("createdieselgenerators:")
                                    || blockId.startsWith("create_enchantment_industry:")
                                    || blockId.startsWith("create_hypertube:")
                                    || blockId.startsWith("create_sabers:")
                                    || blockId.startsWith("create_jetpack:");
                                    
                                if (isMachineBlock) {
                                    LandRegion blockLand = state.landAt(player.level().dimension(), target);
                                    if (blockLand != null && blockLand.type() == LandType.INDUSTRIAL && blockLand.canBuild(player.getUUID())) {
                                        industrialCreateCount++;
                                    }
                                }
                            }
                        }
                    }
                }
                nearCreate = industrialCreateCount > 0;
                player.getPersistentData().putBoolean("nogeon_near_create", nearCreate);
                player.getPersistentData().putInt("nogeon_industrial_create_count", industrialCreateCount);
                
                if (nearCreate && player.tickCount % 100 == 0) {
                    int bonusExp = 2 + Math.min(28, industrialCreateCount / 5);
                    int bonusCredits = 10 + Math.min(90, industrialCreateCount);
                    addExp(player, JobType.ENGINEER, bonusExp);
                    profile.addCredits(bonusCredits);
                    state.setDirty();
                    player.displayClientMessage(Component.literal("§6[공장 관리] §f산업 기계 §e" + industrialCreateCount + "개§f 가동 보너스: §a+" + bonusExp + " EXP§f / §e+" + bonusCredits + " C§f"), true);
                }
            } else {
                nearCreate = player.getPersistentData().getBoolean("nogeon_near_create");
                industrialCreateCount = player.getPersistentData().getInt("nogeon_industrial_create_count");
            }

            int perfectAssemblyLevel = profile.job(JobType.ENGINEER).nodeLevel(SkillNode.ENGINEER_PERFECT_ASSEMBLY);
            if (perfectAssemblyLevel > 0 && nearCreate) {
                int hasteAmp = perfectAssemblyLevel >= 5 ? 2 : 1; // Haste III vs Haste II
                int speedAmp = perfectAssemblyLevel >= 5 ? 1 : 0; // Speed II vs Speed I
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 39, hasteAmp, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 39, speedAmp, false, false));

                if (player.tickCount % 40 == 0) {
                    if (player.getRandom().nextDouble() * 10.0D < perfectAssemblyLevel) {
                        player.getFoodData().eat(1, 0.5F);
                        restoreThirst(player, 1, 1);
                    }
                }

                if (player.tickCount % 20 == 0) {
                    for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
                        if (!stack.isEmpty() && stack.isDamaged()) {
                            String itemStr = stack.getItem().toString().toLowerCase(Locale.ROOT);
                            if (itemStr.contains("wrench") || itemStr.contains("pickaxe")) {
                                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
                            }
                        }
                    }
                }
            }
        } else {
            if (reachAttr != null && reachAttr.getModifier(ENGINEER_REACH_UUID) != null) {
                reachAttr.removeModifier(ENGINEER_REACH_UUID);
            }
        }
        
        CompoundTag playerNbt = player.getPersistentData();
        if (playerNbt.contains("nogeon_engineer_kinetic_boost_ticks")) {
            playerNbt.putInt("nogeon_engineer_kinetic_boost_ticks", 100);
            int lvl = playerNbt.getInt("nogeon_engineer_kinetic_boost_level");
            
            // Find physical ScrapDroneEntity in 256-block radius to prevent duplication/leaking on movement
            AABB searchBox256 = player.getBoundingBox().inflate(256.0D);
            List<com.nogeon.economyland.entity.ScrapDroneEntity> drones = player.level().getEntitiesOfClass(
                com.nogeon.economyland.entity.ScrapDroneEntity.class, searchBox256,
                d -> d.getOwnerUuid().map(uuid -> uuid.equals(player.getUUID())).orElse(false)
            );
            
            // Discard duplicate drones if more than one exists
            if (drones.size() > 1) {
                for (int i = 1; i < drones.size(); i++) {
                    drones.get(i).discard();
                }
            }

            boolean hasPower = false;
            if (!drones.isEmpty()) {
                com.nogeon.economyland.entity.ScrapDroneEntity mainDrone = drones.get(0);
                hasPower = mainDrone.getCharge() > 0;
                
                // Teleport the drone if it is far away (> 16 blocks) instead of spawning a new one
                if (mainDrone.distanceToSqr(player) > 256.0D) {
                    mainDrone.moveTo(player.getX(), player.getY() + 1.8D, player.getZ(), mainDrone.getYRot(), mainDrone.getXRot());
                }
            }
            
            if (hasPower) {
                // Active player buffs: Haste II & Speed II
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 39, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 39, 1, false, false));
            }

            // Verify and spawn physical ScrapDroneEntity if missing (every 20 ticks / 1 second on server)
            if (!player.level().isClientSide && player.tickCount % 20 == 0) {
                if (drones.isEmpty()) {
                    com.nogeon.economyland.entity.ScrapDroneEntity drone = com.nogeon.economyland.entity.ModEntities.SCRAP_DRONE.get().create(player.level());
                    if (drone != null) {
                        drone.setup(player.getUUID(), lvl);
                        drone.setPos(player.getX(), player.getY() + 1.8D, player.getZ());
                        player.level().addFreshEntity(drone);
                    }
                }
            }
        } else {
            // 영구 기관 스킬 비활성화 시 필드 상의 드론 디스폰 처리 (256 범위 전체 청소)
            if (!player.level().isClientSide && player.tickCount % 20 == 0) {
                AABB searchBox256 = player.getBoundingBox().inflate(256.0D);
                List<com.nogeon.economyland.entity.ScrapDroneEntity> drones = player.level().getEntitiesOfClass(
                    com.nogeon.economyland.entity.ScrapDroneEntity.class, searchBox256,
                    d -> d.getOwnerUuid().map(uuid -> uuid.equals(player.getUUID())).orElse(false)
                );
                for (com.nogeon.economyland.entity.ScrapDroneEntity drone : drones) {
                    drone.discard();
                }
            }
        }

        if (profile.selectedJob() == JobType.FISHER) {
            int doubleHookLevel = profile.job(JobType.FISHER).nodeLevel(SkillNode.FISHER_DOUBLE_HOOK);
            if (doubleHookLevel > 0) {
                int ticks = playerNbt.getInt("nogeon_hotspot_timer") - 20;
                
                int radius = Math.min(10, 4 + doubleHookLevel);
                if (ticks <= 0) {
                    BlockPos newHotspot = null;
                    BlockPos pPos = player.blockPosition();
                    ServerLevel sLevel = player.serverLevel();
                    newHotspot = waterBlockForFishingHook(sLevel, player.fishing);
                    
                    // 물 표면 검색 최적화: 시도 횟수를 250회로 늘리고 Y 범위를 물 표면 중심(-3 ~ +1)으로 집중시켜 백발백중 성공하도록 보장
                    if (newHotspot == null) {
                        for (int i = 0; i < 160; i++) {
                            int rx = pPos.getX() + sLevel.random.nextInt(radius * 2 + 1) - radius;
                            int ry = pPos.getY() + sLevel.random.nextInt(5) - 3;
                            int rz = pPos.getZ() + sLevel.random.nextInt(radius * 2 + 1) - radius;
                            BlockPos tPos = new BlockPos(rx, ry, rz);
                            if (sLevel.getFluidState(tPos).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER)
                                && sLevel.getBlockState(tPos.above()).isAir()
                                && isFishingWaterBody(sLevel, tPos)) {
                                newHotspot = tPos;
                                break;
                            }
                        }
                    }
                    
                    if (newHotspot != null) {
                        playerNbt.putInt("nogeon_hotspot_x", newHotspot.getX());
                        playerNbt.putInt("nogeon_hotspot_y", newHotspot.getY());
                        playerNbt.putInt("nogeon_hotspot_z", newHotspot.getZ());
                        playerNbt.putInt("nogeon_hotspot_timer", Math.min(120, 45 + doubleHookLevel * 8) * 20);
                        player.displayClientMessage(Component.literal("§b[물고기 떼] §f근처 물가에 입질이 강한 지점이 생겼습니다."), true);
                    } else {
                        // 검색에 실패해도 기존 hotspot을 성급하게 지우지 않고 2초 뒤 재검색
                        playerNbt.putInt("nogeon_hotspot_timer", 40);
                    }
                } else {
                    playerNbt.putInt("nogeon_hotspot_timer", ticks);
                    if (playerNbt.contains("nogeon_hotspot_x")) {
                        BlockPos hotspot = new BlockPos(
                            playerNbt.getInt("nogeon_hotspot_x"),
                            playerNbt.getInt("nogeon_hotspot_y"),
                            playerNbt.getInt("nogeon_hotspot_z")
                        );
                        // 거리 판정 여유 부여 (radius + 16) 하여 캐스팅 범위 내에서 절대 끊어지지 않고 안정적으로 한 곳에 고정되게 만듦
                        if (player.blockPosition().distSqr(hotspot) > (radius + 16) * (radius + 16)) {
                            playerNbt.remove("nogeon_hotspot_x");
                            playerNbt.putInt("nogeon_hotspot_timer", 20);
                        } else {
                            ServerLevel sLevel = player.serverLevel();
                            double effectRadius = Math.min(7.5D, 3.5D + doubleHookLevel * 0.45D);
                            double px = hotspot.getX() + 0.5D + (sLevel.random.nextDouble() - 0.5D) * effectRadius * 2.0D;
                            double py = hotspot.getY() + 0.9D;
                            double pz = hotspot.getZ() + 0.5D + (sLevel.random.nextDouble() - 0.5D) * effectRadius * 2.0D;
                            
                            sLevel.sendParticles(ParticleTypes.BUBBLE, px, py, pz, 10, 0.45D, 0.18D, 0.45D, 0.04D);
                            sLevel.sendParticles(ParticleTypes.FISHING, px, py + 0.15D, pz, 5, 0.35D, 0.05D, 0.35D, 0.0D);
                            sLevel.sendParticles(ParticleTypes.GLOW, px, py + 0.25D, pz, 4, 0.35D, 0.05D, 0.35D, 0.0D);
                            if (player.tickCount % 40 == 0) {
                                for (int i = 0; i < 16; i++) {
                                    double angle = (i / 16.0D) * Math.PI * 2.0D + player.tickCount * 0.08D;
                                    sLevel.sendParticles(ParticleTypes.SPLASH,
                                        hotspot.getX() + 0.5D + Math.cos(angle) * effectRadius,
                                        hotspot.getY() + 0.9D,
                                        hotspot.getZ() + 0.5D + Math.sin(angle) * effectRadius,
                                        1, 0.0D, 0.08D, 0.0D, 0.0D);
                                }
                                sLevel.playSound(null, hotspot, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.65F, 1.25F);
                            }
                        }
                    }
                }
            } else {
                player.getPersistentData().remove("nogeon_hotspot_x");
                player.getPersistentData().remove("nogeon_hotspot_timer");
            }
            syncFisherDataToPlayer(player);
        }

        // 100레벨 [개안] 스킬 지속 피해 및 자동 해제
        if (profile.minerEyeActive()) {
            if (profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_EYE_OPENING) <= 0) {
                disableMinerEye(player, state, profile, null);
                return;
            }

            {
                clampMinerEyeHealing(player);
                float maxHealth = player.getMaxHealth();
                float damage = maxHealth * 0.05F; // 최대 체력의 5% 대미지

                if (player.getHealth() <= damage + 1.5F) {
                    // 저체력 시 개안 자동 해제
                    disableMinerEye(player, state, profile, Component.literal("§c[개안] §f체력이 너무 부족하여 개안 스킬이 자동으로 해제되었습니다!"));
                } else {
                    player.hurt(player.damageSources().magic(), damage);
                    player.getPersistentData().putFloat(MINER_EYE_HEALTH_CAP_TAG, Math.min(player.getHealth(), player.getMaxHealth()));
                }
            }
        } else {
            player.getPersistentData().remove(MINER_EYE_HEALTH_CAP_TAG);
        }

        // 사냥꾼 [추적자의 감각] 허기 소모 및 감지 스캔
        if (profile.hunterSenseActive()) {
            int quickDrawLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_QUICK_DRAW);
            if (quickDrawLevel <= 0) {
                clearHunterSenseGlow(player, 42);
                profile.setHunterSenseActive(false);
                state.setDirty();
                return;
            }

            // 허기 소모 가중
            player.getFoodData().addExhaustion(0.08F + quickDrawLevel * 0.04F);

            boolean outOfHunger = player.getFoodData().getFoodLevel() <= 2;

            if (outOfHunger) {
                int radius = Math.min(42, 12 + quickDrawLevel * 3);
                clearHunterSenseGlow(player, radius);
                profile.setHunterSenseActive(false);
                state.setDirty();
                player.displayClientMessage(Component.literal("§c[추적자의 감각] §f허기가 너무 부족하여 스킬이 해제되었습니다!"), true);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 0.8F);
                
                com.nogeon.economyland.network.ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new com.nogeon.economyland.network.SyncHunterAbilityPacket(false, radius, profile.hunterPreyMarkedUUID()));
            } else {
                int radius = Math.min(42, 12 + quickDrawLevel * 3);
                applyHunterSenseGlow(player, radius);
                AABB area = player.getBoundingBox().inflate(radius, radius, radius);
                List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && !(e instanceof Player));
                for (LivingEntity target : targets) {
                    if (player.getRandom().nextDouble() < 0.15D) {
                        ServerLevel sLevel = player.serverLevel();
                        double px = target.getX() + (sLevel.random.nextDouble() - 0.5D) * 0.4D;
                        double py = target.getY() + sLevel.random.nextDouble() * target.getBbHeight();
                        double pz = target.getZ() + (sLevel.random.nextDouble() - 0.5D) * 0.4D;
                        sLevel.sendParticles(ParticleTypes.INSTANT_EFFECT, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }

        // 요리사 [나만의 레시피] 지속 버프 틱 카운트다운 (1초마다 20틱씩 소모)
        if (playerNbt.contains("nogeon_boss_slayer_ticks")) {
            int slayerTicks = playerNbt.getInt("nogeon_boss_slayer_ticks") - 20;
            if (slayerTicks <= 0) {
                playerNbt.remove("nogeon_boss_slayer_ticks");
                playerNbt.remove("nogeon_boss_slayer_plus");
            } else {
                playerNbt.putInt("nogeon_boss_slayer_ticks", slayerTicks);
            }
        }
        if (playerNbt.contains("nogeon_steel_guard_ticks")) {
            int guardTicks = playerNbt.getInt("nogeon_steel_guard_ticks") - 20;
            if (guardTicks <= 0) {
                playerNbt.remove("nogeon_steel_guard_ticks");
                playerNbt.remove("nogeon_steel_guard_plus");
            } else {
                playerNbt.putInt("nogeon_steel_guard_ticks", guardTicks);
            }
        }
        if (playerNbt.contains("nogeon_immunity_ticks")) {
            int immunityTicks = playerNbt.getInt("nogeon_immunity_ticks") - 20;
            if (immunityTicks <= 0) {
                playerNbt.remove("nogeon_immunity_ticks");
            } else {
                playerNbt.putInt("nogeon_immunity_ticks", immunityTicks);
                // 절대 면역 활성화 상태: 모든 해로운 디버프 즉시 정화
                List<net.minecraft.world.effect.MobEffect> toRemove = new ArrayList<>();
                for (MobEffectInstance instance : player.getActiveEffects()) {
                    if (!instance.getEffect().isBeneficial()) {
                        toRemove.add(instance.getEffect());
                    }
                }
                for (net.minecraft.world.effect.MobEffect effect : toRemove) {
                    player.removeEffect(effect);
                }
            }
        }

        // 대지의 숨결 (최대 체력 퍼센트 증가) 관리
        net.minecraft.world.entity.ai.attributes.AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (playerNbt.contains("nogeon_heart_breath_ticks")) {
            int breathTicks = playerNbt.getInt("nogeon_heart_breath_ticks") - 20;
            if (breathTicks <= 0) {
                playerNbt.remove("nogeon_heart_breath_ticks");
                playerNbt.remove("nogeon_heart_breath_level");
                playerNbt.remove("nogeon_heart_breath_plus");
                if (maxHealthAttr != null && maxHealthAttr.getModifier(HEART_BREATH_UUID) != null) {
                    maxHealthAttr.removeModifier(HEART_BREATH_UUID);
                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                }
            } else {
                playerNbt.putInt("nogeon_heart_breath_ticks", breathTicks);
                if (maxHealthAttr != null && maxHealthAttr.getModifier(HEART_BREATH_UUID) == null) {
                    int recipeLevel = playerNbt.getInt("nogeon_heart_breath_level");
                    if (recipeLevel <= 0) recipeLevel = 1;
                    double basePercent = 0.10D + recipeLevel * 0.03D;
                    if (playerNbt.getBoolean("nogeon_heart_breath_plus")) {
                        basePercent *= 1.5D;
                    }
                    double percent = Math.min(playerNbt.getBoolean("nogeon_heart_breath_plus") ? 0.90D : 0.60D, basePercent);
                    maxHealthAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        HEART_BREATH_UUID, "Heart Breath Buff", percent, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE));
                }
            }
        } else {
            if (maxHealthAttr != null && maxHealthAttr.getModifier(HEART_BREATH_UUID) != null) {
                maxHealthAttr.removeModifier(HEART_BREATH_UUID);
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }

        // 요리사 [숙성] 인벤토리 음식 에이징 연산 (60초 주기 = 1200틱)
        if (player.tickCount % 1200 == 0 && profile.selectedJob() == JobType.COOK) {
            int chefsSnackLevel = profile.job(JobType.COOK).nodeLevel(SkillNode.COOK_CHEFS_SNACK);
            if (chefsSnackLevel > 0) {
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (!stack.isEmpty() && stack.isEdible() && stack.hasTag()) {
                        CompoundTag stackTag = stack.getTag();
                        if (stackTag.contains("CookedByPlayer")) {
                            int currentLevel = stackTag.getInt("AgeingLevel");
                            if (currentLevel < chefsSnackLevel) {
                                int currentTicks = stackTag.getInt("AgeingTicks") + 1200;
                                if (currentTicks >= 1200) {
                                    currentLevel++;
                                    stackTag.putInt("AgeingLevel", currentLevel);
                                    stackTag.putInt("AgeingTicks", 0);
                                    
                                    String originalName = stackTag.getString("OriginalCookName");
                                    if (originalName.isEmpty()) {
                                        originalName = stack.getHoverName().getString();
                                        stackTag.putString("OriginalCookName", originalName);
                                    }
                                    
                                    String prefix = "§6[숙성 " + currentLevel + "단계] §r";
                                    String customNameJson = Component.Serializer.toJson(Component.literal(prefix + originalName));
                                    stack.getOrCreateTagElement("display").putString("Name", customNameJson);
                                    
                                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.4F, 1.2F);
                                } else {
                                    stackTag.putInt("AgeingTicks", currentTicks);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void onItemFished(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getDrops().isEmpty()) {
            ItemStack rod = findFishingRod(player);
            if (!rod.isEmpty()) {
                int level = SmithingService.level(rod);
                
                // 1. Durability efficiency
                double thriftVal = SmithEvents.reforgeValue(rod, "durability_thrift");
                double ignoreChance = (level * 0.04D) + thriftVal;
                if (player.getRandom().nextDouble() < ignoreChance) {
                    event.damageRodBy(0);
                }
                
                // 2. Rare fish finder
                double rareFinderVal = SmithEvents.reforgeValue(rod, "rare_fish_finder");
                if (rareFinderVal > 0.0D && player.getRandom().nextDouble() < rareFinderVal) {
                    ItemStack firstDrop = event.getDrops().get(0);
                    if (firstDrop.is(Items.COD) || firstDrop.is(Items.SALMON)) {
                        Item rareFish = player.getRandom().nextBoolean() ? Items.PUFFERFISH : Items.TROPICAL_FISH;
                        event.getDrops().set(0, new ItemStack(rareFish));
                        player.displayClientMessage(Component.literal("§b🎣 [희귀 물고기 감지] §f재련 효과로 희귀한 물고기를 포착했습니다!"), true);
                    }
                }
                
                // 3. Treasure hunter
                double treasureHunterVal = SmithEvents.reforgeValue(rod, "treasure_hunter");
                if (treasureHunterVal > 0.0D && player.getRandom().nextDouble() < treasureHunterVal) {
                    ItemStack firstDrop = event.getDrops().get(0);
                    boolean isTreasure = firstDrop.is(Items.ENCHANTED_BOOK) || firstDrop.is(Items.NAME_TAG) || firstDrop.is(Items.SADDLE) || firstDrop.is(Items.NAUTILUS_SHELL) || firstDrop.is(Items.BOW);
                    if (!isTreasure) {
                        ItemStack[] treasures = new ItemStack[] {
                            new ItemStack(Items.NAME_TAG),
                            new ItemStack(Items.SADDLE),
                            new ItemStack(Items.NAUTILUS_SHELL),
                            new ItemStack(Items.BOW),
                            new ItemStack(Items.BOOK)
                        };
                        ItemStack chosenTreasure = treasures[player.getRandom().nextInt(treasures.length)];
                        if (chosenTreasure.is(Items.BOW)) {
                            chosenTreasure.setDamageValue(player.getRandom().nextInt(chosenTreasure.getMaxDamage()));
                        }
                        event.getDrops().set(0, chosenTreasure);
                        player.displayClientMessage(Component.literal("§b🎣 [보물 사냥꾼] §f재련 효과로 깊은 물속의 보물을 낚아 올렸습니다!"), true);
                    }
                }
                
                // 4. Double catch
                double doubleCatchChance = SmithEvents.reforgeValue(rod, "double_catch");
                if (doubleCatchChance > 0.0D && player.getRandom().nextDouble() < doubleCatchChance) {
                    if (!event.getDrops().isEmpty()) {
                        ItemStack catchCopy = event.getDrops().get(0).copy();
                        event.getDrops().add(catchCopy);
                        player.displayClientMessage(Component.literal("§b🎣 [더블 드랍] §f재련 효과로 수확물이 두 배가 되었습니다!"), true);
                    }
                }
            }

            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            if (profile.selectedJob() == JobType.FISHER) {
                applyFisherPerks(player, event);
                int exp = 0;
                for (ItemStack drop : event.getDrops()) {
                    exp += fisherCatchExp(drop);
                }
                int awardedExp = Math.max(10, exp);
                addExp(player, JobType.FISHER, awardedExp);
                grantActivityCredits(profile, state, JobType.FISHER, Math.max(35L, awardedExp * 3L));
            }
        }
    }

    private static boolean isCreateModItem(String itemId) {
        return itemId.startsWith("create:")
            || itemId.startsWith("create_dd:")
            || itemId.startsWith("createaddition:")
            || itemId.startsWith("create_new_age:")
            || itemId.startsWith("createdieselgenerators:")
            || itemId.startsWith("create_enchantment_industry:")
            || itemId.startsWith("create_hypertube:")
            || itemId.startsWith("create_sabers:")
            || itemId.startsWith("create_jetpack:");
    }

    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack crafted = event.getCrafting();
            if (!crafted.isEmpty()) {
                net.minecraft.world.Container craftMatrix = event.getInventory();
                boolean fromShop = false;
                if (craftMatrix != null) {
                    for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
                        ItemStack stack = craftMatrix.getItem(i);
                        if (!stack.isEmpty() && com.nogeon.economyland.shop.ShopItemProtection.isShopPurchased(stack)) {
                            fromShop = true;
                            break;
                        }
                    }
                }
                if (fromShop) {
                    com.nogeon.economyland.shop.ShopItemProtection.markPurchased(crafted);
                }

                String itemId = BuiltInRegistries.ITEM.getKey(crafted.getItem()).toString();
                int amount = crafted.getCount();
                
                boolean isCreate = isCreateModItem(itemId);
                if (isCreate) {
                    if (itemId.endsWith("_block") || itemId.endsWith("_ingot") || itemId.endsWith("_nugget") || itemId.endsWith("_sheet") || itemId.contains("raw_")) {
                        isCreate = false;
                    }
                }
                boolean isAmmo = itemId.startsWith("tacz:") && itemId.contains("ammo");
                
                if (isCreate || isAmmo) {
                    EconomyState state = EconomyState.get(player.server);
                    PlayerProfile profile = state.profile(player.getUUID());
                    if (profile.selectedJob() == JobType.ENGINEER) {
                        int exp = isCreate ? (5 * amount) : (1 * amount);
                        int credits = isCreate ? (25 * amount) : (5 * amount);
                        
                        // ENGINEER_PROCESS_OPTIMIZATION passive effect (Lv.75 large node)
                        int processOptLevel = profile.job(JobType.ENGINEER).nodeLevel(SkillNode.ENGINEER_PROCESS_OPTIMIZATION);
                        if (processOptLevel > 0) {
                            double refundChance = processOptLevel * 10.0D; // 10% per level, up to 100% at level 10
                            if (player.getRandom().nextDouble() * 100.0D < refundChance) {
                                net.minecraft.world.Container matrix = event.getInventory();
                                java.util.List<ItemStack> ingredients = new java.util.ArrayList<>();
                                for (int i = 0; i < matrix.getContainerSize(); i++) {
                                    ItemStack inStack = matrix.getItem(i);
                                    if (!inStack.isEmpty()) {
                                        ingredients.add(inStack.copy());
                                    }
                                }
                                if (!ingredients.isEmpty()) {
                                    ItemStack refund = ingredients.get(player.getRandom().nextInt(ingredients.size()));
                                    refund.setCount(1);
                                    ExtendedInventoryDelivery.giveOrDrop(player, refund);
                                    player.displayClientMessage(Component.literal("§6[공정 최적화] §f설계 최적화! 제작 재료 일부(" + refund.getHoverName().getString() + ")를 환급받았습니다!"), true);
                                    player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.4F, 1.6F);
                                }
                            }
                        }
                        
                        addExp(player, JobType.ENGINEER, exp);
                        profile.addCredits(credits);
                        state.setDirty();
                        player.displayClientMessage(Component.literal("§6[공학 활동] §f제작 보너스: §a+" + exp + " EXP§f / §e+" + credits + " C§f를 획득했습니다!"), true);
                    }
                }
            }
        }

        if (event.getEntity() instanceof ServerPlayer player && event.getCrafting().isEdible()) {
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            if (profile.selectedJob() != JobType.COOK) {
                return;
            }
            ItemStack cookedFood = event.getCrafting();
            if (!isCookCraftExperienceEligible(cookedFood, event.getInventory())) {
                return;
            }
            applyCookPerks(player, cookedFood, event);

            int ingredientCount = 0;
            boolean hasPlusIngredient = false;
            net.minecraft.world.Container matrix = event.getInventory();
            for (int i = 0; i < matrix.getContainerSize(); i++) {
                ItemStack stack = matrix.getItem(i);
                if (!stack.isEmpty()) {
                    ingredientCount++;
                    if (stack.hasTag() && stack.getTag().getBoolean("nogeon_plus_grade")) {
                        hasPlusIngredient = true;
                    }
                }
            }
            int exp = calculateCookExp(player, cookedFood, ingredientCount, hasPlusIngredient);
            addExp(player, JobType.COOK, exp);
        }
    }

    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSmelting().isEdible()) {
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            if (profile.selectedJob() != JobType.COOK) {
                return;
            }
            ItemStack cookedFood = event.getSmelting();
            applyCookPerks(player, cookedFood, null);
            
            // Smelting (화로/훈연기)는 보통 재료가 1개이며 태그가 유실되므로 기본 배율 적용
            int exp = calculateCookExp(player, cookedFood, 1, false);
            addExp(player, JobType.COOK, exp);
        }
    }

    private static int calculateCookExp(ServerPlayer player, ItemStack food, int ingredientCount, boolean hasPlusIngredient) {
        net.minecraft.world.food.FoodProperties foodProps = food.getItem().getFoodProperties(food, player);
        int nutrition = foodProps != null ? foodProps.getNutrition() : 1;
        float saturation = foodProps != null ? foodProps.getSaturationModifier() : 0.1F;

        // 공식: 기본(5) + (허기*3) + (포만감*10) + (재료수*8)
        double exp = 5.0 + (nutrition * 3.0) + (saturation * 10.0) + (ingredientCount * 8.0);
        exp *= itemRarityMultiplier(food);
        
        if (hasPlusIngredient) {
            exp *= 2.5; // 대지의 기적(+) 재료 사용 시 보너스
        }
        
        return (int) Math.round(exp);
    }

    private static boolean isCookCraftExperienceEligible(ItemStack output, net.minecraft.world.Container matrix) {
        if (isFoodStorageItem(output) || isSimpleRawFoodIngredient(output)) {
            return false;
        }
        for (int i = 0; i < matrix.getContainerSize(); i++) {
            ItemStack ingredient = matrix.getItem(i);
            if (!ingredient.isEmpty() && isFoodStorageItem(ingredient)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFoodStorageItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        return containsAny(path, "crate", "box", "basket", "sack", "bag", "bale", "compressed", "storage_block")
            || path.endsWith("_block");
    }

    private static boolean isSimpleRawFoodIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        return path.equals("carrot")
            || path.equals("apple")
            || path.equals("beetroot")
            || path.equals("potato")
            || path.equals("melon_slice")
            || path.equals("sweet_berries")
            || path.equals("glow_berries")
            || path.equals("tomato")
            || path.equals("onion")
            || path.equals("cabbage")
            || path.equals("corn")
            || path.equals("rice");
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        clearBleedingData(event.getEntity().getPersistentData());

        if (event.getEntity() instanceof com.nogeon.economyland.entity.ScrapDroneEntity drone) {
            drone.getOwnerUuid().ifPresent(ownerUuid -> {
                net.minecraft.world.entity.player.Player owner = drone.level().getPlayerByUUID(ownerUuid);
                if (owner instanceof ServerPlayer sPlayer) {
                    preserveDroneStateOnBreak(sPlayer, drone);
                    sPlayer.getPersistentData().putBoolean("nogeon_engineer_drone_broken", true);
                    sPlayer.getPersistentData().remove("nogeon_engineer_kinetic_boost_ticks");
                    sPlayer.getPersistentData().remove("nogeon_engineer_kinetic_boost_level");
                    sPlayer.displayClientMessage(Component.literal("§c[오토 스크랩 드론] 드론이 파괴되었습니다! 복구하려면 수리해야 합니다."), false);
                    sPlayer.level().playSound(null, sPlayer.getX(), sPlayer.getY(), sPlayer.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
                }
            });
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        triggerDroneExpression(player, 3, 40); // Happy expression on mob kill

        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        LivingEntity victim = event.getEntity();
        boolean isEnemy = victim instanceof Enemy || victim instanceof WitherBoss || victim instanceof EnderDragon;
        boolean isAnimal = isFarmAnimal(victim);

        if (isEnemy || isAnimal) {
            if (profile.selectedJob() == JobType.HUNTER) {
                applyHunterPerks(player, victim);
                addExp(player, JobType.HUNTER, hunterKillExp(victim, isAnimal));
                dropCreditsFromMob(player, victim);
            }
        }

        if (profile.selectedJob() == JobType.HUNTER && victim.getStringUUID().equals(profile.hunterPreyMarkedUUID())) {
                int markLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_WILD_STEP);
                if (markLevel > 0) {
                    int duration = Math.min(24, 4 + markLevel * 2) * 20;
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 1, false, true));
                    player.displayClientMessage(Component.literal("§4[사냥감의 표식] §f표식 대상을 처치하여 이동속도 및 공격속도가 대폭 상승했습니다!"), true);
                }

                profile.setHunterPreyMarkedUUID("");
                state.setDirty();
                int quickDrawLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_QUICK_DRAW);
                int radius = Math.min(42, 12 + quickDrawLevel * 3);
                com.nogeon.economyland.network.ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new com.nogeon.economyland.network.SyncHunterAbilityPacket(profile.hunterSenseActive(), radius, ""));
            }
        }

    private static void preserveDroneStateOnBreak(ServerPlayer owner, com.nogeon.economyland.entity.ScrapDroneEntity drone) {
        net.minecraft.nbt.CompoundTag data = owner.getPersistentData();
        String droneName = drone.getDroneName();
        if (droneName != null && !droneName.isBlank()) {
            data.putString("nogeon_engineer_drone_name", droneName);
        }
        data.putInt("nogeon_engineer_drone_charge", Math.max(0, drone.getCharge()));

        if (drone.hasUpgradeInventory()) {
            data.putBoolean("nogeon_engineer_drone_upgrade_inventory", true);
            if (data.getInt("nogeon_engineer_drone_upgrade_inventory_level") <= 0) {
                data.putInt("nogeon_engineer_drone_upgrade_inventory_level", 1);
            }
        }
        preserveDroneUpgrade(data, "transmitter", drone.getTransLevel(), drone.hasUpgradeTransmitter());
        preserveDroneUpgrade(data, "booster", drone.getBoostLevel(), drone.hasUpgradeBooster());
        preserveDroneUpgrade(data, "sensor", drone.getSensorLevel(), drone.getSensorLevel() > 0);
        preserveDroneUpgrade(data, "grabber", drone.getGrabberLevel(), drone.getGrabberLevel() > 0);
    }

    private static void preserveDroneUpgrade(net.minecraft.nbt.CompoundTag data, String key, int entityLevel, boolean enabled) {
        if (!enabled && entityLevel <= 0) {
            return;
        }
        String boolKey = "nogeon_engineer_drone_upgrade_" + key;
        String levelKey = boolKey + "_level";
        data.putBoolean(boolKey, true);
        if (data.getInt(levelKey) <= 0) {
            data.putInt(levelKey, Math.max(1, entityLevel));
        }
    }

    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
        if (profile.selectedJob() != JobType.FARMER) {
            return;
        }
        JobProgress progress = profile.job(JobType.FARMER);
        int protectionLevel = progress.nodeLevel(SkillNode.secondaryEffectNode(JobType.FARMER));
        if (protectionLevel <= 0) {
            return;
        }

        if (roll(player, Math.min(80, 30 + protectionLevel * 5))) {
            event.setCanceled(true);
        }
    }

    public static void grantJobExp(ServerPlayer player, JobType job, int amount) {
        addExp(player, job, amount);
    }

    private static void grantActivityCredits(PlayerProfile profile, EconomyState state, JobType job, long amount) {
        if (amount <= 0 || profile.selectedJob() != job) {
            return;
        }
        profile.addCredits(amount);
        state.setDirty();
    }

    public static void addExp(ServerPlayer player, JobType job, int amount) {
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        if (profile.selectedJob() != job) {
            return;
        }
        JobProgress progress = profile.job(job);
        int beforeLevel = progress.level();
        int baseAmount = Math.max(0, Math.round(amount * jobExpMultiplier(job)));
        int adjustedAmount = baseAmount + baseAmount * progress.bonusPercent(SkillNodeStat.EXP_GAIN) / 100;
        progress.addExp(adjustedAmount);
        state.setDirty();
        if (progress.level() > beforeLevel) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.nogeon_economy_land.job.level_up",
                    net.minecraft.network.chat.Component.translatable("job.nogeon_economy_land." + job.id()),
                    progress.level()),
                false
            );
        }
    }

    private static float jobExpMultiplier(JobType job) {
        return switch (job) {
            case FISHER -> 2.0F;
            default -> 1.0F;
        };
    }

    private static int hunterKillExp(LivingEntity victim, boolean isAnimal) {
        double health = Math.max(1.0D, victim.getMaxHealth());
        double baseHealth = Math.min(400.0D, health);
        double overflowHealth = Math.max(0.0D, health - 400.0D);
        double expValue = (isAnimal ? 3.0D : 6.0D)
            + Math.sqrt(baseHealth) * (isAnimal ? 1.2D : 2.2D)
            + Math.pow(overflowHealth, 0.40D) * (isAnimal ? 1.5D : 12.0D);
        int exp = (int) Math.round(expValue);
        exp = Math.max(isAnimal ? 4 : 8, exp);
        if (isBossEntity(victim)) {
            return exp * 4;
        }
        return exp;
    }

    private static boolean stopMismatchedActiveSkills(ServerPlayer player, PlayerProfile profile) {
        boolean changed = false;
        JobType selected = profile.selectedJob();
        if (selected != JobType.MINER && (profile.minerBodyActive() || profile.minerEyeActive())) {
            profile.setMinerBodyActive(false);
            profile.setMinerEyeActive(false);
            changed = true;
            com.nogeon.economyland.network.ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new com.nogeon.economyland.network.SyncMinerAbilityPacket(false, false, 0));
        }
        if (selected != JobType.HUNTER && (profile.hunterSenseActive() || !profile.hunterPreyMarkedUUID().isEmpty())) {
            clearHunterSenseGlow(player, 42);
            profile.setHunterSenseActive(false);
            profile.setHunterSenseTicks(0);
            profile.setHunterPreyMarkedUUID("");
            changed = true;
            com.nogeon.economyland.network.ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new com.nogeon.economyland.network.SyncHunterAbilityPacket(false, 0, ""));
        }
        if (selected != JobType.FISHER) {
            CompoundTag nbt = player.getPersistentData();
            boolean hasData = nbt.contains("nogeon_hotspot_x") || nbt.contains("nogeon_hotspot_timer") || nbt.getInt("nogeon_fisher_flow_gauge") > 0;
            if (hasData) {
                nbt.remove("nogeon_hotspot_x");
                nbt.remove("nogeon_hotspot_y");
                nbt.remove("nogeon_hotspot_z");
                nbt.remove("nogeon_hotspot_timer");
                nbt.putInt("nogeon_fisher_flow_gauge", 0);
            }
            boolean removedZones = FISHERY_ZONES.entrySet().removeIf(entry -> entry.getValue().owner.equals(player.getUUID()));
            if (hasData || removedZones) {
                syncFisherDataToPlayer(player);
            }
        }
        if (selected != JobType.ENGINEER) {
            CompoundTag nbt = player.getPersistentData();
            boolean hasChange = false;
            if (nbt.getBoolean("nogeon_engineer_overclock_active")) {
                nbt.remove("nogeon_engineer_overclock_active");
                player.removeEffect(MobEffects.DIG_SPEED);
                player.removeEffect(MobEffects.MOVEMENT_SPEED);
                hasChange = true;
            }
            if (nbt.contains("nogeon_engineer_kinetic_boost_ticks")) {
                nbt.remove("nogeon_engineer_kinetic_boost_ticks");
                hasChange = true;
            }
            var reachAttr = player.getAttribute(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());
            if (reachAttr != null && reachAttr.getModifier(ENGINEER_REACH_UUID) != null) {
                reachAttr.removeModifier(ENGINEER_REACH_UUID);
                hasChange = true;
            }
            if (hasChange) {
                changed = true;
            }
        }
        return changed;
    }

    private static void applyFarmerPerks(ServerPlayer player, BlockState originState, BlockPos originPos, boolean isAlreadyHarvested) {
        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
        JobProgress progress = profile.job(JobType.FARMER);
        int chanceBonus = progress.bonusPercent(SkillNodeStat.SPECIAL_CHANCE);
        ServerLevel level = player.serverLevel();

        // 1. 작은 노드: 수확량 증가 패시브 (FARMER_FIELD_ROUTINE)
        int fieldRoutineLevel = progress.nodeLevel(SkillNode.FARMER_FIELD_ROUTINE);
        int dropMultiplier = 1;
        if (fieldRoutineLevel > 0 && roll(player, scaledChance(fieldRoutineLevel, 1.5D, 45.0D))) {
            dropMultiplier++;
        }

        // 2. 100레벨 [대지의 기적] 고급 부산물 (+) 획득 확률 검사
        int earthMiracleLevel = progress.nodeLevel(SkillNode.FARMER_EARTH_MIRACLE);
        boolean makePlusGrade = false;
        if (profile.selectedJob() == JobType.FARMER && earthMiracleLevel > 0) {
            double plusChance = 3.0D + earthMiracleLevel * 2.0D; // 5% / 7% / 9%
            if (player.getRandom().nextDouble() * 100.0D < plusChance) {
                makePlusGrade = true;
            }
        }

        // 3. 75레벨 [인간 트랙터] 연쇄 수확
        int tractorLevel = progress.nodeLevel(SkillNode.FARMER_FIELD_SNACK);
        boolean isFarmer = profile.selectedJob() == JobType.FARMER;
        
        if (isFarmer && tractorLevel > 0 && isFarmerCropBlock(originState)) {
            java.util.List<BlockPos> blocksToHarvest = new java.util.ArrayList<>();
            java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
            java.util.Set<BlockPos> visited = new java.util.HashSet<>();
            
            queue.add(originPos);
            visited.add(originPos);
            
            int maxHarvest = 9 + tractorLevel * 6; // 15 / 21 / 27개
            net.minecraft.world.level.block.Block cropBlock = originState.getBlock();
            
            int maxHarvestLimit = Math.min(maxHarvest, 45);
            while (!queue.isEmpty() && blocksToHarvest.size() < maxHarvestLimit) {
                BlockPos current = queue.poll();
                blocksToHarvest.add(current);
                
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            BlockPos neighbor = current.offset(dx, dy, dz);
                            if (visited.contains(neighbor)) continue;
                            visited.add(neighbor);
                            
                            BlockState nState = level.getBlockState(neighbor);
                            if (nState.getBlock() == cropBlock && isFarmerCropBlock(nState) && isCropMaxAge(nState)) {
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
            
            int harvestedCount = 0;
            for (BlockPos pos : blocksToHarvest) {
                if (pos.equals(originPos) && isAlreadyHarvested) {
                    // Apply double drops / plus grade for already harvested origin block
                    if (dropMultiplier > 1 || makePlusGrade) {
                        java.util.List<ItemStack> drops = Block.getDrops(originState, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
                        for (ItemStack drop : drops) {
                            if (drop.isEmpty()) continue;
                            ItemStack giveStack = drop.copy();
                            if (dropMultiplier > 1) {
                                giveStack.setCount(giveStack.getCount() * (dropMultiplier - 1));
                            } else {
                                giveStack.setCount(1);
                            }
                            if (makePlusGrade) {
                                applyPlusGrade(giveStack);
                            }
                            Block.popResource(level, pos, giveStack);
                        }
                    }
                    BlockState currentState = level.getBlockState(pos);
                    if (currentState.isAir() || currentState.getBlock() != originState.getBlock()) {
                        replantCrop(player, progress, level, originState, pos, originState.getBlock());
                    }
                    harvestedCount++;
                    continue;
                }

                BlockState cropState = level.getBlockState(pos);
                if (!isFarmerCropBlock(cropState) || !isCropMaxAge(cropState)) continue;
                
                java.util.List<ItemStack> drops = Block.getDrops(cropState, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
                for (ItemStack drop : drops) {
                    if (drop.isEmpty()) continue;
                    
                    ItemStack giveStack = drop.copy();
                    int count = giveStack.getCount() * dropMultiplier;
                    if (player.hasEffect(MobEffects.LUCK) && player.getRandom().nextDouble() < 0.15D) {
                        count *= 2;
                    }
                    giveStack.setCount(count);
                    
                    if (makePlusGrade) {
                        applyPlusGrade(giveStack);
                    }
                    
                    ExtendedInventoryDelivery.giveOrDrop(player, giveStack);
                }
                
                level.destroyBlock(pos, false, player);
                
                replantCrop(player, progress, level, cropState, pos, cropState.getBlock());
                harvestedCount++;
            }
            
            level.playSound(null, originPos.getX(), originPos.getY(), originPos.getZ(),
                SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, originPos.getX() + 0.5D, originPos.getY() + 0.5D, originPos.getZ() + 0.5D, 15, 1.0D, 0.2D, 1.0D, 0.05D);
            player.displayClientMessage(Component.literal("§a🚜 [인간 트랙터] §f작물 §e" + harvestedCount + "개§f를 연쇄 수확하고 비옥한 토지에 자동 재심기를 수행했습니다!"), true);
            
            if (harvestedCount > 1) {
                addExp(player, JobType.FARMER, (harvestedCount - 1) * Math.max(3, farmerCropExp(originState) / 3));
            }
            
            chargeNearbyScarecrow(player, originPos, harvestedCount);
        } else {
            if (isAlreadyHarvested) {
                if (dropMultiplier > 1) {
                    java.util.List<ItemStack> drops = Block.getDrops(originState, level, originPos, level.getBlockEntity(originPos), player, player.getMainHandItem());
                    for (ItemStack drop : drops) {
                        if (drop.isEmpty()) continue;
                        ItemStack bonus = drop.copy();
                        bonus.setCount(bonus.getCount() * (dropMultiplier - 1));
                        if (makePlusGrade) {
                            applyPlusGrade(bonus);
                        }
                        Block.popResource(level, originPos, bonus);
                    }
                } else if (makePlusGrade) {
                    java.util.List<ItemStack> drops = Block.getDrops(originState, level, originPos, level.getBlockEntity(originPos), player, player.getMainHandItem());
                    for (ItemStack drop : drops) {
                        if (drop.isEmpty()) continue;
                        ItemStack bonus = drop.copy();
                        bonus.setCount(1);
                        applyPlusGrade(bonus);
                        Block.popResource(level, originPos, bonus);
                        break;
                    }
                }
                BlockState currentState = level.getBlockState(originPos);
                if (currentState.isAir() || currentState.getBlock() != originState.getBlock()) {
                    replantCrop(player, progress, level, originState, originPos, originState.getBlock());
                }
            } else {
                java.util.List<ItemStack> drops = Block.getDrops(originState, level, originPos, level.getBlockEntity(originPos), player, player.getMainHandItem());
                for (ItemStack drop : drops) {
                    if (drop.isEmpty()) continue;
                    ItemStack giveStack = drop.copy();
                    int count = giveStack.getCount() * dropMultiplier;
                    if (player.hasEffect(MobEffects.LUCK) && player.getRandom().nextDouble() < 0.15D) {
                        count *= 2;
                    }
                    giveStack.setCount(count);
                    
                    if (makePlusGrade) {
                        applyPlusGrade(giveStack);
                    }
                    
                    ExtendedInventoryDelivery.giveOrDrop(player, giveStack);
                }
                level.destroyBlock(originPos, false, player);

                if (isFarmerCropBlock(originState)) {
                    replantCrop(player, progress, level, originState, originPos, originState.getBlock());
                }
            }
            
            int bountifulLevel = progress.nodeLevel(SkillNode.FARMER_BOUNTIFUL_HARVEST);
            if (isFarmer && bountifulLevel > 0 && roll(player, Math.min(60, 8 + bountifulLevel * 5))) {
                growNearbyCropsBountiful(level, originState, originPos, bountifulLevel);
                player.displayClientMessage(Component.literal("§e✨ [풍요로운 손길] §f대지의 은총으로 주변 작물들이 한 번에 성장합니다!"), true);
            }
            
            chargeNearbyScarecrow(player, originPos, 1);
        }
    }

    private static void applyMinerPerks(ServerPlayer player, BlockState state, BlockPos pos) {
        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
        JobProgress progress = profile.job(JobType.MINER);
        int chanceBonus = progress.bonusPercent(SkillNodeStat.SPECIAL_CHANCE);
        
        // 작은노드: 더블 드랍 확률 증가 (miner_ore_sense)
        int oreSenseLevel = progress.nodeLevel(SkillNode.MINER_ORE_SENSE);
        if (oreSenseLevel > 0 && roll(player, Math.min(60, 8 + chanceBonus / 2 + scaledChance(oreSenseLevel, 1.5D, 45.0D)))) {
            spawnExtraDrop(player, state, pos);
        }

        if (player.hasEffect(MobEffects.LUCK) && player.getRandom().nextDouble() < 0.15D) {
            spawnExtraDrop(player, state, pos);
        }

        // 작은노드: 내구도 효율 증가 (miner_fault_reading)
        int faultReadingLevel = progress.nodeLevel(SkillNode.MINER_FAULT_READING);
        if (faultReadingLevel > 0 && roll(player, Math.min(60, 10 + chanceBonus / 3 + scaledChance(faultReadingLevel, 1.5D, 45.0D)))) {
            refundDurability(player.getMainHandItem(), 1);
        }

        int prophetTreasureLevel = progress.nodeLevel(SkillNode.MINER_DEEP_BREATH);
        if (prophetTreasureLevel > 0) {
            tryDropEnhancementGem(player, state, pos, prophetTreasureLevel);
        }
    }

    private static void tryDropEnhancementGem(ServerPlayer player, BlockState state, BlockPos pos, int skillLevel) {
        boolean isOre = isOreBlock(state);
        double chance = Math.min(1.00D, 0.05D + skillLevel * 0.028D);
        if (!isOre) {
            // 암석(stone 등)의 경우 드랍 확률을 광석의 30%로 조율 (체감 획득률 대폭 상향)
            chance *= 0.10D;
        }
        if (player.getRandom().nextDouble() * 100.0D >= chance) {
            return;
        }
        ItemStack gem = rollEnhancementGem(player);
        Block.popResource(player.serverLevel(), pos, gem);
        playGemDropEffects(player, pos, gem);
    }

    private static void playGemDropEffects(ServerPlayer player, BlockPos pos, ItemStack gem) {
        ServerLevel level = player.serverLevel();
        double px = pos.getX() + 0.5D;
        double py = pos.getY() + 0.5D;
        double pz = pos.getZ() + 0.5D;
        Item item = gem.getItem();

        if (item == com.nogeon.economyland.item.ModItems.PERFECT_ENHANCEMENT_GEM.get() ||
            item == com.nogeon.economyland.item.ModItems.FLAWLESS_ENHANCEMENT_GEM.get()) {
            
            // Legendary (Perfect / Flawless)
            level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 0.8F);
            
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, px, py, pz, 35, 0.3D, 0.5D, 0.3D, 0.2D);
            level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 20, 0.2D, 0.4D, 0.2D, 0.1D);
            
            player.displayClientMessage(Component.literal("§6§l★ 잭팟! 전설적인 강화의 보석 발견! ★"), true);
            player.displayClientMessage(Component.literal("§6§l[선지자의 보물 - 전설] §f광석 깊은 곳에서 눈부시게 빛나는 §b§l[ " + gemDropName(gem) + "§b§l ]§f(을)를 발굴해냈습니다!"), false);
        } else if (item == com.nogeon.economyland.item.ModItems.ENHANCEMENT_GEM.get() ||
                   item == com.nogeon.economyland.item.ModItems.FLAWED_ENHANCEMENT_GEM.get()) {
            
            // Epic (Normal / Flawed)
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.2F, 1.2F);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
            
            level.sendParticles(ParticleTypes.WAX_ON, px, py, pz, 18, 0.25D, 0.35D, 0.25D, 0.15D);
            level.sendParticles(ParticleTypes.ENCHANT, px, py, pz, 12, 0.2D, 0.3D, 0.2D, 0.1D);
            
            player.displayClientMessage(Component.literal("§d§l✦ 희귀한 강화의 보석 발견! ✦"), true);
            player.displayClientMessage(Component.literal("§d§l[선지자의 보물 - 희귀] §f광석 속에서 희미하게 빛나는 §d§l[ " + gemDropName(gem) + "§d§l ]§f(을)를 발굴했습니다."), false);
        } else {
            // Common (Split / Cracked)
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.5F);
            
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 8, 0.15D, 0.25D, 0.15D, 0.05D);
            
            player.displayClientMessage(Component.literal("§a§l✔ 강화의 보석 발견!"), true);
            player.displayClientMessage(Component.literal("§7[선지자의 보물] §f광석 틈새에서 §7[ " + gemDropName(gem) + "§7 ]§f(을)를 캐냈습니다."), false);
        }
    }

    private static String gemDropName(ItemStack gem) {
        Item item = gem.getItem();
        if (item == com.nogeon.economyland.item.ModItems.CRACKED_ENHANCEMENT_GEM.get()) {
            return "\u00a7f\uae08\uc774\uac04 \uac15\ud654\uc758 \ubcf4\uc11d";
        }
        if (item == com.nogeon.economyland.item.ModItems.SPLIT_ENHANCEMENT_GEM.get()) {
            return "\u00a7a\uac08\ub77c\uc9c4 \uac15\ud654\uc758 \ubcf4\uc11d";
        }
        if (item == com.nogeon.economyland.item.ModItems.FLAWED_ENHANCEMENT_GEM.get()) {
            return "\u00a79\uacb0\uc810\uc774 \uc788\ub294 \uac15\ud654\uc758 \ubcf4\uc11d";
        }
        if (item == com.nogeon.economyland.item.ModItems.ENHANCEMENT_GEM.get()) {
            return "\u00a7d\uac15\ud654\uc758 \ubcf4\uc11d";
        }
        if (item == com.nogeon.economyland.item.ModItems.FLAWLESS_ENHANCEMENT_GEM.get()) {
            return "\u00a76\uacb0\uc810\uc774 \uc5c6\ub294 \uac15\ud654\uc758 \ubcf4\uc11d";
        }
        if (item == com.nogeon.economyland.item.ModItems.PERFECT_ENHANCEMENT_GEM.get()) {
            return "\u00a76\u00a7l\uc644\ubcbd\ud55c \uac15\ud654\uc758 \ubcf4\uc11d";
        }
        return gem.getHoverName().getString();
    }

    private static ItemStack rollEnhancementGem(ServerPlayer player) {
        double roll = player.getRandom().nextDouble() * 10000.0D;
        if (roll < 10.0D) return new ItemStack(com.nogeon.economyland.item.ModItems.PERFECT_ENHANCEMENT_GEM.get()); // 0.1% (이전 0.01% - 10배 버프)
        if (roll < 100.0D) return new ItemStack(com.nogeon.economyland.item.ModItems.FLAWLESS_ENHANCEMENT_GEM.get()); // 0.9% (이전 0.1% - 9배 버프)
        if (roll < 500.0D) return new ItemStack(com.nogeon.economyland.item.ModItems.ENHANCEMENT_GEM.get()); // 4.0% (이전 0.5% - 8배 버프)
        if (roll < 2000.0D) return new ItemStack(com.nogeon.economyland.item.ModItems.FLAWED_ENHANCEMENT_GEM.get()); // 15.0% (이전 2.0% - 7.5배 버프)
        if (roll < 4500.0D) return new ItemStack(com.nogeon.economyland.item.ModItems.SPLIT_ENHANCEMENT_GEM.get()); // 25.0% (이전 8.0% - 3.1배 버프)
        return new ItemStack(com.nogeon.economyland.item.ModItems.CRACKED_ENHANCEMENT_GEM.get()); // 55.0% (이전 89.39% - 금간것 도배 해소)
    }

    private static void applyCookPerks(ServerPlayer player, ItemStack cookedFood, PlayerEvent.ItemCraftedEvent event) {
        applyCookPerks(player, cookedFood, event, true);
    }

    private static void applyCookPerks(ServerPlayer player, ItemStack cookedFood, PlayerEvent.ItemCraftedEvent event, boolean grantCompletionRewards) {
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        JobProgress progress = profile.job(JobType.COOK);

        // 요리 낙인 각인
        CompoundTag nbt = cookedFood.getOrCreateTag();
        nbt.putString("CookedByPlayer", player.getName().getString());
        nbt.putString("CookedByPlayerUUID", player.getStringUUID());
        int warmMealLevel = progress.nodeLevel(SkillNode.COOK_WARM_MEAL);
        if (warmMealLevel > 0) {
            nbt.putInt("nogeon_warm_meal_level", warmMealLevel);
        }

        // 요리 제작 재료 중 [nogeon_plus_grade]가 붙은 작물이 있는지 조사 (대지의 기적 각인 효과)
        boolean hasPlusIngredient = false;
        if (event != null && event.getInventory() != null) {
            net.minecraft.world.Container craftMatrix = event.getInventory();
            for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
                ItemStack ingredient = craftMatrix.getItem(i);
                if (!ingredient.isEmpty() && ingredient.hasTag() && ingredient.getTag().getBoolean("nogeon_plus_grade")) {
                    hasPlusIngredient = true;
                    break;
                }
            }
        }
        
        if (hasPlusIngredient) {
            nbt.putBoolean("nogeon_plus_grade", true);
            // 이름 끝에 + 마킹
            String originalName = cookedFood.getHoverName().getString();
            String customNameJson = Component.Serializer.toJson(Component.literal("§d" + originalName + "+"));
            cookedFood.getOrCreateTagElement("display").putString("Name", customNameJson);
            
            // 로어 툴팁
            ListTag loreList = new ListTag();
            loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§d[대지의 기적] 최고급 요리 재료 각인"))));
            loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§6섭취 시 적용되는 모든 버프 효과/시간 50% 증폭"))));
            cookedFood.getOrCreateTagElement("display").put("Lore", loreList);
        }

        // 1. 제작 크레딧 직접 획득 패시브
        int cookLevel = progress.level();
        long creditBonus = Math.min(1000L, 50L + cookLevel * 3L);
        if (grantCompletionRewards && creditBonus > 0) {
            profile.addCredits(creditBonus);
            state.setDirty();
            player.displayClientMessage(Component.literal("§e[요리 보너스] §f요리를 완성하여 §6" + creditBonus + " C§f를 획득했습니다!"), true);
        }

        // 2. 더블 드랍 패시브 (COOK_SEASONING - 작은 노드)
        int seasoningLevel = progress.nodeLevel(SkillNode.COOK_SEASONING);
        if (grantCompletionRewards && seasoningLevel > 0 && roll(player, scaledChance(seasoningLevel, 1.5D, 45.0D))) {
            ItemStack doubleFood = cookedFood.copy();
            ExtendedInventoryDelivery.giveOrDrop(player, doubleFood);
            player.displayClientMessage(Component.literal("§d[요리 비법] §f더블 드랍이 발동하여 요리가 추가로 복사되었습니다!"), true);
        }

        // 3. 재료 소모 방지 패시브 (COOK_TASTE_MEMORY - 작은 노드)
        int tasteMemoryLevel = progress.nodeLevel(SkillNode.COOK_TASTE_MEMORY);
        if (grantCompletionRewards && event != null && tasteMemoryLevel > 0 && roll(player, scaledChance(tasteMemoryLevel, 1.5D, 45.0D))) {
            net.minecraft.world.Container craftMatrix = event.getInventory();
            for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
                ItemStack stack = craftMatrix.getItem(i);
                if (!stack.isEmpty()) {
                    stack.setCount(stack.getCount() + 1);
                }
            }
            player.displayClientMessage(Component.literal("§a[재료 보존] §f재료 보존 비법이 발동하여 요리 재료를 소모하지 않았습니다!"), true);
        }

        // 4. 25레벨 [손맛] 대성공 명품화 각인
        int heartyPortionLevel = progress.nodeLevel(SkillNode.COOK_HEARTY_PORTION);
        if (heartyPortionLevel > 0 && roll(player, Math.min(65, 15 + heartyPortionLevel * 5))) {
            nbt.putBoolean("CookGreatSuccess", true);
            nbt.putInt("HeartyLevel", heartyPortionLevel);
            
            // 이름 앞에 [명품] 또는 [손맛]을 이쁘게 입혀주며 닉네임 각인
            String originalName = cookedFood.getHoverName().getString();
            String customNameJson = Component.Serializer.toJson(Component.literal("§e" + player.getName().getString() + "의 손맛이 깃든 " + originalName));
            cookedFood.getOrCreateTagElement("display").putString("Name", customNameJson);
            
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.8F);
            player.displayClientMessage(Component.literal("§e✨ [요리 대성공] §f손맛이 가득 깃든 특별한 요리가 탄생했습니다!"), true);
        }

        // 5. 100레벨 [나만의 레시피] 특수 버프 각인
        int masterRecipeLevel = progress.nodeLevel(SkillNode.COOK_MASTER_RECIPE);
        if (masterRecipeLevel > 0 && !profile.cookRecipeBuffs().isEmpty()) {
            ListTag recipeBuffsNbt = new ListTag();
            for (String buff : profile.cookRecipeBuffs()) {
                recipeBuffsNbt.add(net.minecraft.nbt.StringTag.valueOf(buff));
            }
            nbt.put("RecipeBuffs", recipeBuffsNbt);
            nbt.putInt("MasterRecipeLevel", masterRecipeLevel);
            
            player.displayClientMessage(Component.literal("§5🧪 [나만의 레시피] §f설정해 둔 나만의 특수 비법 버프들이 음식에 완벽히 각인되었습니다!"), true);
        }
    }

    private static void markCookOutputsFirstHolder(ServerPlayer player, PlayerProfile profile) {
        int marked = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && marked < 4; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isCookOutputWithoutFirstHolder(stack)) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putString("CookFirstHolderUUID", player.getStringUUID());
                tag.putString("CookFirstHolderName", player.getName().getString());
                tag.putBoolean("CookFirstHolderWasCook", profile.selectedJob() == JobType.COOK);
                if (profile.selectedJob() == JobType.COOK) {
                    applyCookPerks(player, stack, null, false);
                    int exp = calculateCookExp(player, stack, 1, stack.hasTag() && stack.getTag().getBoolean("nogeon_plus_grade"));
                    // 직접 제작한 것이 아니므로 절반의 경험치만 획득 (최소 2)
                    addExp(player, JobType.COOK, Math.max(2, exp / 2));
                }
                marked++;
            }
        }
    }

    private static boolean isCookOutputWithoutFirstHolder(ItemStack stack) {
        if (stack.isEmpty() || stack.hasTag() && (stack.getTag().contains("CookFirstHolderUUID") || stack.getTag().contains("CookedByPlayer"))) {
            return false;
        }
        if (isFoodStorageItem(stack) || isSimpleRawFoodIngredient(stack)) {
            return false;
        }
        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("wine") || path.contains("juice") || path.contains("cider") ||
               path.contains("mead") || path.contains("beer") || path.contains("ale") ||
               path.contains("liquor") || path.contains("tea") || path.contains("coffee") ||
               path.contains("fried") || path.contains("grilled") || path.contains("roast") ||
               path.contains("skillet") || path.contains("pan") || path.contains("omelet") ||
               path.contains("pasta") || path.contains("stew") || path.contains("soup") ||
               path.contains("meal") || path.contains("delight") || path.contains("feast") ||
               path.contains("platter") || path.contains("sandwich") || path.contains("burger") ||
               path.contains("baked") || path.contains("cooked") || path.contains("pie") ||
               path.contains("cake") || path.contains("bread") || path.contains("toast")) {
            return true;
        }
        net.minecraft.world.food.FoodProperties food = stack.getItem().getFoodProperties();
        return food != null && (food.getNutrition() >= 6 || food.getSaturationModifier() >= 0.6F);
    }

    private static void applyFisherPerks(ServerPlayer player, ItemFishedEvent event) {
        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
        JobProgress progress = profile.job(JobType.FISHER);
        boolean isFisher = profile.selectedJob() == JobType.FISHER;
        if (!isFisher) {
            return;
        }
        int chanceBonus = progress.bonusPercent(SkillNodeStat.SPECIAL_CHANCE);
        ServerLevel level = player.serverLevel();
        
        if (event.getDrops().isEmpty()) return;
        
        ItemStack mainCatch = event.getDrops().get(0).copy();
        
        // 1. 작은 노드: 낚싯대 내구도 소모 효율 증가 (FISHER_LINE_ROUTINE)
        int lineRoutineLevel = progress.nodeLevel(SkillNode.FISHER_LINE_ROUTINE);
        if (lineRoutineLevel > 0 && roll(player, scaledChance(lineRoutineLevel, 1.5D, 45.0D))) {
            refundDurability(findFishingRod(player), 1);
        }

        // 2. 작은 노드: 더블 드랍 확률 증가 (FISHER_CURRENT_READING)
        int currentReadingLevel = progress.nodeLevel(SkillNode.FISHER_CURRENT_READING);
        boolean doDoubleDrop = currentReadingLevel > 0 && roll(player, scaledChance(currentReadingLevel, 1.5D, 45.0D));
        if (doDoubleDrop) {
            ItemStack extraCatch = mainCatch.copy();
            event.getDrops().add(extraCatch);
            player.displayClientMessage(Component.literal("§b🎣 [더블 드랍] §f낚시 비법이 발동하여 수확물이 두 배가 되었습니다!"), true);
        }

        // 3. 50레벨 [숙련된 낚싯줄] 추가 보상 낚시 (FISHER_TIDAL_STEP)
        int tidalStepLevel = progress.nodeLevel(SkillNode.FISHER_TIDAL_STEP);
        
        if (isFisher && tidalStepLevel > 0) {
            double extraChance = Math.min(75.0D, 25.0D + tidalStepLevel * 5.0D);
            if (player.getRandom().nextDouble() * 100.0D < extraChance) {
                ItemStack additionalReward = rollAdditionalFisherReward(player, tidalStepLevel);
                if (!additionalReward.isEmpty()) {
                    ExtendedInventoryDelivery.giveOrDrop(player, additionalReward);
                    player.displayClientMessage(Component.literal("§b🎣 [숙련된 낚싯줄] §f손맛이 한 번 더 찌릿하게 흔들려 추가 보상을 낚았습니다!"), false);
                }
            }
        }

        // 4. 75레벨 [미끼 뿌리기] 흐름 게이지 연동 (FISHER_CALM_WATER)
        int calmWaterLevel = progress.nodeLevel(SkillNode.FISHER_CALM_WATER);
        if (isFisher && calmWaterLevel > 0) {
            CompoundTag pNbt = player.getPersistentData();
            int currentGauge = pNbt.getInt("nogeon_fisher_flow_gauge");
            if (currentGauge < 100) {
                int add = Math.min(50, 10 + calmWaterLevel * 4);
                int nextGauge = Math.min(100, currentGauge + add);
                pNbt.putInt("nogeon_fisher_flow_gauge", nextGauge);
                
                if (nextGauge >= 100) {
                    player.displayClientMessage(Component.literal("§d🌊 [미끼 뿌리기 Ready] §f흐름 게이지가 100% 채워졌습니다! §eZ 키§f를 물에 대고 클릭하여 어장을 만드세요!"), false);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.6F);
                } else {
                    player.displayClientMessage(Component.literal("§d🌊 [흐름 게이지] §b" + nextGauge + "% §7(+" + add + "%)"), true);
                }
                com.nogeon.economyland.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.nogeon.economyland.network.SyncFisherDataPacket(nextGauge));
            }
        }

        // 5. 100레벨 [보물 찾기] 심해 크레이트 가차 획득 (FISHER_TREASURE_HUNT)
        int treasureHuntLevel = progress.nodeLevel(SkillNode.FISHER_TREASURE_HUNT);
        if (isFisher && treasureHuntLevel > 0) {
            double crateChance = Math.min(35.0D, 6.0D + treasureHuntLevel * 5.8D);
            if (player.getRandom().nextDouble() * 100.0D < crateChance) {
                ItemStack crate = generateDeepseaCrate(player, treasureHuntLevel);
                if (event.getHookEntity() != null) {
                    double hx = event.getHookEntity().getX();
                    double hy = event.getHookEntity().getY();
                    double hz = event.getHookEntity().getZ();
                    
                    net.minecraft.world.entity.item.ItemEntity crateEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level, hx, hy, hz, crate
                    );
                    
                    double dx = player.getX() - hx;
                    double dy = player.getY() - hy;
                    double dz = player.getZ() - hz;
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    
                    if (dist > 0.1D) {
                        crateEntity.setDeltaMovement(
                            dx * 0.12D,
                            dy * 0.12D + Math.sqrt(dist) * 0.08D + 0.15D,
                            dz * 0.12D
                        );
                    }
                    crateEntity.setNoPickUpDelay();
                    level.addFreshEntity(crateEntity);
                    
                    // VFX & SFX
                    level.sendParticles(ParticleTypes.SPLASH, hx, hy + 0.5D, hz, 30, 0.3D, 0.3D, 0.3D, 0.2D);
                    level.sendParticles(ParticleTypes.GLOW, hx, hy + 0.5D, hz, 15, 0.2D, 0.2D, 0.2D, 0.1D);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.85F, 1.2F);
                } else {
                    ExtendedInventoryDelivery.giveOrDrop(player, crate);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.85F, 1.2F);
                }
                player.displayClientMessage(Component.literal("§6📦 [보물 찾기] §f깊은 심해 속 잠들어있던 §e신비한 크레이트§f를 건져올렸습니다!"), false);
            }
        }
    }

    private static void applyHunterPerks(ServerPlayer player, LivingEntity victim) {
        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
        JobProgress progress = profile.job(JobType.HUNTER);
        int chanceBonus = progress.bonusPercent(SkillNodeStat.SPECIAL_CHANCE);
        
        // Quick Draw: Speed effect on kill
        int quickDrawLevel = progress.nodeLevel(SkillNode.primaryEffectNode(JobType.HUNTER));
        if (quickDrawLevel > 0) {
            int duration = 20 * (4 + quickDrawLevel * 2);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false));
        }

        // Wild Step: Resistance effect on kill
        int wildStepLevel = progress.nodeLevel(SkillNode.secondaryEffectNode(JobType.HUNTER));
        if (wildStepLevel > 0) {
            int duration = 20 * (5 + wildStepLevel * 3);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false));
        }

        // Steady Aim: Strength
        int steadyAimLevel = progress.nodeLevel(SkillNode.tertiaryEffectNode(JobType.HUNTER));
        if (steadyAimLevel > 0 && roll(player, Math.min(60, 10 + chanceBonus / 2 + steadyAimLevel * 5))) {
            int duration = 20 * Math.min(23, 3 + steadyAimLevel * 2);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, false));
        }
    }

    private static void dropCreditsFromMob(ServerPlayer player, LivingEntity victim) {
        long amount;
        if (victim instanceof EnderDragon) {
            amount = 100000L;
        } else if (victim instanceof WitherBoss) {
            amount = 60000L;
        } else {
            float maxHealth = victim.getMaxHealth();
            double attackDamage = victim.getAttribute(Attributes.ATTACK_DAMAGE) == null
                ? 0.0D
                : victim.getAttributeValue(Attributes.ATTACK_DAMAGE);
            amount = Math.max(400L, Math.round(500.0D + maxHealth * 8.0D + attackDamage * 250.0D));
        }

        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        if (profile.selectedJob() == JobType.HUNTER) {
            int trophySenseLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_TROPHY_SENSE);
            if (trophySenseLevel > 0) {
                amount = Math.round(amount * (1.0D + scaledPercent(trophySenseLevel, 0.02D, 0.60D)));
            }
        }

        if (victim.getPersistentData().getBoolean("nogeon_hunter_marked") || victim.getStringUUID().equals(profile.hunterPreyMarkedUUID())) {
            amount *= 2;
        }

        if (amount > 0) {
            state.profile(player.getUUID()).addCredits(amount);
            state.setDirty();
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.nogeon_economy_land.credit_drop", amount),
                true
            );
        }
    }

    private static boolean isOreBlock(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("ore") || path.equals("ancient_debris");
    }

    private static int minerBlockExp(BlockState state, boolean ore) {
        if (!ore) {
            return 1;
        }
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(Locale.ROOT);
        if (id.contains("ancient_debris") || id.contains("netherite")) {
            return 40;
        }
        if (containsAny(id, "diamond", "emerald")) {
            return 24;
        }
        if (containsAny(id, "lapis", "redstone", "gold", "ruby", "sapphire", "amethyst")) {
            return 14;
        }
        if (containsAny(id, "iron", "copper", "quartz")) {
            return 8;
        }
        if (id.contains("coal")) {
            return 5;
        }
        return 10;
    }

    private static boolean isFarmerHarvestBlock(BlockState state) {
        if (isCropMaxAge(state)) {
            return true;
        }
        return isStemFruitBlock(state);
    }

    private static boolean isFarmerHarvestAllowed(BlockState state, ServerLevel level, BlockPos pos) {
        if (!isFarmerHarvestBlock(state)) {
            return false;
        }
        if (isCropMaxAge(state)) {
            return true;
        }
        return !isPlayerPlacedResourceBlock(level, pos);
    }

    private static int getBlockAge(BlockState state) {
        net.minecraft.world.level.block.state.properties.IntegerProperty ageProp = findCropAgeProperty(state);
        return ageProp == null ? 0 : state.getValue(ageProp);
    }

    private static boolean isCropMaxAge(BlockState state) {
        net.minecraft.world.level.block.state.properties.IntegerProperty ageProp = findCropAgeProperty(state);
        return ageProp != null && state.getValue(ageProp) >= getMaxAge(ageProp);
    }

    private static boolean canGrowCrop(BlockState state) {
        net.minecraft.world.level.block.state.properties.IntegerProperty ageProp = findCropAgeProperty(state);
        return ageProp != null && state.getValue(ageProp) < getMaxAge(ageProp);
    }

    private static BlockState growCropAge(BlockState state, int amount) {
        net.minecraft.world.level.block.state.properties.IntegerProperty ageProp = findCropAgeProperty(state);
        if (ageProp == null) {
            return state;
        }
        return setCropAge(state, state.getValue(ageProp) + amount);
    }

    private static BlockState setCropAge(BlockState state, int age) {
        net.minecraft.world.level.block.state.properties.IntegerProperty ageProp = findCropAgeProperty(state);
        if (ageProp == null) {
            return state;
        }
        int maxAge = getMaxAge(ageProp);
        return state.setValue(ageProp, Math.max(0, Math.min(maxAge, age)));
    }

    private static int getMaxAge(net.minecraft.world.level.block.state.properties.IntegerProperty ageProp) {
        return ageProp.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private static net.minecraft.world.level.block.state.properties.IntegerProperty findCropAgeProperty(BlockState state) {
        if (state.hasProperty(CropBlock.AGE)) {
            return CropBlock.AGE;
        }
        for (net.minecraft.world.level.block.state.properties.Property<?> prop : state.getProperties()) {
            if (prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty intProp && prop.getName().equals("age")) {
                return intProp;
            }
        }
        return null;
    }

    private static boolean isFarmerCropBlock(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock) {
            return true;
        }
        if (block == Blocks.COCOA) {
            return true;
        }
        return findCropAgeProperty(state) != null;
    }

    private static boolean canReplantAt(ServerLevel level, BlockState cropState, BlockPos pos) {
        Block block = cropState.getBlock();
        if (block instanceof CropBlock) {
            return isCropReplantSoil(level.getBlockState(pos.below()));
        }
        if (block == Blocks.COCOA) {
            if (cropState.hasProperty(net.minecraft.world.level.block.CocoaBlock.FACING)) {
                net.minecraft.core.Direction facing = cropState.getValue(net.minecraft.world.level.block.CocoaBlock.FACING);
                BlockPos logPos = pos.relative(facing.getOpposite());
                BlockState logState = level.getBlockState(logPos);
                return logState.is(BlockTags.JUNGLE_LOGS) || logState.is(Blocks.JUNGLE_LOG) || logState.is(Blocks.JUNGLE_WOOD) || logState.is(Blocks.STRIPPED_JUNGLE_LOG) || logState.is(Blocks.STRIPPED_JUNGLE_WOOD);
            }
        }
        return false;
    }

    private static boolean replantCrop(ServerPlayer player, JobProgress progress, ServerLevel level, BlockState cropState, BlockPos pos, Block crop) {
        if (!canReplantAt(level, cropState, pos)) {
            return false;
        }
        int seedSelectionLevel = progress.nodeLevel(SkillNode.FARMER_SEED_SELECTION);
        boolean planted = seedSelectionLevel > 0 && roll(player, scaledChance(seedSelectionLevel, 1.5D, 45.0D));
        if (!planted) {
            ItemStack seedStack = findSeedInInventory(player, crop, pos, cropState);
            if (!seedStack.isEmpty()) {
                seedStack.shrink(1);
                planted = true;
            }
        }
        if (!planted) {
            return false;
        }
        level.setBlock(pos, setCropAge(cropState, 0), 3);
        if (crop instanceof CropBlock) {
            FERTILE_SOILS.put(pos.below(), 400);
        }
        return true;
    }

    private static boolean isCropReplantSoil(BlockState soilState) {
        if (soilState.is(Blocks.FARMLAND) || soilState.getBlock() instanceof FarmBlock) {
            return true;
        }
        String id = BuiltInRegistries.BLOCK.getKey(soilState.getBlock()).toString().toLowerCase(Locale.ROOT);
        return id.contains("farmland") || id.contains("rich_soil");
    }

    private static boolean isStemFruitBlock(BlockState state) {
        if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
            return true;
        }
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(Locale.ROOT);
        return containsAny(id, "pumpkin", "melon") && !containsAny(id, "stem", "seed");
    }

    private static int farmerCropExp(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(Locale.ROOT);
        int exp = 8;
        if (containsAny(id, "melon", "pumpkin", "cocoa", "berry", "grape", "tomato", "corn", "rice")) {
            exp += 3;
        }
        if (containsAny(id, "nether_wart", "chorus", "mushroom")) {
            exp += 5;
        }
        if (containsAny(id, "rare", "golden", "ancient", "magic", "mystic")) {
            exp += 8;
        }
        return exp;
    }

    private static int fisherCatchExp(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        int exp = 10 * Math.max(1, stack.getCount());
        if (containsAny(id, "puffer", "tropical", "lobster", "crab", "oyster", "clam")) {
            exp += 8 * Math.max(1, stack.getCount());
        }
        if (containsAny(id, "treasure", "crate", "pearl", "heart_of_the_sea", "nautilus")) {
            exp += 30 * Math.max(1, stack.getCount());
        }
        if (stack.getRarity() == net.minecraft.world.item.Rarity.RARE) {
            exp += 20;
        } else if (stack.getRarity() == net.minecraft.world.item.Rarity.EPIC) {
            exp += 45;
        } else if (stack.getRarity() == net.minecraft.world.item.Rarity.UNCOMMON) {
            exp += 8;
        }
        return exp;
    }

    private static double itemRarityMultiplier(ItemStack stack) {
        return switch (stack.getRarity()) {
            case UNCOMMON -> 1.15D;
            case RARE -> 1.35D;
            case EPIC -> 1.75D;
            default -> 1.0D;
        };
    }

    private static boolean isBossEntity(LivingEntity entity) {
        if (entity instanceof WitherBoss || entity instanceof EnderDragon) {
            return true;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().toLowerCase(Locale.ROOT);
        String className = entity.getClass().getName().toLowerCase(Locale.ROOT);
        if (containsAny(id, "boss", "dragon", "wither") || containsAny(className, "boss", "dragon", "wither")) {
            return true;
        }
        if (entity.getMaxHealth() >= 250.0F) {
            return true;
        }
        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                String fieldType = field.getType().getName();
                if ("net.minecraft.server.level.ServerBossEvent".equals(fieldType) || "net.minecraft.world.BossEvent".equals(fieldType)) {
                    return true;
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlayerPlacedResourceBlock(ServerLevel level, BlockPos pos) {
        return EconomyState.get(level.getServer()).isPlayerPlacedResourceBlock(level.dimension(), pos);
    }

    public static boolean isTrackedPlacedResourceBlock(BlockState state) {
        return isOreBlock(state)
            || isRockBlock(state)
            || state.is(BlockTags.LOGS)
            || isDiggableMaterial(state);
    }

    public static boolean isDiggableMaterial(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(Locale.ROOT);
        return id.contains("dirt")
            || id.contains("sand")
            || id.contains("gravel")
            || id.contains("clay")
            || id.contains("grass_block")
            || id.contains("podzol")
            || id.contains("mycelium");
    }

    private static void growNearbyCrops(ServerLevel level, BlockState harvestedState, BlockPos origin, int growthBursts) {
        if (!(harvestedState.getBlock() instanceof CropBlock)) {
            return;
        }

        BlockPos[] nearby = new BlockPos[] {
            origin.north(),
            origin.south(),
            origin.east(),
            origin.west(),
            origin.north().east(),
            origin.north().west(),
            origin.south().east(),
            origin.south().west()
        };

        for (int burst = 0; burst < growthBursts; burst++) {
            BlockPos targetPos = nearby[level.random.nextInt(nearby.length)];
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.getBlock() != harvestedState.getBlock() || !(targetState.getBlock() instanceof CropBlock)) {
                continue;
            }
            if (!canGrowCrop(targetState)) {
                continue;
            }
            level.setBlock(targetPos, growCropAge(targetState, 1), Block.UPDATE_ALL);
        }
    }

    private static ItemStack findFishingRod(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static void refundDurability(ItemStack stack, int amount) {
        if (!stack.isDamageableItem() || !stack.isDamaged()) {
            return;
        }
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
    }

    private static void spawnExtraDrop(ServerPlayer player, BlockState state, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem())) {
            if (!drop.isEmpty()) {
                ItemStack extraDrop = drop.copy();
                extraDrop.setCount(1);
                Block.popResource(level, pos, extraDrop);
                return;
            }
        }
    }

    private static boolean roll(ServerPlayer player, int percentChance) {
        return player.getRandom().nextInt(100) < Math.min(95, Math.max(0, percentChance));
    }

    private static int scaledChance(int level, double perLevel, double maxChance) {
        return (int) Math.round(Math.min(maxChance, Math.max(0.0D, level * perLevel)));
    }

    private static double scaledPercent(int level, double perLevel, double maxPercent) {
        return Math.min(maxPercent, Math.max(0.0D, level * perLevel));
    }

    private static void performChainMine(ServerPlayer player, BlockState originState, BlockPos originPos, int skillLevel) {
        if (isChainMining.get()) {
            return;
        }
        isChainMining.set(true);
        try {
            ServerLevel level = player.serverLevel();
            List<BlockPos> queue = new ArrayList<>();
            List<BlockPos> toBreak = new ArrayList<>();
            queue.add(originPos);
            
            boolean oreType = isOreBlock(originState);
            boolean rockType = isRockBlock(originState);
            int maxBreak = oreType
                ? Math.min(6, 1 + (int) Math.ceil(skillLevel * 0.5D))
                : Math.min(4, 1 + (int) Math.ceil(skillLevel * 0.3D));
            
            int index = 0;
            while (index < queue.size() && toBreak.size() < maxBreak) {
                BlockPos current = queue.get(index++);
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (queue.contains(neighbor) || toBreak.contains(neighbor)) {
                        continue;
                    }
                    
                    BlockState neighborState = level.getBlockState(neighbor);
                    boolean match = false;
                    if (oreType && isOreBlock(neighborState) && neighborState.getBlock() == originState.getBlock()) {
                        match = true;
                    } else if (rockType && isRockBlock(neighborState)) {
                        match = true;
                    }
                    
                    if (match) {
                        queue.add(neighbor);
                        toBreak.add(neighbor);
                        if (toBreak.size() >= maxBreak) {
                            break;
                        }
                    }
                }
            }
            
            ItemStack pickaxe = player.getMainHandItem();
            for (BlockPos targetPos : toBreak) {
                BlockState state = level.getBlockState(targetPos);
                if (!canRewardBlockBreak(player, targetPos) || isPlayerPlacedResourceBlock(level, targetPos)) {
                    continue;
                }
                if (state.isAir() || state.getDestroySpeed(level, targetPos) < 0.0F) {
                    continue;
                }
                
                level.destroyBlock(targetPos, false, player);
                Block.dropResources(state, level, targetPos, level.getBlockEntity(targetPos), player, pickaxe);
                handleMinerBreakEvents(player, state, targetPos, false);
            }
        } finally {
            isChainMining.set(false);
        }
    }

    private static void applyOreHunter(ServerPlayer player, BlockPos pos, int skillLevel) {
        double chance = Math.min(5.0D, Math.max(0.0D, skillLevel) * 0.5D);
        if (player.getRandom().nextDouble() * 100 < chance) {
            ItemStack[] pool = new ItemStack[] {
                new ItemStack(Items.COAL),
                new ItemStack(Items.RAW_COPPER),
                new ItemStack(Items.RAW_IRON),
                new ItemStack(Items.RAW_GOLD),
                new ItemStack(Items.REDSTONE),
                new ItemStack(Items.LAPIS_LAZULI),
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.EMERALD)
            };
            int[] weights = new int[] {28, 24, 18, 10, 8, 7, 3, 2};
            ItemStack randomOre = pool[pickWeightedIndex(player, weights)];
            Block.popResource(player.serverLevel(), pos, randomOre);
        }
    }

    private static int pickWeightedIndex(ServerPlayer player, int[] weights) {
        int total = 0;
        for (int weight : weights) {
            total += Math.max(0, weight);
        }
        int roll = player.getRandom().nextInt(Math.max(1, total));
        for (int i = 0; i < weights.length; i++) {
            roll -= Math.max(0, weights[i]);
            if (roll < 0) {
                return i;
            }
        }
        return 0;
    }

    private static boolean isRockBlock(BlockState state) {
        Block block = state.getBlock();
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return path.equals("stone") || path.equals("deepslate") || path.equals("netherrack") 
            || path.contains("cobblestone") || path.contains("andesite") || path.contains("diorite") || path.contains("granite");
    }

    private static boolean isFarmAnimal(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.animal.Animal;
    }

    public static void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());

        if (profile.selectedJob() == JobType.HUNTER) {
            int huntRoutine = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_HUNT_ROUTINE);
            int doubleDrop = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_WEAPON_TUNING);

            // 1. 드랍률 증가 패시브
            if (huntRoutine > 0 && player.getRandom().nextDouble() < scaledPercent(huntRoutine, 0.015D, 0.45D)) {
                for (net.minecraft.world.entity.item.ItemEntity itemEntity : event.getDrops()) {
                    ItemStack stack = itemEntity.getItem();
                    stack.setCount(stack.getCount() + 1);
                }
            }

            // 2. 더블 드랍 확률 패시브
            if (doubleDrop > 0 && player.getRandom().nextDouble() < scaledPercent(doubleDrop, 0.0125D, 0.35D)) {
                java.util.List<net.minecraft.world.entity.item.ItemEntity> extraDrops = new java.util.ArrayList<>();
                for (net.minecraft.world.entity.item.ItemEntity itemEntity : event.getDrops()) {
                    ItemStack copyStack = itemEntity.getItem().copy();
                    net.minecraft.world.entity.item.ItemEntity extraEntity = new net.minecraft.world.entity.item.ItemEntity(
                        victim.level(), victim.getX(), victim.getY(), victim.getZ(), copyStack
                    );
                    extraDrops.add(extraEntity);
                }
                event.getDrops().addAll(extraDrops);
            }

            // 3. 표식 대상 처치 시 전리품 2배
            if (victim.getPersistentData().getBoolean("nogeon_hunter_marked") || victim.getStringUUID().equals(profile.hunterPreyMarkedUUID())) {
                java.util.List<net.minecraft.world.entity.item.ItemEntity> extraDrops = new java.util.ArrayList<>();
                for (net.minecraft.world.entity.item.ItemEntity itemEntity : event.getDrops()) {
                    ItemStack copyStack = itemEntity.getItem().copy();
                    net.minecraft.world.entity.item.ItemEntity extraEntity = new net.minecraft.world.entity.item.ItemEntity(
                        victim.level(), victim.getX(), victim.getY(), victim.getZ(), copyStack
                    );
                    extraDrops.add(extraEntity);
                }
                event.getDrops().addAll(extraDrops);
            }
        }

        // 4. 황금 행운 요리 버프 (행운 효과를 가졌을 때 사냥 더블 드랍 15% 적용)
        if (player.hasEffect(MobEffects.LUCK) && player.getRandom().nextDouble() < 0.15D) {
            java.util.List<net.minecraft.world.entity.item.ItemEntity> extraDrops = new java.util.ArrayList<>();
            for (net.minecraft.world.entity.item.ItemEntity itemEntity : event.getDrops()) {
                ItemStack copyStack = itemEntity.getItem().copy();
                net.minecraft.world.entity.item.ItemEntity extraEntity = new net.minecraft.world.entity.item.ItemEntity(
                    victim.level(), victim.getX(), victim.getY(), victim.getZ(), copyStack
                );
                extraDrops.add(extraEntity);
            }
            event.getDrops().addAll(extraDrops);
        }
    }

    public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        // [피격자 판정 - 플레이어가 대미지를 입을 때]
        if (event.getEntity() instanceof ServerPlayer victim) {
            triggerDroneExpression(victim, 4, 40); // Worried expression on damage
            CompoundTag victimNbt = victim.getPersistentData();
            
            if (victimNbt.contains("nogeon_engineer_kinetic_boost_ticks")) {
                int lvl = victimNbt.getInt("nogeon_engineer_kinetic_boost_level");
                // Option A: Armor +4 equivalent (16% extra reduction) -> 31% base + 1% per level. Max 41% reduction!
                float reduction = 0.69F - lvl * 0.01F;
                event.setAmount(event.getAmount() * reduction);
            }
            
            EconomyState eState = EconomyState.get(victim.server);
            PlayerProfile victimProfile = eState.profile(victim.getUUID());
            if (victimProfile.selectedJob() == JobType.ENGINEER) {

            }
            
            // 1. 금강불괴의 방벽 (STEEL_GUARD)
            if (victimNbt.contains("nogeon_steel_guard_ticks")) {
                boolean isPlus = victimNbt.getBoolean("nogeon_steel_guard_plus");
                float baseMultiplier = isPlus ? 0.70F : 0.80F; // 상시 -30% 또는 -20%
                
                boolean isProjectile = event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) 
                    || event.getSource().getMsgId().equals("bullet") 
                    || event.getSource().getMsgId().equals("tacz_bullet")
                    || event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;
                
                float finalMultiplier = baseMultiplier;
                if (isProjectile) {
                    finalMultiplier = isPlus ? 0.455F : 0.55F; // 투사체 시 총 -54.5% 또는 -45% 곱연산 감쇄
                }
                event.setAmount(event.getAmount() * finalMultiplier);
            }

            // 2. 신성한 수호 결계 (IMMUNITY) 속성/환경 피해 50% 감쇄
            if (victimNbt.contains("nogeon_immunity_ticks")) {
                boolean isAttributeDamage = event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                    || event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                    || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
                    || event.getSource().getMsgId().equals("magic")
                    || event.getSource().getMsgId().equals("wither")
                    || event.getSource().getMsgId().equals("poison")
                    || event.getSource().getMsgId().equals("cactus")
                    || event.getSource().getMsgId().equals("lava")
                    || event.getSource().getMsgId().equals("inFire")
                    || event.getSource().getMsgId().equals("onFire")
                    || event.getSource().getMsgId().equals("fall");
                
                if (isAttributeDamage) {
                    event.setAmount(event.getAmount() * 0.5F); // 속성 피해 50% 무효화
                }
            }

            // 3. 구원의 영양식 (DEATH_PREVENTION)
            if (victim.getHealth() - event.getAmount() <= 0 && !event.isCanceled()) {
                ItemStack deathFood = findDeathPreventionFood(victim);
                if (!deathFood.isEmpty()) {
                    deathFood.setCount(deathFood.getCount() - 1);
                    
                    event.setCanceled(true);
                    victim.setHealth(6.0F); // 하트 3칸 (6.0F)
                    
                    victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                    victim.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                    victim.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                    
                    ServerLevel sLevel = victim.serverLevel();
                    sLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    sLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
                    
                    // 화려한 파티클 연출
                    sLevel.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.1D);
                    sLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 30, 0.8D, 0.8D, 0.8D, 0.2D);
                    
                    // 5블록 반경 내 적 밀쳐내기 및 속도 둔화 II 3초 적용
                    net.minecraft.world.phys.AABB pushArea = victim.getBoundingBox().inflate(5.0D);
                    java.util.List<LivingEntity> enemies = sLevel.getEntitiesOfClass(LivingEntity.class, pushArea, 
                        e -> e != victim && !(e instanceof net.minecraft.world.entity.player.Player));
                    
                    for (LivingEntity enemy : enemies) {
                        double dx = enemy.getX() - victim.getX();
                        double dz = enemy.getZ() - victim.getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 0.1D) {
                            enemy.setDeltaMovement(dx / dist * 1.5D, 0.45D, dz / dist * 1.5D);
                            enemy.hurtMarked = true;
                        }
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); // 둔화 II 3초
                    }
                    
                    victim.displayClientMessage(Component.literal("§d✨ [기적 - 구원의 영양식] §f가방 속 영양식 요리가 소모되어 치명상을 모면하고 충격파로 적을 날려버렸습니다!"), true);
                }
            }
        }

        // [공격자 판정 - 플레이어가 타격할 때]
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());

        // 영구 기관 활성화 시 TACZ 총기 데미지 15% 증가
        if (player.getPersistentData().contains("nogeon_engineer_kinetic_boost_ticks")) {
            boolean isRanged = event.getSource().getMsgId().contains("bullet") 
                || event.getSource().getMsgId().contains("tacz") 
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);
            if (isRanged) {
                event.setAmount(event.getAmount() * 1.15F);
            }
        }

        // 3. 대재앙의 학살자 (BOSS_DAMAGE)
        if (player.getPersistentData().contains("nogeon_boss_slayer_ticks")) {
            boolean isPlus = player.getPersistentData().getBoolean("nogeon_boss_slayer_plus");
            boolean isBoss = victim instanceof WitherBoss 
                || victim instanceof EnderDragon 
                || victim.getClass().getSimpleName().toLowerCase().contains("boss")
                || victim.getClass().getName().toLowerCase().contains("boss")
                || !victim.canChangeDimensions();
            
            float multiplier;
            if (isBoss) {
                multiplier = isPlus ? 1.60F : 1.40F; // 보스 상대 시 총 피해 +60% / +40%
            } else {
                multiplier = isPlus ? 1.225F : 1.15F; // 일반 몹 상대 시 상시 피해 +22.5% / +15%
            }
            event.setAmount(event.getAmount() * multiplier);
        }

        if (profile.selectedJob() == JobType.HUNTER) {
            // 1. 50레벨 [갈증] 출혈 부여 (steady_aim 노드를 50렙 출혈 노드로 차용)
            int thirstLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_STEADY_AIM);
            if (thirstLevel > 0) {
                double chance = Math.min(60.0D, 10.0D + thirstLevel * 5.0D);
                if (canTickBleeding(victim) && player.getRandom().nextDouble() * 100 < chance) {
                    CompoundTag nbt = victim.getPersistentData();
                    int ticks = Math.min(14, 4 + thirstLevel) * 20;
                    float bleedDamage = 1.0F + Math.min(5.0F, thirstLevel * 0.5F);

                    nbt.putInt("nogeon_bleeding_ticks", ticks);
                    nbt.putFloat("nogeon_bleeding_damage", bleedDamage);
                    nbt.putInt("nogeon_bleeding_level", thirstLevel);
                    nbt.putString("nogeon_bleeding_source", player.getStringUUID());

                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
            }

            // 2. 75레벨 [사냥감의 표식] 피해량 증폭 (wild_step 노드 사용)
            if (victim.getStringUUID().equals(profile.hunterPreyMarkedUUID())) {
                int markLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_WILD_STEP);
                if (markLevel > 0) {
                    float multiplier = 1.0F + Math.min(0.40F, 0.05F + markLevel * 0.035F);
                    event.setAmount(event.getAmount() * multiplier);
                }
            }

            // 3. 100레벨 [먹이사슬의 정점] 약점 3D 빌보드 타격 판정 (apex_predator 노드 사용)
            int apexLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_APEX_PREDATOR);
            if (apexLevel > 0 && profile.hunterSenseActive()) {
                long gameTime = victim.level().getGameTime();
                long period = gameTime / 60;
                java.util.Random rand = new java.util.Random(victim.getUUID().getMostSignificantBits() ^ victim.getUUID().getLeastSignificantBits() ^ period);
                int weakpointDir = rand.nextInt(6); // 0-3: side, 4: high, 5: low

                double dx = player.getX() - victim.getX();
                double dz = player.getZ() - victim.getZ();
                double attackYaw = Math.toDegrees(Math.atan2(-dx, dz));
                double angleDiff = (attackYaw - victim.getYRot()) % 360;
                if (angleDiff < 0) {
                    angleDiff += 360;
                }

                int attackerPosDir = -1;
                if (angleDiff > 315.0D || angleDiff <= 45.0D) {
                    attackerPosDir = 0; // 전방
                } else if (angleDiff > 45.0D && angleDiff <= 135.0D) {
                    attackerPosDir = 3; // 우측
                } else if (angleDiff > 135.0D && angleDiff <= 225.0D) {
                    attackerPosDir = 1; // 후방
                } else if (angleDiff > 225.0D && angleDiff <= 315.0D) {
                    attackerPosDir = 2; // 좌측
                }

                if (attackerPosDir == weakpointDir || hitHunterWeakpoint(player, victim, weakpointDir)) {
                    float multiplier = 1.3F + apexLevel * 0.07F;
                    event.setAmount(event.getAmount() * multiplier);

                    float healAmount = player.getMaxHealth() * Math.min(0.10F, 0.01F * apexLevel);
                    player.heal(healAmount);

                    int duration = Math.min(13, 3 + apexLevel) * 20;
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 2, false, true));

                    ServerLevel sLevel = player.serverLevel();
                    sLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2F, 1.4F);
                    sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 1.2F);

                    for (int i = 0; i < 8; i++) {
                        double px = victim.getX() + (sLevel.random.nextDouble() - 0.5D) * 0.6D;
                        double py = victim.getY() + sLevel.random.nextDouble() * victim.getBbHeight();
                        double pz = victim.getZ() + (sLevel.random.nextDouble() - 0.5D) * 0.6D;
                        sLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.1D);
                        sLevel.sendParticles(ParticleTypes.GLOW, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }

                    player.displayClientMessage(Component.literal("§e💥 [약점 타격 성공] §a치명타 피해를 입히고 체력을 회복했습니다!"), true);
                }
            }
        }
    }

    private static void restoreThirst(ServerPlayer player, int thirst, int quenched) {
        player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(data -> {
            data.drink(player, thirst, quenched);
            data.updateThirstData(player);
        });
    }

    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            accelerateFishingLure(player);
        }

        CompoundTag nbt = entity.getPersistentData();
        if (nbt.contains("nogeon_bleeding_ticks")) {
            if (!canTickBleeding(entity)) {
                clearBleedingData(nbt);
                return;
            }
            int ticks = nbt.getInt("nogeon_bleeding_ticks");
            float damage = nbt.getFloat("nogeon_bleeding_damage");
            
            if (ticks <= 0) {
                clearBleedingData(nbt);
                return;
            }

            ticks--;
            nbt.putInt("nogeon_bleeding_ticks", ticks);

            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));

            if (entity.tickCount % 10 == 0) {
                ServerLevel sLevel = (ServerLevel) entity.level();
                double px = entity.getX() + (sLevel.random.nextDouble() - 0.5D) * 0.5D;
                double py = entity.getY() + sLevel.random.nextDouble() * entity.getBbHeight();
                double pz = entity.getZ() + (sLevel.random.nextDouble() - 0.5D) * 0.5D;
                sLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0F);
            }

            if (ticks % 40 == 0) {
                net.minecraft.world.damagesource.DamageSource source = entity.damageSources().magic();
                if (nbt.contains("nogeon_bleeding_source")) {
                    try {
                        java.util.UUID sourceUUID = java.util.UUID.fromString(nbt.getString("nogeon_bleeding_source"));
                        ServerPlayer attacker = (ServerPlayer) entity.level().getServer().getPlayerList().getPlayer(sourceUUID);
                        if (attacker != null) {
                            source = entity.damageSources().playerAttack(attacker);
                        }
                    } catch (Exception e) {}
                }

                float finalDamage = damage;
                if (entity instanceof WitherBoss || entity instanceof EnderDragon) {
                    finalDamage *= 0.5F;
                }
                if (entity instanceof Player) {
                    finalDamage *= 0.25F;
                }

                try {
                    entity.hurt(source, finalDamage);
                } catch (RuntimeException e) {
                    clearBleedingData(nbt);
                    if (!(entity instanceof Player) && !entity.isRemoved() && entity.getHealth() <= 0.0F) {
                        entity.setHealth(1.0F);
                    }
                    ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                    com.nogeon.economyland.NoGeonEconomyLand.LOGGER.warn(
                        "Suppressed external loot crash while applying hunter bleeding damage to {}. Bleeding was cleared.",
                        entityId,
                        e
                    );
                }
            }
        }
    }

    private static void clearBleedingData(CompoundTag nbt) {
        nbt.remove("nogeon_bleeding_ticks");
        nbt.remove("nogeon_bleeding_damage");
        nbt.remove("nogeon_bleeding_level");
        nbt.remove("nogeon_bleeding_source");
    }

    private static boolean canTickBleeding(LivingEntity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive() || entity.isDeadOrDying() || entity.getHealth() <= 0.0F) {
            return false;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId != null && entityId.toString().toLowerCase(Locale.ROOT).contains("corpse")) {
            return false;
        }
        String className = entity.getClass().getName().toLowerCase(Locale.ROOT);
        return !className.contains("corpse");
    }

    public static void onItemUseFinish(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItem();
        if (!stack.isEmpty() && stack.isEdible() && stack.hasTag()) {
            CompoundTag nbt = stack.getTag();
            if (nbt.contains("CookedByPlayer")) {
                EconomyState state = EconomyState.get(player.server);
                PlayerProfile profile = state.profile(player.getUUID());
                
                // 1. 25레벨 [손맛] 기본 회복량/포만감 증폭 및 숙성도 중첩
                int heartyLevel = nbt.contains("HeartyLevel") ? nbt.getInt("HeartyLevel") : (profile.selectedJob() == JobType.COOK ? profile.job(JobType.COOK).nodeLevel(SkillNode.COOK_HEARTY_PORTION) : 0);
                int ageingLevel = nbt.getInt("AgeingLevel");
                double multiplier = 1.0D + scaledPercent(heartyLevel, 0.05D, 0.50D) + scaledPercent(ageingLevel, 0.08D, 0.80D);

                net.minecraft.world.food.FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, player);
                if (foodProperties != null) {
                    int baseFood = foodProperties.getNutrition();
                    float baseSaturation = foodProperties.getSaturationModifier();

                    int bonusFood = (int) Math.round(baseFood * (multiplier - 1.0D));
                    float bonusSaturation = (float) (baseSaturation * (multiplier - 1.0D));

                    if (bonusFood > 0) {
                        player.getFoodData().eat(bonusFood, bonusSaturation);
                    }
                }

                // 2. 25레벨 [손맛] 대성공 버프 부여 (3분 = 3600틱)
                if (nbt.getBoolean("CookGreatSuccess")) {
                    net.minecraft.world.effect.MobEffect[] successBuffs = new net.minecraft.world.effect.MobEffect[] {
                        MobEffects.DAMAGE_BOOST,
                        MobEffects.MOVEMENT_SPEED,
                        MobEffects.REGENERATION,
                        MobEffects.DAMAGE_RESISTANCE
                    };
                    net.minecraft.world.effect.MobEffect chosen = successBuffs[player.getRandom().nextInt(successBuffs.length)];
                    player.addEffect(new MobEffectInstance(chosen, 3600, 0, false, true));
                    player.displayClientMessage(Component.literal("§e✨ [손맛 대성공] §f명품 음식의 풍미로 강력한 특수 버프를 획득했습니다!"), true);
                }

                // 3. 50레벨 [배는 채워야지] 포만감 100% 버프 부여 (타인에게도 요리에 각인되어 전파)
                int warmMealLevel = (profile.selectedJob() == JobType.COOK ? profile.job(JobType.COOK).nodeLevel(SkillNode.COOK_WARM_MEAL) : 0);
                if (warmMealLevel <= 0 && nbt.contains("nogeon_warm_meal_level")) {
                    warmMealLevel = nbt.getInt("nogeon_warm_meal_level");
                }
                if (warmMealLevel > 0 && player.getFoodData().getFoodLevel() >= 20) {
                    net.minecraft.world.effect.MobEffect[] mealBuffs = new net.minecraft.world.effect.MobEffect[] {
                        MobEffects.MOVEMENT_SPEED,
                        MobEffects.DIG_SPEED,
                        MobEffects.REGENERATION,
                        MobEffects.DAMAGE_RESISTANCE
                    };
                    net.minecraft.world.effect.MobEffect chosen = mealBuffs[player.getRandom().nextInt(mealBuffs.length)];
                    int duration = Math.min(10, warmMealLevel) * 60 * 20;
                    int amplifier = Math.min(3, Math.max(0, (warmMealLevel - 1) / 3));
                    
                    boolean isPlus = nbt.getBoolean("nogeon_plus_grade");
                    if (isPlus) {
                        duration = (int) (duration * 1.5D);
                        amplifier = (int) Math.round((amplifier + 1) * 1.5D) - 1;
                        if (amplifier < 0) amplifier = 0;
                    }

                    player.addEffect(new MobEffectInstance(chosen, duration, amplifier, false, true));
                    String ampText = amplifier > 0 ? " " + (amplifier + 1) + "단계" : "";
                    player.displayClientMessage(Component.literal("§6🍖 [배는 채워야지] §f허기를 완전히 채워 특별한 식사 버프" + ampText + "를 얻었습니다!"), true);
                }

                // 4. 100레벨 [나만의 레시피] 특수 고급 버프 적용
                if (nbt.contains("RecipeBuffs")) {
                    ListTag buffsNbt = nbt.getList("RecipeBuffs", Tag.TAG_STRING);
                    int recipeLevel = nbt.getInt("MasterRecipeLevel");
                    int duration = (5 + recipeLevel * 5) * 60 * 20;

                    // 고급 부산물 재료로 제작된 명품 요리인 경우 효과/시간 50% 증폭!
                    if (nbt.getBoolean("nogeon_plus_grade")) {
                        duration = (int) (duration * 1.5);
                    }

                    for (int i = 0; i < buffsNbt.size(); i++) {
                        String buffType = buffsNbt.getString(i);
                        boolean isPlus = nbt.getBoolean("nogeon_plus_grade");
                        switch (buffType) {
                            case "BOSS_DAMAGE":
                                player.getPersistentData().putInt("nogeon_boss_slayer_ticks", duration);
                                if (isPlus) {
                                    player.getPersistentData().putBoolean("nogeon_boss_slayer_plus", true);
                                } else {
                                    player.getPersistentData().remove("nogeon_boss_slayer_plus");
                                }
                                int bossDmgText = isPlus ? 30 : 20;
                                player.displayClientMessage(Component.literal("§c⚔️ [보스 처단자] §f보스 몬스터 공격력 +" + bossDmgText + "% 각인 효과가 발현되었습니다!"), false);
                                break;
                            case "GOLDEN_LUCK":
                                int luckLevel = recipeLevel - 1;
                                if (isPlus) {
                                    luckLevel = (int) Math.round((luckLevel + 1) * 1.5D) - 1;
                                    if (luckLevel < 0) luckLevel = 0;
                                }
                                player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, luckLevel, false, true));
                                player.displayClientMessage(Component.literal("§a🍀 [황금 행운] §f아이템 드랍 행운 증폭 효과가 주입되었습니다! (Lv." + (luckLevel + 1) + ")"), false);
                                break;
                            case "HEART_BREATH":
                                {
                                    player.getPersistentData().putInt("nogeon_heart_breath_ticks", duration);
                                    player.getPersistentData().putInt("nogeon_heart_breath_level", recipeLevel);
                                    if (isPlus) {
                                        player.getPersistentData().putBoolean("nogeon_heart_breath_plus", true);
                                    } else {
                                        player.getPersistentData().remove("nogeon_heart_breath_plus");
                                    }
                                    
                                    net.minecraft.world.entity.ai.attributes.AttributeInstance mhAttr = player.getAttribute(Attributes.MAX_HEALTH);
                                    if (mhAttr != null) {
                                        mhAttr.removeModifier(HEART_BREATH_UUID);
                                        double basePercent = 0.10D + recipeLevel * 0.03D;
                                        if (isPlus) {
                                            basePercent *= 1.5D;
                                        }
                                        double percent = Math.min(isPlus ? 0.90D : 0.60D, basePercent);
                                        mhAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            HEART_BREATH_UUID, "Heart Breath Buff", percent, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE));
                                        
                                        // 퍼센트 증가된 절대 수치만큼 체력 즉시 회복
                                        float healAmt = player.getMaxHealth() * (float) percent;
                                        player.heal(healAmt);
                                    }
                                    
                                    int percentText = (int) Math.round(Math.min(isPlus ? 90.0D : 60.0D, (10.0D + recipeLevel * 3.0D) * (isPlus ? 1.5D : 1.0D)));
                                    player.displayClientMessage(Component.literal("§d💖 [대지의 숨결] §f최대 체력이 기본 체력 대비 §a" + percentText + "%§f 증가했습니다!"), false);
                                }
                                break;
                            case "IMMUNITY":
                                player.getPersistentData().putInt("nogeon_immunity_ticks", duration);
                                List<net.minecraft.world.effect.MobEffect> toRemove = new ArrayList<>();
                                for (MobEffectInstance instance : player.getActiveEffects()) {
                                    if (!instance.getEffect().isBeneficial()) {
                                        toRemove.add(instance.getEffect());
                                    }
                                }
                                for (net.minecraft.world.effect.MobEffect effect : toRemove) {
                                    player.removeEffect(effect);
                                }
                                player.displayClientMessage(Component.literal("§b🧪 [절대 면역] §f해로운 디버프에 대한 절대 면역 결계가 생성되었습니다!"), false);
                                break;
                            case "STEEL_GUARD":
                                player.getPersistentData().putInt("nogeon_steel_guard_ticks", duration);
                                if (isPlus) {
                                    player.getPersistentData().putBoolean("nogeon_steel_guard_plus", true);
                                } else {
                                    player.getPersistentData().remove("nogeon_steel_guard_plus");
                                }
                                int guardText = isPlus ? 45 : 30;
                                player.displayClientMessage(Component.literal("§7🏹 [강철 수호] §f원거리 탄환/투사체 피해 " + guardText + "% 감쇄 효과가 적용되었습니다!"), false);
                                break;
                        }
                    }
                }
            }
        }
    }

    private static ItemStack findDeathPreventionFood(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.isEdible() && stack.hasTag()) {
                CompoundTag nbt = stack.getTag();
                if (nbt.contains("CookedByPlayer") && nbt.contains("RecipeBuffs")) {
                    ListTag buffsNbt = nbt.getList("RecipeBuffs", Tag.TAG_STRING);
                    for (int i = 0; i < buffsNbt.size(); i++) {
                        if (buffsNbt.getString(i).equals("DEATH_PREVENTION")) {
                            return stack;
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static java.lang.reflect.Field timeUntilLureField = null;
    private static java.lang.reflect.Field timeUntilHookedField = null;

    private static java.lang.reflect.Field resolveFishingHookIntField(String... names) throws NoSuchFieldException {
        for (String fieldName : names) {
            try {
                java.lang.reflect.Field field = net.minecraft.world.entity.projectile.FishingHook.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(java.util.Arrays.toString(names));
    }

    private static boolean accelerateFishingLureDirect(ServerPlayer player) {
        try {
            if (timeUntilLureField == null) {
                timeUntilLureField = resolveFishingHookIntField("timeUntilLured", "f_37090_", "timeUntilLure", "f_37118_");
            }
            if (timeUntilHookedField == null) {
                timeUntilHookedField = resolveFishingHookIntField("timeUntilHooked", "f_37091_", "f_37117_");
            }
            PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
            if (profile.selectedJob() != JobType.FISHER) {
                return true;
            }
            int lureTuningLevel = profile.job(JobType.FISHER).nodeLevel(SkillNode.FISHER_LURE_TUNING);
            BlockPos hookPos = waterBlockForFishingHook(player.serverLevel(), player.fishing);
            if (hookPos == null) {
                hookPos = player.fishing.blockPosition();
            }

            boolean inFishery = false;
            for (java.util.Map.Entry<BlockPos, FisheryZone> zoneEntry : FISHERY_ZONES.entrySet()) {
                BlockPos center = zoneEntry.getKey();
                FisheryZone zone = zoneEntry.getValue();
                if (Math.abs(hookPos.getY() - center.getY()) <= 4) {
                    double dx = hookPos.getX() - center.getX();
                    double dz = hookPos.getZ() - center.getZ();
                    double dist2DSq = dx * dx + dz * dz;
                    double radius = Math.min(18.0D, 5.0D + zone.skillLevel * 1.4D);
                    if (dist2DSq <= radius * radius) {
                        inFishery = true;
                        break;
                    }
                }
            }

            boolean inHotspot = false;
            CompoundTag playerNbt = player.getPersistentData();
            if (playerNbt.contains("nogeon_hotspot_x")) {
                BlockPos hotspot = new BlockPos(
                    playerNbt.getInt("nogeon_hotspot_x"),
                    playerNbt.getInt("nogeon_hotspot_y"),
                    playerNbt.getInt("nogeon_hotspot_z")
                );
                if (Math.abs(hookPos.getY() - hotspot.getY()) <= 4) {
                    double dx = hookPos.getX() - hotspot.getX();
                    double dz = hookPos.getZ() - hotspot.getZ();
                    double dist2DSq = dx * dx + dz * dz;
                    double hotspotRadius = Math.min(7.5D, 3.5D + lureTuningLevel * 0.45D);
                    inHotspot = dist2DSq <= hotspotRadius * hotspotRadius;
                }
            }

            int ticksToReduce = Math.max(0, Math.min(10, (lureTuningLevel + 2) / 3));
            if (inFishery) {
                ticksToReduce += 18;
            } else if (inHotspot) {
                ticksToReduce += 10;
            }

            ItemStack rod = findFishingRod(player);
            if (!rod.isEmpty()) {
                int level = SmithingService.level(rod);
                if (level > 0) {
                    ticksToReduce += (level + 1) / 2;
                }
                double reforgeSpeed = SmithEvents.reforgeValue(rod, "fishing_speed");
                if (reforgeSpeed > 0.0D) {
                    ticksToReduce += (int) Math.round(reforgeSpeed * 20.0D);
                }
            }

            if (ticksToReduce <= 0) {
                return true;
            }

            int lureTime = timeUntilLureField.getInt(player.fishing);
            int hookedTime = timeUntilHookedField.getInt(player.fishing);
            if (lureTime > 0) {
                timeUntilLureField.setInt(player.fishing, Math.max(1, lureTime - ticksToReduce));
            }
            if (hookedTime > 0) {
                timeUntilHookedField.setInt(player.fishing, Math.max(1, hookedTime - ticksToReduce));
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void accelerateFishingLure(ServerPlayer player) {
        if (player.fishing == null) return;
        if (player.tickCount % 5 != 0) return;
        if (accelerateFishingLureDirect(player)) {
            return;
        }
        try {
            if (timeUntilLureField == null) {
                // Mojang 이름 및 SRG 이름 우선 검색
                String[] fieldNames = { "timeUntilLured", "f_37118_", "timeUntilLure", "f_37121_", "f_37120_", "f_37119_", "timeUntilHooked", "f_37117_", "f_37114_" };
                for (String fieldName : fieldNames) {
                    try {
                        timeUntilLureField = net.minecraft.world.entity.projectile.FishingHook.class.getDeclaredField(fieldName);
                        break;
                    } catch (NoSuchFieldException ignored) {
                    }
                }
                
                // 만약 여전히 null인 경우, FishingHook 내부의 모든 private int 필드를 동적으로 검사하여 100% 견고하게 획득
                if (timeUntilLureField == null) {
                    for (java.lang.reflect.Field field : net.minecraft.world.entity.projectile.FishingHook.class.getDeclaredFields()) {
                        if (field.getType() == int.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            String fName = field.getName();
                            if (fName.equals("timeUntilLured") || fName.equals("f_37118_")) {
                                timeUntilLureField = field;
                                break;
                            }
                        }
                    }
                }
                
                if (timeUntilLureField == null) {
                    System.err.println("[NoGeon Economy Land] CRITICAL: could not resolve timeUntilLured in FishingHook via reflection!");
                    return;
                }
                timeUntilLureField.setAccessible(true);
            }
            
            int currentTime = timeUntilLureField.getInt(player.fishing);
            if (currentTime > 0) {
                PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
                if (profile.selectedJob() != JobType.FISHER) {
                    return;
                }
                int lureTuningLevel = profile.job(JobType.FISHER).nodeLevel(SkillNode.FISHER_LURE_TUNING);
                
                BlockPos hookPos = player.fishing.blockPosition();
                boolean inFishery = false;
                for (java.util.Map.Entry<BlockPos, FisheryZone> zoneEntry : FISHERY_ZONES.entrySet()) {
                    BlockPos center = zoneEntry.getKey();
                    FisheryZone zone = zoneEntry.getValue();
                    
                    // Y축 편차 완화 검사 (+- 4블록 이내)
                    if (Math.abs(hookPos.getY() - center.getY()) <= 4) {
                        double dx = hookPos.getX() - center.getX();
                        double dz = hookPos.getZ() - center.getZ();
                        double dist2DSq = dx * dx + dz * dz;
                        double radius = Math.min(18.0D, 5.0D + zone.skillLevel * 1.4D);
                        if (dist2DSq <= radius * radius) {
                            inFishery = true;
                            break;
                        }
                    }
                }
                
                boolean inHotspot = false;
                net.minecraft.nbt.CompoundTag playerNbt = player.getPersistentData();
                if (playerNbt.contains("nogeon_hotspot_x")) {
                    BlockPos hotspot = new BlockPos(
                        playerNbt.getInt("nogeon_hotspot_x"),
                        playerNbt.getInt("nogeon_hotspot_y"),
                        playerNbt.getInt("nogeon_hotspot_z")
                    );
                    
                    // Y축 편차 완화 검사 (+- 4블록 이내)
                    if (Math.abs(hookPos.getY() - hotspot.getY()) <= 4) {
                        double dx = hookPos.getX() - hotspot.getX();
                        double dz = hookPos.getZ() - hotspot.getZ();
                        double dist2DSq = dx * dx + dz * dz;
                        double hotspotRadius = Math.min(6.5D, 3.0D + lureTuningLevel * 0.4D);
                        if (dist2DSq <= hotspotRadius * hotspotRadius) {
                            inHotspot = true;
                        }
                    }
                }
                
                // 입질 시간 감소 폭 대폭 상향 패치 (체감 만족도 극대화)
                int ticksToReduce = 0;
                if (lureTuningLevel > 0) {
                    ticksToReduce += Math.min(10, (lureTuningLevel + 2) / 3);
                }
                if (inFishery) {
                    ticksToReduce += 18;
                } else if (inHotspot) {
                    ticksToReduce += 10;
                }

                ItemStack rod = findFishingRod(player);
                if (!rod.isEmpty()) {
                    int level = SmithingService.level(rod);
                    if (level > 0) {
                        ticksToReduce += (level + 1) / 2;
                    }
                    double reforgeSpeed = SmithEvents.reforgeValue(rod, "fishing_speed");
                    if (reforgeSpeed > 0.0D) {
                        ticksToReduce += (int) Math.round(reforgeSpeed * 20.0D);
                    }
                }
                
                if (ticksToReduce > 0) {
                    timeUntilLureField.setInt(player.fishing, Math.max(1, currentTime - ticksToReduce));
                }
            }
        } catch (Exception e) {
            System.err.println("[NoGeon Economy Land] Exception in accelerateFishingLure reflection:");
            e.printStackTrace();
        }
    }

    public static void onWorldTick(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START || event.level.isClientSide) {
            return;
        }

        if (event.level instanceof ServerLevel sLevel) {
            long curTime = event.level.getGameTime();
            java.util.List<HarvestCheck> toRemove = new java.util.ArrayList<>();
            for (HarvestCheck check : PENDING_HARVEST_CHECKS) {
                if (check.level.dimension().equals(sLevel.dimension()) && curTime > check.gameTime) {
                    toRemove.add(check);
                    BlockState newState = sLevel.getBlockState(check.pos);
                    boolean harvested = false;
                    boolean alreadyHarvested = false;
                    if (newState.isAir() || newState.getBlock() != check.oldState.getBlock()) {
                        harvested = true;
                        alreadyHarvested = true;
                    } else {
                        int oldAge = getBlockAge(check.oldState);
                        int newAge = getBlockAge(newState);
                        if (newAge < oldAge) {
                            harvested = true;
                            alreadyHarvested = true;
                        } else if (newState.getBlock() instanceof CropBlock && isCropMaxAge(newState)) {
                            harvested = true;
                        }
                    }
                    if (harvested) {
                        ServerPlayer player = check.player;
                        if (player.isAlive()) {
                            EconomyState econState = EconomyState.get(player.server);
                            PlayerProfile profile = econState.profile(player.getUUID());
                            if (profile.selectedJob() == JobType.FARMER) {
                                int exp = farmerCropExp(check.oldState);
                                addExp(player, JobType.FARMER, exp);
                                grantActivityCredits(profile, econState, JobType.FARMER, Math.max(20L, exp * 3L));
                                applyFarmerPerks(player, check.oldState, check.pos, alreadyHarvested);
                            }
                        }
                    }
                }
            }
            PENDING_HARVEST_CHECKS.removeAll(toRemove);
        }

        if (event.level.getGameTime() % 20 == 0) {
            for (java.util.Map.Entry<BlockPos, Integer> entry : new java.util.ArrayList<>(FERTILE_SOILS.entrySet())) {
                BlockPos pos = entry.getKey();
                int remaining = entry.getValue() - 20;
                if (remaining <= 0) {
                    FERTILE_SOILS.remove(pos, entry.getValue());
                    continue;
                }

                BlockPos cropPos = pos.above();
                BlockState cropState = event.level.getBlockState(cropPos);
                if (cropState.getBlock() instanceof CropBlock && canGrowCrop(cropState)) {
                    event.level.setBlock(cropPos, growCropAge(cropState, 1), 3);

                    if (event.level instanceof ServerLevel sLevel) {
                        sLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, cropPos.getX() + 0.5, cropPos.getY() + 0.3, cropPos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.0);
                    }
                }
                FERTILE_SOILS.put(pos, remaining);
            }

            FISHERY_ZONES.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                FisheryZone zone = entry.getValue();
                int remaining = zone.ticksRemaining - 20;
                if (remaining <= 0) {
                    return true;
                }

                if (event.level instanceof ServerLevel sLevel) {
                    ServerPlayer owner = sLevel.getServer().getPlayerList().getPlayer(zone.owner);
                    if (owner == null || EconomyState.get(owner.server).profile(owner.getUUID()).selectedJob() != JobType.FISHER) {
                        return true;
                    }
                    double radius = Math.min(18.0D, 5.0D + zone.skillLevel * 1.4D);
                    for (int i = 0; i < 24; i++) {
                        double angle = (i / 24.0D) * Math.PI * 2.0D + (event.level.getGameTime() * 0.08D);
                        double ox = Math.cos(angle) * radius;
                        double oz = Math.sin(angle) * radius;
                        sLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5D + ox, pos.getY() + 0.85D, pos.getZ() + 0.5D + oz, 1, 0, 0.08D, 0, 0.0D);
                        if (i % 2 == 0) {
                            sLevel.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5D + ox, pos.getY() + 1.0D, pos.getZ() + 0.5D + oz, 1, 0.0D, 0.03D, 0.0D, 0.0D);
                            sLevel.sendParticles(ParticleTypes.FISHING, pos.getX() + 0.5D + ox, pos.getY() + 1.1D, pos.getZ() + 0.5D + oz, 1, 0.0D, 0.03D, 0.0D, 0.0D);
                        }
                    }

                    sLevel.sendParticles(ParticleTypes.FISHING, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 14, radius * 0.45D, 0.05D, radius * 0.45D, 0.0D);
                    for (int i = 0; i < Math.min(36, 10 + zone.skillLevel * 3); i++) {
                        double px = pos.getX() + 0.5D + (sLevel.random.nextDouble() - 0.5D) * radius * 1.5D;
                        double py = pos.getY() + 0.8D;
                        double pz = pos.getZ() + 0.5D + (sLevel.random.nextDouble() - 0.5D) * radius * 1.5D;
                        sLevel.sendParticles(ParticleTypes.BUBBLE, px, py, pz, 1, 0.0D, 0.1D, 0.0D, 0.02D);
                        if (sLevel.random.nextDouble() < 0.3D) {
                            sLevel.sendParticles(ParticleTypes.GLOW, px, py, pz, 1, 0.0D, 0.02D, 0.0D, 0.0D);
                            sLevel.sendParticles(ParticleTypes.FISHING, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        }
                    }
                }

                zone.ticksRemaining = remaining;
                return false;
            });
            
            if (event.level instanceof ServerLevel sLevel) {
                java.util.List<net.minecraft.world.entity.decoration.ArmorStand> stands = sLevel.getEntitiesOfClass(
                    net.minecraft.world.entity.decoration.ArmorStand.class,
                    new net.minecraft.world.phys.AABB(Double.NEGATIVE_INFINITY, -64, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 320, Double.POSITIVE_INFINITY),
                    e -> e.getPersistentData().contains("nogeon_scarecrow")
                );

                for (net.minecraft.world.entity.decoration.ArmorStand stand : stands) {
                    int skillLevel = stand.getPersistentData().getInt("nogeon_scarecrow_level");
                    if (skillLevel <= 0) skillLevel = 1;
                    int radius = Math.min(28, 8 + skillLevel * 2);

                    int gauge = stand.getPersistentData().getInt("nogeon_scarecrow_gauge");
                    if (gauge >= 100) {
                        stand.getPersistentData().putInt("nogeon_scarecrow_gauge", 0);
                        BlockPos center = stand.blockPosition();
                        int grown = 0;
                        for (int attempt = 0; attempt < 100 && grown < Math.min(45, 15 + skillLevel * 3); attempt++) {
                            int rx = center.getX() + sLevel.random.nextInt(radius * 2 + 1) - radius;
                            int ry = center.getY() + sLevel.random.nextInt(6) - 3;
                            int rz = center.getZ() + sLevel.random.nextInt(radius * 2 + 1) - radius;
                            BlockPos targetPos = new BlockPos(rx, ry, rz);
                            BlockState state = sLevel.getBlockState(targetPos);
                            if (state.getBlock() instanceof CropBlock && canGrowCrop(state)) {
                                sLevel.setBlock(targetPos, growCropAge(state, 2 + sLevel.random.nextInt(2)), 3);
                                sLevel.sendParticles(ParticleTypes.HEART, rx + 0.5D, ry + 0.5D, rz + 0.5D, 3, 0.2D, 0.2D, 0.2D, 0.0D);
                                grown++;
                            }
                        }
                        sLevel.playSound(null, stand.getX(), stand.getY(), stand.getZ(),
                            net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.5F);
                    } else {
                        BlockPos center = stand.blockPosition();
                        int grown = 0;
                        for (int attempt = 0; attempt < 30 && grown < 2 + skillLevel; attempt++) {
                            int rx = center.getX() + sLevel.random.nextInt(radius * 2 + 1) - radius;
                            int ry = center.getY() + sLevel.random.nextInt(6) - 3;
                            int rz = center.getZ() + sLevel.random.nextInt(radius * 2 + 1) - radius;
                            BlockPos targetPos = new BlockPos(rx, ry, rz);
                            BlockState state = sLevel.getBlockState(targetPos);
                            if (state.getBlock() instanceof CropBlock && canGrowCrop(state)) {
                                sLevel.setBlock(targetPos, growCropAge(state, 1), 3);
                                sLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, rx + 0.5D, ry + 0.3D, rz + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                                grown++;
                            }
                        }
                        sLevel.sendParticles(ParticleTypes.WAX_OFF, stand.getX(), stand.getY() + 1.2D, stand.getZ(), 3, 0.2D, 0.4D, 0.2D, 0.05D);
                    }
                }
            }
        }
    }

    public static void onCheckSpawn(net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }

        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
            event.getX() - 45, event.getY() - 20, event.getZ() - 45,
            event.getX() + 45, event.getY() + 20, event.getZ() + 45
        );

        java.util.List<net.minecraft.world.entity.decoration.ArmorStand> stands = event.getLevel().getEntitiesOfClass(
            net.minecraft.world.entity.decoration.ArmorStand.class,
            area,
            e -> e.getPersistentData().contains("nogeon_scarecrow")
        );

        for (net.minecraft.world.entity.decoration.ArmorStand stand : stands) {
            int skillLevel = stand.getPersistentData().getInt("nogeon_scarecrow_level");
            if (skillLevel <= 0) skillLevel = 1;
            int radius = Math.min(45, 15 + skillLevel * 3);
            
            double distSqr = stand.distanceToSqr(event.getX(), event.getY(), event.getZ());
            if (distSqr <= radius * radius) {
                event.setSpawnCancelled(true);
                event.setCanceled(true);
                return;
            }
        }

        java.util.List<FarmerScarecrowEntity> scarecrows = event.getLevel().getEntitiesOfClass(
            FarmerScarecrowEntity.class,
            area
        );
        for (FarmerScarecrowEntity scarecrow : scarecrows) {
            int radius = Math.min(45, 15 + scarecrow.skillLevel() * 3);
            double distSqr = scarecrow.distanceToSqr(event.getX(), event.getY(), event.getZ());
            if (distSqr <= radius * radius) {
                event.setSpawnCancelled(true);
                event.setCanceled(true);
                return;
            }
        }
    }

    public static void onEntityJoinLevel(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof net.minecraft.world.entity.decoration.ArmorStand stand)) {
            return;
        }

        net.minecraft.world.entity.player.Player creator = event.getLevel().getNearestPlayer(stand, 5.0D);
        if (creator instanceof ServerPlayer player) {
            PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
            int scarecrowLevel = profile.job(JobType.FARMER).nodeLevel(SkillNode.FARMER_SUNLIT_STEP);
            if (profile.selectedJob() == JobType.FARMER && scarecrowLevel > 0) {
                stand.getPersistentData().putBoolean("nogeon_scarecrow", true);
                stand.getPersistentData().putInt("nogeon_scarecrow_level", scarecrowLevel);
                stand.getPersistentData().putInt("nogeon_scarecrow_gauge", 0);
                stand.getPersistentData().putString("nogeon_scarecrow_owner", player.getStringUUID());

                stand.setCustomName(Component.literal("§e농부의 허수아비 (Lv." + scarecrowLevel + ")"));
                stand.setCustomNameVisible(true);

                stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
                stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));

                player.displayClientMessage(Component.literal("§a✨ [허수아비 배치] §f농부의 허수아비가 설치되었습니다! (스폰 방지 및 농사 축복 활성화)"), false);
                
                ServerLevel sLevel = (ServerLevel) event.getLevel();
                sLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, stand.getX(), stand.getY() + 1.0D, stand.getZ(), 30, 0.4D, 0.4D, 0.4D, 0.05D);
                sLevel.playSound(null, stand.getX(), stand.getY(), stand.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    public static void onCropGrowPre(net.minecraftforge.event.level.BlockEvent.CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel sLevel)) {
            return;
        }

        BlockPos pos = event.getPos();
        for (ServerPlayer player : sLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() == sLevel) {
                PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
                int earthMiracleLevel = profile.job(JobType.FARMER).nodeLevel(SkillNode.FARMER_EARTH_MIRACLE);
                if (profile.selectedJob() == JobType.FARMER && earthMiracleLevel > 0) {
                    double radius = Math.min(25.0D, 5.0D + earthMiracleLevel * 2.0D);
                    if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= radius * radius) {
                        event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
                        
                        BlockPos below = pos.below();
                        BlockState belowState = sLevel.getBlockState(below);
                        if (belowState.is(Blocks.FARMLAND)) {
                            int moisture = belowState.getValue(FarmBlock.MOISTURE);
                            if (moisture < 7) {
                                sLevel.setBlock(below, belowState.setValue(FarmBlock.MOISTURE, 7), 2);
                            }
                        }
                        return;
                    }
                }
            }
        }
        
        for (ServerPlayer player : sLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() == sLevel) {
                PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
                int soilStudyLevel = profile.job(JobType.FARMER).nodeLevel(SkillNode.FARMER_SOIL_STUDY);
                if (profile.selectedJob() == JobType.FARMER && soilStudyLevel > 0) {
                    if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 144.0D) {
                        if (player.getRandom().nextDouble() < scaledPercent(soilStudyLevel, 0.0125D, 0.40D)) {
                            event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
                            return;
                        }
                    }
                }
            }
        }
    }

    public static ItemStack createRandomApotheosisGem(ServerPlayer player) {
        try {
            Class<?> gemRegistryClass = Class.forName("dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry");
            java.lang.reflect.Method method = gemRegistryClass.getMethod("createRandomGemStack", 
                net.minecraft.util.RandomSource.class, 
                net.minecraft.server.level.ServerLevel.class, 
                float.class, 
                java.util.function.Predicate[].class);
            
            java.util.function.Predicate[] emptyFilters = (java.util.function.Predicate[]) java.lang.reflect.Array.newInstance(java.util.function.Predicate.class, 0);
            
            return (ItemStack) method.invoke(null, player.getRandom(), player.serverLevel(), 0.0F, emptyFilters);
        } catch (Exception e) {
            // Safe fallback when not present or fails
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack rollCompressionGem(ServerPlayer player) {
        double roll = player.getRandom().nextDouble() * 10000.0D;
        if (roll < 0.5D) {
            return new ItemStack(com.nogeon.economyland.item.ModItems.PERFECT_ENHANCEMENT_GEM.get());
        } else if (roll < 3.0D) {
            return new ItemStack(com.nogeon.economyland.item.ModItems.FLAWLESS_ENHANCEMENT_GEM.get());
        } else if (roll < 12.0D) {
            return new ItemStack(com.nogeon.economyland.item.ModItems.ENHANCEMENT_GEM.get());
        } else if (roll < 50.0D) {
            return new ItemStack(com.nogeon.economyland.item.ModItems.FLAWED_ENHANCEMENT_GEM.get());
        } else if (roll < 150.0D) {
            return new ItemStack(com.nogeon.economyland.item.ModItems.SPLIT_ENHANCEMENT_GEM.get());
        } else if (roll < 500.0D) {
            return new ItemStack(com.nogeon.economyland.item.ModItems.CRACKED_ENHANCEMENT_GEM.get());
        } else {
            ItemStack gem = createRandomApotheosisGem(player);
            if (gem.isEmpty()) {
                return new ItemStack(com.nogeon.economyland.item.ModItems.CRACKED_ENHANCEMENT_GEM.get());
            }
            return gem;
        }
    }

    public static void onRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        net.minecraft.world.level.block.state.BlockState clickedState = event.getLevel().getBlockState(event.getPos());
        if (isFarmerHarvestAllowed(clickedState, player.serverLevel(), event.getPos()) 
            && canRewardBlockBreak(player, event.getPos())) {
            PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
            if (profile.selectedJob() == JobType.FARMER) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(clickedState.getBlock());
                boolean isVanillaCrop = blockId.getNamespace().equals("minecraft");

                if (isVanillaCrop) {
                    event.setCanceled(true);
                    applyFarmerPerks(player, clickedState, event.getPos(), false);
                    int exp = farmerCropExp(clickedState);
                    addExp(player, JobType.FARMER, exp);
                    grantActivityCredits(profile, EconomyState.get(player.server), JobType.FARMER, Math.max(20L, exp * 3L));
                    player.swing(event.getHand(), true);
                    return;
                } else if (!player.isShiftKeyDown()) {
                    PENDING_HARVEST_CHECKS.removeIf(check -> check.player == player && check.level.dimension().equals(player.serverLevel().dimension()) && check.pos.equals(event.getPos()));
                    PENDING_HARVEST_CHECKS.add(new HarvestCheck(player, player.serverLevel(), event.getPos(), clickedState, player.level().getGameTime()));
                }
            }
        }

        if (event.isCanceled()) {
            return;
        }

        if (player.getVehicle() instanceof com.nogeon.economyland.entity.ScrapDroneEntity drone) {
            int boosterLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_booster_level");
            if (boosterLvl <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_booster")) {
                boosterLvl = 1;
            }
            if (boosterLvl > 0) {
                double boostCost = 10.0D - (boosterLvl - 1) * 1.5D; // scales from 10.0 to 4.0
                if (drone.getCharge() >= boostCost) {
                    drone.consumeCharge(boostCost);
                    player.getPersistentData().putInt("nogeon_engineer_drone_boost_ticks", 15);
                    
                    ServerLevel sLevel = player.serverLevel();
                    sLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, drone.getX(), drone.getY() - 0.5D, drone.getZ(), 15, 0.2D, 0.2D, 0.2D, 0.05D);
                    sLevel.sendParticles(ParticleTypes.CLOUD, drone.getX(), drone.getY() - 0.5D, drone.getZ(), 10, 0.1D, 0.1D, 0.1D, 0.1D);
                    sLevel.playSound(null, drone.getX(), drone.getY(), drone.getZ(),
                        SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.PLAYERS, 1.2F, 1.2F);
                    sLevel.playSound(null, drone.getX(), drone.getY(), drone.getZ(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
                    
                    player.displayClientMessage(Component.literal("§b[압축 공기 추진 부스터] §f급가속 추진을 시작합니다! (현재 동력 소모: §e" + String.format("%.1f", boostCost) + "%§f)"), true);
                } else {
                    player.displayClientMessage(Component.literal("§c[추진 부스터] 동력이 부족합니다. (최소 " + String.format("%.1f", boostCost) + "% 필요)"), true);
                }
                event.setCanceled(true);
                return;
            }
        }

        ItemStack stack = event.getItemStack();
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());

        if (profile.selectedJob() == JobType.ENGINEER
            && player.isShiftKeyDown()
            && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
            && stack.isEmpty()) {
            BlockPos pos = event.getPos();
            int transmitterLevel = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_transmitter_level");
            if (transmitterLevel <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_transmitter")) {
                transmitterLevel = 1;
            }
            if (transmitterLevel <= 0) {
                return;
            }
            net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
            if (be != null) {
                net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> cap =
                    be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, null);
                if (cap.isPresent()) {
                    player.getPersistentData().putLong("nogeon_engineer_drone_linked_chest_pos", pos.asLong());
                    player.getPersistentData().putString("nogeon_engineer_drone_linked_chest_dim", player.level().dimension().location().toString());
                    player.displayClientMessage(Component.literal("§a[안테나 링크] §f보관함(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")을 드론 무선 전송 위치로 지정했습니다."), false);
                    player.level().playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.5F);
                    event.setCanceled(true);
                }
            }
        }
    }

    public static void onLeftClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.isCanceled()) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty()) {
            net.minecraft.world.level.block.state.BlockState clickedState = event.getLevel().getBlockState(event.getPos());
            if (isFarmerHarvestAllowed(clickedState, player.serverLevel(), event.getPos())) {
                PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
                if (profile.selectedJob() == JobType.FARMER) {
                    event.setCanceled(true);
                    applyFarmerPerks(player, clickedState, event.getPos(), false);
                    int exp = farmerCropExp(clickedState);
                    addExp(player, JobType.FARMER, exp);
                    grantActivityCredits(profile, EconomyState.get(player.server), JobType.FARMER, Math.max(20L, exp * 3L));
                    player.swing(event.getHand(), true);
                }
            }
        }
    }

    public static void onRightClickItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.getVehicle() instanceof com.nogeon.economyland.entity.ScrapDroneEntity drone) {
            int boosterLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_booster_level");
            if (boosterLvl <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_booster")) {
                boosterLvl = 1;
            }
            if (boosterLvl > 0) {
                double boostCost = 10.0D - (boosterLvl - 1) * 1.5D; // scales from 10.0 to 4.0
                if (drone.getCharge() >= boostCost) {
                    drone.consumeCharge(boostCost);
                    player.getPersistentData().putInt("nogeon_engineer_drone_boost_ticks", 15);
                    
                    ServerLevel sLevel = player.serverLevel();
                    sLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, drone.getX(), drone.getY() - 0.5D, drone.getZ(), 15, 0.2D, 0.2D, 0.2D, 0.05D);
                    sLevel.sendParticles(ParticleTypes.CLOUD, drone.getX(), drone.getY() - 0.5D, drone.getZ(), 10, 0.1D, 0.1D, 0.1D, 0.1D);
                    sLevel.playSound(null, drone.getX(), drone.getY(), drone.getZ(),
                        SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.PLAYERS, 1.2F, 1.2F);
                    sLevel.playSound(null, drone.getX(), drone.getY(), drone.getZ(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
                    
                    player.displayClientMessage(Component.literal("§b[압축 공기 추진 부스터] §f급가속 추진을 시작합니다! (현재 동력 소모: §e" + String.format("%.1f", boostCost) + "%§f)"), true);
                } else {
                    player.displayClientMessage(Component.literal("§c[추진 부스터] 동력이 부족합니다. (최소 " + String.format("%.1f", boostCost) + "% 필요)"), true);
                }
                event.setCanceled(true);
                return;
            }
        }
        
        ItemStack stack = event.getItemStack();
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());


        int crateTier = deepseaCrateTier(stack);
        if (crateTier > 0) {
            int tier = crateTier;
            
            stack.shrink(1);
            event.setCanceled(true);
            
            int rewardCount = player.getRandom().nextInt(3) + 1;
            String[] categories = new String[] { "weapon", "gun_bow", "armor", "item" };
            
            player.displayClientMessage(Component.literal("§6📦 [크레이트 개봉] §f심해 크레이트를 개봉하고 있습니다..."), false);
            
            for (int i = 0; i < rewardCount; i++) {
                String cat = categories[player.getRandom().nextInt(categories.length)];
                ItemStack rewardStack = rollGacha(player, cat, tier);
                if (!rewardStack.isEmpty()) {
                    ExtendedInventoryDelivery.giveOrDrop(player, rewardStack);
                    player.displayClientMessage(Component.literal("  §e- ✨ " + rewardStack.getHoverName().getString() + " §7(x" + rewardStack.getCount() + ") 획득!"), false);
                }
            }
            
            ServerLevel sLevel = player.serverLevel();
            sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            
            if (tier >= 4) {
                sLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 0.4F, 1.2F);
            }
            
            sLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0D, player.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.1D);
            sLevel.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.05D);
        }
    }

    private static int deepseaCrateTier(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.hasTag() && stack.getTag().contains("nogeon_crate_tier")) {
            return Math.max(1, Math.min(4, stack.getTag().getInt("nogeon_crate_tier")));
        }
        if (stack.getItem() instanceof com.nogeon.economyland.item.DeepseaCrateItem crateItem) {
            return crateItem.tier();
        }
        return 0;
    }

    public static ItemStack rollGacha(ServerPlayer player, String categoryId, int crateTier) {
        com.nogeon.economyland.state.TraderShopState shopState = com.nogeon.economyland.state.TraderShopState.get(player.server);
        EconomyState ecoState = EconomyState.get(player.server);
        java.util.List<ShopEntry> entries = shopState.gachaRewardEntries(ecoState, categoryId);
        if (entries == null || entries.isEmpty()) {
            return new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT);
        }
        
        java.util.List<ShopEntry> common = new java.util.ArrayList<>();
        java.util.List<ShopEntry> rare = new java.util.ArrayList<>();
        java.util.List<ShopEntry> epic = new java.util.ArrayList<>();
        java.util.List<ShopEntry> legendary = new java.util.ArrayList<>();
        
        for (ShopEntry entry : entries) {
            int rarity = Math.max(0, Math.min(3, entry.stack().getOrCreateTag().getInt("NoGeonGachaRarity")));
            if (rarity == 3) {
                legendary.add(entry);
            } else if (rarity == 2) {
                epic.add(entry);
            } else if (rarity == 1) {
                rare.add(entry);
            } else {
                common.add(entry);
            }
        }
        
        if (common.isEmpty()) common.addAll(entries);
        if (rare.isEmpty()) rare.addAll(common);
        if (epic.isEmpty()) epic.addAll(rare);
        if (legendary.isEmpty()) legendary.addAll(epic);
        
        java.util.List<ShopEntry> chosenPool = switch (Math.max(1, Math.min(4, crateTier))) {
            case 1 -> common;
            case 2 -> rare;
            case 3 -> epic;
            default -> legendary;
        };
        
        long totalWeight = 0;
        for (ShopEntry entry : chosenPool) {
            totalWeight += Math.max(1, entry.price());
        }
        
        long target = (long) (player.getRandom().nextDouble() * totalWeight);
        long current = 0;
        for (ShopEntry entry : chosenPool) {
            current += Math.max(1, entry.price());
            if (current >= target) {
                return entry.stack().copy();
            }
        }
        return chosenPool.get(0).stack().copy();
    }

    private static ItemStack rollAdditionalFisherReward(ServerPlayer player, int skillLevel) {
        double rand = player.getRandom().nextDouble() * 100.0D;
        int rewardType;
        if (skillLevel == 1) {
            if (rand < 70) rewardType = 0;
            else if (rand < 90) rewardType = 1;
            else rewardType = 2;
        } else if (skillLevel == 2) {
            if (rand < 50) rewardType = 0;
            else if (rand < 65) rewardType = 1;
            else rewardType = 2;
        } else {
            if (rand < 30) rewardType = 0;
            else if (rand < 40) rewardType = 1;
            else rewardType = 2;
        }

        if (rewardType == 0) {
            Item[] fishPool = new Item[] { Items.COD, Items.SALMON, Items.PUFFERFISH, Items.TROPICAL_FISH };
            return new ItemStack(fishPool[player.getRandom().nextInt(fishPool.length)]);
        } else if (rewardType == 1) {
            Item[] junkPool = new Item[] { Items.LEATHER, Items.BOWL, Items.ROTTEN_FLESH, Items.BONE, Items.APPLE };
            return new ItemStack(junkPool[player.getRandom().nextInt(junkPool.length)]);
        } else {
            ItemStack[] treasurePool = new ItemStack[] {
                new ItemStack(Items.IRON_INGOT, player.getRandom().nextInt(3) + 1),
                new ItemStack(Items.GOLD_INGOT, player.getRandom().nextInt(2) + 1),
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.EMERALD, player.getRandom().nextInt(4) + 1),
                new ItemStack(Items.BOOK)
            };
            return treasurePool[player.getRandom().nextInt(treasurePool.length)];
        }
    }

    private static ItemStack generateDeepseaCrate(ServerPlayer player, int skillLevel) {
        double rand = player.getRandom().nextDouble() * 100.0D;
        int tier;
        double quality = Math.min(1.0D, Math.max(0.0D, skillLevel / 10.0D));
        double woodCutoff = 55.0D - quality * 35.0D;
        double stoneCutoff = 85.0D - quality * 20.0D;
        double ironCutoff = 98.0D - quality * 8.0D;
        if (rand < woodCutoff) tier = 1;
        else if (rand < stoneCutoff) tier = 2;
        else if (rand < ironCutoff) tier = 3;
        else tier = 4;

        ItemStack crate;
        String prefix;
        switch (tier) {
            case 1:
                crate = new ItemStack(com.nogeon.economyland.item.ModItems.DEEPSEA_CRATE_WOOD.get());
                prefix = "\u00a76[\ub098\ubb34] ";
                break;
            case 2:
                crate = new ItemStack(com.nogeon.economyland.item.ModItems.DEEPSEA_CRATE_STONE.get());
                prefix = "\u00a77[\ub3cc] ";
                break;
            case 3:
                crate = new ItemStack(com.nogeon.economyland.item.ModItems.DEEPSEA_CRATE_IRON.get());
                prefix = "\u00a7f[\ucca0] ";
                break;
            case 4:
            default:
                crate = new ItemStack(com.nogeon.economyland.item.ModItems.DEEPSEA_CRATE_DIAMOND.get());
                prefix = "\u00a7b[\ub2e4\uc774\uc544] ";
                break;
        }

        prefix = switch (tier) {
            case 1 -> "\u00a76[\ub098\ubb34] ";
            case 2 -> "\u00a77[\ub3cc] ";
            case 3 -> "\u00a7f[\ucca0] ";
            default -> "\u00a7b[\ub2e4\uc774\uc544] ";
        };

        CompoundTag tag = crate.getOrCreateTag();
        tag.putInt("nogeon_crate_tier", tier);

        String customNameJson = Component.Serializer.toJson(Component.literal(prefix + "심해 크레이트"));
        crate.getOrCreateTagElement("display").putString("Name", customNameJson);

        ListTag loreList = new ListTag();
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7[보물 찾기] 심해 속 건져올린 고대 상자"))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§e들고 우클릭하여 개봉 시 가챠 무기/방어구/총기 획득!"))));
        crate.getOrCreateTagElement("display").put("Lore", loreList);

        String fixedNameJson = Component.Serializer.toJson(Component.literal(prefix + "\uc2ec\ud574 \ud06c\ub808\uc774\ud2b8"));
        crate.getOrCreateTagElement("display").putString("Name", fixedNameJson);
        ListTag fixedLoreList = new ListTag();
        fixedLoreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("\u00a77[\ubcf4\ubb3c \ucc3e\uae30] \ub0da\uc2dc\ub85c \uac74\uc838 \uc62c\ub9b0 \uc2ec\ud574 \uc0c1\uc790"))));
        fixedLoreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("\u00a7e\ub4e4\uace0 \uc6b0\ud074\ub9ad\ud558\uba74 \ub4f1\uae09\ubcc4 \uac00\ucc28 \ubcf4\uc0c1 1~3\uac1c \ud68d\ub4dd"))));
        crate.getOrCreateTagElement("display").put("Lore", fixedLoreList);

        return crate;
    }

    private static void growNearbyCropsBountiful(ServerLevel level, BlockState harvestedState, BlockPos origin, int skillLevel) {
        int radius = skillLevel;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockPos targetPos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(targetPos);
                    if (state.getBlock() == harvestedState.getBlock() && state.getBlock() instanceof CropBlock && canGrowCrop(state)) {
                        level.setBlock(targetPos, growCropAge(state, 1), 3);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5D, targetPos.getY() + 0.3D, targetPos.getZ() + 0.5D, 2, 0.1D, 0.1D, 0.1D, 0.0D);
                    }
                }
            }
        }
    }

    private static void chargeNearbyScarecrow(ServerPlayer player, BlockPos pos, int amount) {
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
            pos.getX() - 25, pos.getY() - 10, pos.getZ() - 25,
            pos.getX() + 25, pos.getY() + 10, pos.getZ() + 25
        );
        java.util.List<net.minecraft.world.entity.decoration.ArmorStand> stands = player.level().getEntitiesOfClass(
            net.minecraft.world.entity.decoration.ArmorStand.class,
            area,
            e -> e.getPersistentData().contains("nogeon_scarecrow")
        );
        
        for (net.minecraft.world.entity.decoration.ArmorStand stand : stands) {
            int currentGauge = stand.getPersistentData().getInt("nogeon_scarecrow_gauge");
            stand.getPersistentData().putInt("nogeon_scarecrow_gauge", Math.min(100, currentGauge + amount * 3));
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, stand.getX(), stand.getY() + 2.0D, stand.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.0D);
        }
    }

    private static void applyPlusGrade(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("nogeon_plus_grade", true);
        
        String originalName = stack.getHoverName().getString();
        String customNameJson = Component.Serializer.toJson(Component.literal("§e" + originalName + "+"));
        stack.getOrCreateTagElement("display").putString("Name", customNameJson);
        
        ListTag loreList = new ListTag();
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7[대지의 기적] 최고급 농산물"))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§6가치 2배 (납품가/판매가 200%)"))));
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§a요리 재료 사용 시 버프 효과 50% 증폭"))));
        stack.getOrCreateTagElement("display").put("Lore", loreList);
    }

    private static ItemStack findSeedInInventory(ServerPlayer player, Block crop, BlockPos pos, BlockState state) {
        Item seedItem = null;
        if (crop instanceof CropBlock cropBlock) {
            seedItem = cropBlock.getCloneItemStack(player.level(), pos, state).getItem();
            if (cropBlock == Blocks.WHEAT) {
                seedItem = Items.WHEAT_SEEDS;
            } else if (cropBlock == Blocks.BEETROOTS) {
                seedItem = Items.BEETROOT_SEEDS;
            } else if (cropBlock == Blocks.CARROTS) {
                seedItem = Items.CARROT;
            } else if (cropBlock == Blocks.POTATOES) {
                seedItem = Items.POTATO;
            }
        } else if (crop == Blocks.COCOA) {
            seedItem = Items.COCOA_BEANS;
        }
        
        if (seedItem != null) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(seedItem)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onLivingKnockBack(net.minecraftforge.event.entity.living.LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getPersistentData().contains("nogeon_immunity_ticks")) {
                event.setCanceled(true);
            } else if (player.getPersistentData().contains("nogeon_engineer_kinetic_boost_ticks")) {
                event.setStrength(event.getStrength() * 0.5F); // 넉백 50% 감쇄
            }
        }
    }

    public static void handleEngineerSkill(ServerPlayer player, EconomyState state, PlayerProfile profile, int slot) {
        if (profile.selectedJob() != JobType.ENGINEER) {
            return;
        }

        long currentTick = player.level().getGameTime();

        if (slot == 1) { // Compression GUI (자원 압축 - Lv.25 Active)
            int compressionLevel = profile.job(JobType.ENGINEER).nodeLevel(SkillNode.ENGINEER_COMPRESSION);
            if (compressionLevel <= 0) {
                player.displayClientMessage(Component.literal("§c먼저 [자원 압축] 스킬을 배워야 합니다."), true);
                return;
            }
            com.nogeon.economyland.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new com.nogeon.economyland.network.OpenCompressionScreenPacket(0)
            );
        } else if (slot == 2) { // Perpetual Motion (영구 기관 - Lv.100 Ultimate Active)
            int kineticBoostLevel = profile.job(JobType.ENGINEER).nodeLevel(SkillNode.ENGINEER_KINETIC_BOOST);
            if (kineticBoostLevel <= 0) {
                player.displayClientMessage(Component.literal("§c먼저 [영구 기관] 스킬을 배워야 합니다."), true);
                return;
            }

            boolean broken = player.getPersistentData().getBoolean("nogeon_engineer_drone_broken");
            if (broken) {
                player.displayClientMessage(Component.literal("§c[오토 스크랩 드론] 드론이 파괴되어 기동할 수 없습니다. 수리 창을 엽니다."), true);
                com.nogeon.economyland.menu.DeconstructOpener.open(player, -1, null);
                return;
            }

            boolean isKineticActive = player.getPersistentData().contains("nogeon_engineer_kinetic_boost_ticks");
            if (isKineticActive) {
                // Toggle off
                player.getPersistentData().remove("nogeon_engineer_kinetic_boost_ticks");
                player.getPersistentData().remove("nogeon_engineer_kinetic_boost_level");
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.2F);
                player.displayClientMessage(Component.literal("§c[오토 스크랩 드론] §f드론 기동을 중단하고 회수 모드를 종료합니다."), true);
            } else {
                player.getPersistentData().putInt("nogeon_engineer_kinetic_boost_ticks", 100);
                player.getPersistentData().putInt("nogeon_engineer_kinetic_boost_level", kineticBoostLevel);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.2F);
                player.displayClientMessage(Component.literal("§a[오토 스크랩 드론] §f지원형 오토 스크랩 드론을 기동합니다! (지원 사격, 동력 쉴드, 나노 수리/치유, 고철 회수, 진공 흡입, 총기 보조 활성화)"), true);
            }
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onGunShoot(com.tacz.guns.api.event.common.GunShootEvent event) {
        if (event.getLogicalSide().isClient()) {
            return;
        }
        if (!(event.getShooter() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack gunStack = event.getGunItemStack();
        com.tacz.guns.api.item.IGun iGun = com.tacz.guns.api.item.IGun.getIGunOrNull(gunStack);
        if (iGun == null) {
            return;
        }

        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        if (profile.selectedJob() == JobType.ENGINEER) {
            int processOptLevel = profile.job(JobType.ENGINEER).nodeLevel(SkillNode.ENGINEER_PROCESS_OPTIMIZATION);
            if (processOptLevel > 0) {
                double saveChance = processOptLevel * 3.0D; // 3% per level, up to 30%
                boolean isKineticActive = player.getPersistentData().contains("nogeon_engineer_kinetic_boost_ticks");
                if (isKineticActive) {
                    saveChance += 30.0D; // +30% chance when Perpetual Engine is active!
                }

                if (player.getRandom().nextDouble() * 100.0D < saveChance) {
                    int currentAmmo = iGun.getCurrentAmmoCount(gunStack);
                    iGun.setCurrentAmmoCount(gunStack, currentAmmo + 1);
                    player.displayClientMessage(Component.literal("§a[공정 최적화] 탄약 절약!"), true);
                }
            }
        }
    }

    public static void triggerDroneExpression(ServerPlayer player, int expressionId, int durationTicks) {
        boolean droneBroken = player.getPersistentData().getBoolean("nogeon_engineer_drone_broken");
        if (droneBroken) return;
        
        AABB searchBox = player.getBoundingBox().inflate(32.0D);
        List<com.nogeon.economyland.entity.ScrapDroneEntity> drones = player.level().getEntitiesOfClass(
            com.nogeon.economyland.entity.ScrapDroneEntity.class, searchBox
        );
        for (com.nogeon.economyland.entity.ScrapDroneEntity drone : drones) {
            java.util.Optional<java.util.UUID> ownerOpt = drone.getOwnerUuid();
            if (ownerOpt.isPresent() && ownerOpt.get().equals(player.getUUID())) {
                if (drone.getCharge() > 0) {
                    drone.triggerExpression(expressionId, durationTicks);
                    break;
                }
            }
        }
    }
}
