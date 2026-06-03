package com.nogeon.economyland.entity;

import com.nogeon.economyland.menu.DeconstructOpener;
import com.nogeon.economyland.network.SyncCreditsPacket;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ScrapDroneEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> SKILL_LEVEL =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DRONE_CHARGE =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DRONE_EXPRESSION =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> UPG_INV =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> UPG_TRANS =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> UPG_BOOST =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DRONE_NAME =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> TRANS_LEVEL =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BOOST_LEVEL =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SENSOR_LEVEL =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GRABBER_LEVEL =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BOOST_TICKS =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> EQUIP_GUN =
        SynchedEntityData.defineId(ScrapDroneEntity.class, EntityDataSerializers.ITEM_STACK);

    private int combatTicks = 0;
    private int attackCooldown = 0;
    private double preciseCharge = 100.0;
    private int burstCount = 0;
    private int burstDelay = 0;
    private LivingEntity burstTarget = null;
    private int tempExpr = 0;
    private int tempExprTicks = 0;

    private static java.lang.reflect.Field jumpingField;
    private static java.lang.reflect.Field xxaField;
    private static java.lang.reflect.Field zzaField;

    static {
        try {
            try {
                jumpingField = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("jumping");
            } catch (NoSuchFieldException e) {
                jumpingField = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("f_20899_");
            }
            jumpingField.setAccessible(true);
            
            try {
                xxaField = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("xxa");
            } catch (NoSuchFieldException e) {
                xxaField = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("f_20900_");
            }
            xxaField.setAccessible(true);
            
            try {
                zzaField = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("zza");
            } catch (NoSuchFieldException e) {
                zzaField = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("f_20902_");
            }
            zzaField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean getRiderJumping(LivingEntity rider) {
        if (jumpingField != null) {
            try {
                return jumpingField.getBoolean(rider);
            } catch (Exception e) {}
        }
        return false;
    }

    private float getRiderXxa(LivingEntity rider) {
        if (xxaField != null) {
            try {
                return xxaField.getFloat(rider);
            } catch (Exception e) {}
        }
        return 0.0F;
    }

    private float getRiderZza(LivingEntity rider) {
        if (zzaField != null) {
            try {
                return zzaField.getFloat(rider);
            } catch (Exception e) {}
        }
        return 0.0F;
    }

    public ScrapDroneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
    }

    private boolean canOccupy(double x, double y, double z) {
        return level().noCollision(this, getBoundingBox().move(x - getX(), y - getY(), z - getZ()));
    }

    private void moveDroneSafely(double x, double y, double z) {
        if (canOccupy(x, y, z)) {
            this.setPos(x, y, z);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SKILL_LEVEL, 1);
        entityData.define(OWNER, Optional.empty());
        entityData.define(DRONE_CHARGE, 100);
        entityData.define(DRONE_EXPRESSION, 0);
        entityData.define(UPG_INV, false);
        entityData.define(UPG_TRANS, false);
        entityData.define(UPG_BOOST, false);
        entityData.define(DRONE_NAME, "오토 스크랩 드론");
        entityData.define(TRANS_LEVEL, 0);
        entityData.define(BOOST_LEVEL, 0);
        entityData.define(SENSOR_LEVEL, 0);
        entityData.define(GRABBER_LEVEL, 0);
        entityData.define(BOOST_TICKS, 0);
        entityData.define(EQUIP_GUN, ItemStack.EMPTY);
    }

    public ItemStack getEquipGun() {
        return entityData.get(EQUIP_GUN);
    }

    public void setEquipGun(ItemStack stack) {
        entityData.set(EQUIP_GUN, stack);
    }

    public int skillLevel() {
        return Math.max(1, entityData.get(SKILL_LEVEL));
    }

    public Optional<UUID> getOwnerUuid() {
        return entityData.get(OWNER);
    }

    public int getCharge() {
        return entityData.get(DRONE_CHARGE);
    }

    public void setCharge(int charge) {
        int clamped = Math.max(0, Math.min(100, charge));
        entityData.set(DRONE_CHARGE, clamped);
        this.preciseCharge = clamped;
        
        Optional<UUID> ownerOpt = getOwnerUuid();
        if (ownerOpt.isPresent() && !level().isClientSide) {
            net.minecraft.world.entity.player.Player owner = level().getPlayerByUUID(ownerOpt.get());
            if (owner != null) {
                owner.getPersistentData().putInt("nogeon_engineer_drone_charge", clamped);
            }
        }
    }

    public void consumeCharge(double amount) {
        preciseCharge = Math.max(0.0, Math.min(100.0, preciseCharge - amount));
        int prev = getCharge();
        int next = (int) preciseCharge;
        if (prev != next) {
            entityData.set(DRONE_CHARGE, next);
            updateCustomName();
        }
        
        Optional<UUID> ownerOpt = getOwnerUuid();
        if (ownerOpt.isPresent() && !level().isClientSide) {
            net.minecraft.world.entity.player.Player owner = level().getPlayerByUUID(ownerOpt.get());
            if (owner != null) {
                owner.getPersistentData().putInt("nogeon_engineer_drone_charge", next);
            }
        }
    }

    public void addCharge(double amount) {
        preciseCharge = Math.max(0.0, Math.min(100.0, preciseCharge + amount));
        int prev = getCharge();
        int next = (int) preciseCharge;
        if (prev != next) {
            entityData.set(DRONE_CHARGE, next);
            updateCustomName();
        }
        
        Optional<UUID> ownerOpt = getOwnerUuid();
        if (ownerOpt.isPresent() && !level().isClientSide) {
            net.minecraft.world.entity.player.Player owner = level().getPlayerByUUID(ownerOpt.get());
            if (owner != null) {
                owner.getPersistentData().putInt("nogeon_engineer_drone_charge", next);
            }
        }
    }

    public double getFuelPowerValue(String itemId) {
        if (itemId.equals("minecraft:cobblestone") || itemId.equals("minecraft:cobbled_deepslate") || itemId.equals("minecraft:stone") || itemId.equals("minecraft:deepslate")) {
            return 1.0D;
        }
        if (itemId.equals("minecraft:coal") || itemId.equals("minecraft:charcoal") || itemId.equals("minecraft:coal_block")) {
            return itemId.equals("minecraft:coal_block") ? 45.0D : 5.0D;
        }
        if (itemId.equals("minecraft:redstone") || itemId.equals("minecraft:lapis_lazuli")) {
            return 10.0D;
        }
        if (itemId.equals("minecraft:iron_ingot") || itemId.equals("minecraft:copper_ingot") || itemId.contains("zinc_ingot")) {
            return 15.0D;
        }
        if (itemId.equals("minecraft:gold_ingot") || itemId.contains("brass_ingot")) {
            return 25.0D;
        }
        if (itemId.equals("minecraft:diamond") || itemId.equals("minecraft:emerald")) {
            return 80.0D;
        }
        if (itemId.contains("cogwheel") || itemId.contains("shaft") || itemId.contains("gear")) {
            return 40.0D;
        }
        return 2.0D;
    }

    public int getExpression() {
        return entityData.get(DRONE_EXPRESSION);
    }

    public void setExpression(int expression) {
        entityData.set(DRONE_EXPRESSION, expression);
    }

    public boolean hasUpgradeInventory() {
        return entityData.get(UPG_INV);
    }

    public int getTransLevel() {
        return entityData.get(TRANS_LEVEL);
    }

    public int getBoostLevel() {
        return entityData.get(BOOST_LEVEL);
    }

    public int getSensorLevel() {
        return entityData.get(SENSOR_LEVEL);
    }

    public int getGrabberLevel() {
        return entityData.get(GRABBER_LEVEL);
    }

    public boolean hasUpgradeTransmitter() {
        return entityData.get(UPG_TRANS) || getTransLevel() > 0;
    }

    public boolean hasUpgradeBooster() {
        return entityData.get(UPG_BOOST) || getBoostLevel() > 0;
    }

    public String getDroneName() {
        return entityData.get(DRONE_NAME);
    }
    
    public void setDroneName(String name) {
        entityData.set(DRONE_NAME, name);
        updateCustomName();
    }

    public void setup(UUID owner, int skillLevel) {
        entityData.set(OWNER, Optional.of(owner));
        entityData.set(SKILL_LEVEL, Math.max(1, skillLevel));
        
        int initialCharge = 100;
        String initialName = "오토 스크랩 드론";
        int healthLvl = 1;
        if (!level().isClientSide) {
            net.minecraft.world.entity.player.Player p = level().getPlayerByUUID(owner);
            if (p != null) {
                if (p.getPersistentData().contains("nogeon_engineer_drone_charge")) {
                    initialCharge = p.getPersistentData().getInt("nogeon_engineer_drone_charge");
                }
                if (p.getPersistentData().contains("nogeon_engineer_drone_name")) {
                    initialName = p.getPersistentData().getString("nogeon_engineer_drone_name");
                }
                healthLvl = p.getPersistentData().getInt("nogeon_engineer_drone_stat_health");
                if (healthLvl <= 0) healthLvl = 1;
            }
        }
        
        setDroneName(initialName);
        setCharge(initialCharge);
        updateCustomName();
        setCustomNameVisible(true);

        double maxHealth = 20.0D + healthLvl * 10.0D;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        this.setHealth((float) maxHealth);
    }

    public void updateCustomName() {
        String customBase = getDroneName();
        if (customBase.isEmpty()) {
            customBase = "오토 스크랩 드론";
        }
        if (getCharge() <= 0) {
            setCustomName(Component.literal("§6" + customBase + " §7(동력: §c방전됨§7)"));
        } else {
            setCustomName(Component.literal("§6" + customBase + " §7(동력: §e" + getCharge() + "%§7)"));
        }
    }


    @Override
    public void tick() {
        super.tick();
        
        if (this.isDeadOrDying()) {
            return;
        }

        Optional<UUID> ownerUuidOpt = getOwnerUuid();
        if (!ownerUuidOpt.isPresent()) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        Player owner = level().getPlayerByUUID(ownerUuidOpt.get());
        if (owner == null || !owner.isAlive()) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        // 1초마다 중복 생성 여부를 감지하여 나중에 생성된 드론은 스스로 자멸(discard)
        if (!level().isClientSide && tickCount % 20 == 0) {
            AABB checkRange = getBoundingBox().inflate(8.0D);
            List<ScrapDroneEntity> duplicateDrones = level().getEntitiesOfClass(
                ScrapDroneEntity.class, checkRange,
                other -> other != this && other.getOwnerUuid().map(uuid -> uuid.equals(ownerUuidOpt.get())).orElse(false)
            );
            if (!duplicateDrones.isEmpty()) {
                boolean shouldDiscard = false;
                for (ScrapDroneEntity other : duplicateDrones) {
                    if (other.getId() < this.getId()) {
                        shouldDiscard = true;
                        break;
                    }
                }
                if (shouldDiscard) {
                    discard();
                    return;
                }
            }
        }

        // Server-only data synchronization from owner persistent data to entityData
        if (!level().isClientSide) {
            boolean upgInv = owner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_inventory");
            ItemStack currentGun = ItemStack.EMPTY;
            if (upgInv && owner.getPersistentData().contains("nogeon_engineer_drone_gun")) {
                currentGun = ItemStack.of(owner.getPersistentData().getCompound("nogeon_engineer_drone_gun"));
            }
            if (!ItemStack.matches(getEquipGun(), currentGun)) {
                setEquipGun(currentGun);
                this.setItemSlot(EquipmentSlot.MAINHAND, currentGun);
            }
            boolean upgTrans = owner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_transmitter");
            boolean upgBoost = owner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_booster");
            
            if (entityData.get(UPG_INV) != upgInv) {
                entityData.set(UPG_INV, upgInv);
            }
            if (entityData.get(UPG_TRANS) != upgTrans) {
                entityData.set(UPG_TRANS, upgTrans);
            }
            if (entityData.get(UPG_BOOST) != upgBoost) {
                entityData.set(UPG_BOOST, upgBoost);
            }

            int transLvl = owner.getPersistentData().getInt("nogeon_engineer_drone_upgrade_transmitter_level");
            if (transLvl <= 0 && owner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_transmitter")) {
                transLvl = 1;
            }
            int boostLvl = owner.getPersistentData().getInt("nogeon_engineer_drone_upgrade_booster_level");
            if (boostLvl <= 0 && owner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_booster")) {
                boostLvl = 1;
            }
            int sensorLvl = owner.getPersistentData().getInt("nogeon_engineer_drone_upgrade_sensor_level");
            if (sensorLvl <= 0 && owner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_sensor")) {
                sensorLvl = 1;
            }
            int grabberLvl = owner.getPersistentData().getInt("nogeon_engineer_drone_upgrade_grabber_level");
            if (grabberLvl <= 0 && owner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_grabber")) {
                grabberLvl = 1;
            }
            int boostTicks = owner.getPersistentData().getInt("nogeon_engineer_drone_boost_ticks");

            if (entityData.get(TRANS_LEVEL) != transLvl) {
                entityData.set(TRANS_LEVEL, transLvl);
            }
            if (entityData.get(BOOST_LEVEL) != boostLvl) {
                entityData.set(BOOST_LEVEL, boostLvl);
            }
            if (entityData.get(SENSOR_LEVEL) != sensorLvl) {
                entityData.set(SENSOR_LEVEL, sensorLvl);
            }
            if (entityData.get(GRABBER_LEVEL) != grabberLvl) {
                entityData.set(GRABBER_LEVEL, grabberLvl);
            }
            if (entityData.get(BOOST_TICKS) != boostTicks) {
                entityData.set(BOOST_TICKS, boostTicks);
            }
        }

        // Expression and timer tracking (both sides)
        if (tempExprTicks > 0) {
            tempExprTicks--;
            setExpression(tempExpr);
        } else if (getCharge() <= 0) {
            setExpression(1); // LOW_POWER
            combatTicks = 0;
        } else if (getCharge() <= 20) {
            setExpression(4); // WORRIED
        } else if (combatTicks > 0) {
            combatTicks--;
            setExpression(2); // COMBAT
        } else {
            setExpression(0); // NORMAL
        }

        // Client-side visual hover effect
        if (level().isClientSide) {
            double rx = getX() + (random.nextDouble() - 0.5D) * 0.1D;
            double ry = getY() - 0.1D;
            double rz = getZ() + (random.nextDouble() - 0.5D) * 0.1D;
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, rx, ry, rz, 0.0D, -0.01D, 0.0D);
        }

        // 1. Movement logic (Both client and server)
        boolean isRidden = false;
        Entity passenger = getFirstPassenger();
        if (passenger instanceof Player pRider && pRider.getUUID().equals(owner.getUUID())) {
            isRidden = true;
            double speed = 0.35D;
            int boostTicks = entityData.get(BOOST_TICKS);
            int boosterLvl = entityData.get(BOOST_LEVEL);
            
            // 탑승 중 달리기(Sprint) 키 입력 시 자동 부스트 충전
            if (boosterLvl > 0 && pRider.isSprinting() && boostTicks <= 0) {
                double boostCost = 10.0D - (boosterLvl - 1) * 1.5D; // scales from 10.0 to 4.0
                if (getCharge() >= boostCost) {
                    consumeCharge(boostCost);
                    boostTicks = 15;
                    entityData.set(BOOST_TICKS, boostTicks);
                    
                    if (!level().isClientSide) {
                        owner.getPersistentData().putInt("nogeon_engineer_drone_boost_ticks", boostTicks);
                        ServerLevel sLevel = (ServerLevel) level();
                        sLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() - 0.5D, this.getZ(), 15, 0.2D, 0.2D, 0.2D, 0.05D);
                        sLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() - 0.5D, this.getZ(), 10, 0.1D, 0.1D, 0.1D, 0.1D);
                        sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.PLAYERS, 1.2F, 1.2F);
                        sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
                    }
                }
            }
            
            if (boostTicks > 0) {
                double boostSpeed = 1.05D;
                if (boosterLvl > 0) {
                    boostSpeed = 1.05D + (boosterLvl - 1) * (0.95D / 4.0D);
                }
                speed = boostSpeed;
                
                // Tick down boost ticks on server only, and save to player persistent data
                if (!level().isClientSide) {
                    owner.getPersistentData().putInt("nogeon_engineer_drone_boost_ticks", boostTicks - 1);
                    entityData.set(BOOST_TICKS, boostTicks - 1);
                    
                    if (owner.tickCount % 2 == 0) {
                        ServerLevel sLevel = (ServerLevel) level();
                        int flameCount = 2 + boosterLvl;
                        int smokeCount = 3 + boosterLvl;
                        sLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() - 0.6D, this.getZ(), flameCount, 0.05D * boosterLvl, 0.05D, 0.05D * boosterLvl, 0.01D * boosterLvl);
                        sLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() - 0.6D, this.getZ(), smokeCount, 0.1D * boosterLvl, 0.1D, 0.1D * boosterLvl, 0.01D);
                        if (boosterLvl >= 3) {
                            sLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() - 0.6D, this.getZ(), boosterLvl - 2, 0.05D, 0.05D, 0.05D, 0.02D);
                        }
                        if (boosterLvl >= 5) {
                            sLevel.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() - 0.6D, this.getZ(), 3, 0.1D, 0.1D, 0.1D, 0.05D);
                        }
                    }
                }
            }
            if (getCharge() <= 0) {
                speed = 0.0D;
            }
            
            double dx = 0;
            double dy = 0;
            double dz = 0;
            
            Vec3 lookVec = pRider.getLookAngle();
            float xxa = getRiderXxa(pRider);
            float zza = getRiderZza(pRider);
            boolean jumping = getRiderJumping(pRider);
            
            if (zza > 0) { // Forward
                dx += lookVec.x * speed;
                dy += lookVec.y * speed;
                dz += lookVec.z * speed;
            } else if (zza < 0) { // Backward
                dx -= lookVec.x * (speed * 0.5);
                dy -= lookVec.y * (speed * 0.5);
                dz -= lookVec.z * (speed * 0.5);
            }
            
            if (xxa != 0) { // Strafe
                Vec3 side = lookVec.yRot((float)Math.toRadians(90)).normalize();
                dx += side.x * xxa * (speed * 0.5);
                dz += side.z * xxa * (speed * 0.5);
            }
            
            if (jumping) { // Space (Upwards)
                dy += 0.25D;
            }
            
            if (level().isClientSide) {
                moveDroneSafely(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
                this.setRot(pRider.getYRot(), pRider.getXRot());
            }
            
            if (!level().isClientSide && (dx != 0 || dy != 0 || dz != 0) && pRider.tickCount % 20 == 0) {
                consumeCharge(1.0D);
            }
        } else {
            // Hover & follow logic (서버 측에서만 위치 연산을 수행하여 클라이언트 떨림을 방지합니다)
            if (!level().isClientSide) {
                double angleRad = Math.toRadians((owner.tickCount * 4) % 360);
                double targetX = owner.getX() + 1.35D * Math.cos(angleRad);
                double targetY = owner.getY() + 2.25D + 0.15D * Math.sin(Math.toRadians((owner.tickCount * 8) % 360));
                double targetZ = owner.getZ() + 1.35D * Math.sin(angleRad);
                
                double lerpX = this.getX() + (targetX - this.getX()) * 0.20D;
                double lerpY = this.getY() + (targetY - this.getY()) * 0.20D;
                double lerpZ = this.getZ() + (targetZ - this.getZ()) * 0.20D;
                
                moveDroneSafely(lerpX, lerpY, lerpZ);
                if (burstTarget != null && burstTarget.isAlive()) {
                    double tDx = burstTarget.getX() - this.getX();
                    double tDy = (burstTarget.getY() + burstTarget.getEyeHeight() / 2.0D) - this.getY();
                    double tDz = burstTarget.getZ() - this.getZ();
                    double tDist = Math.sqrt(tDx * tDx + tDz * tDz);
                    float tPitch = (float) -Math.toDegrees(Math.atan2(tDy, tDist));
                    float tYaw = (float) Math.toDegrees(Math.atan2(tDz, tDx)) - 90.0F;
                    this.setRot(tYaw, tPitch);
                    this.yHeadRot = tYaw;
                    this.yBodyRot = tYaw;
                } else {
                    this.setRot(owner.getYRot(), owner.getXRot());
                    this.yHeadRot = owner.getYHeadRot();
                    this.yBodyRot = owner.yBodyRot;
                }
            }
        }

        // Server-side active functionalities
        if (!level().isClientSide) {
            ServerLevel sLevel = (ServerLevel) level();
            ServerPlayer sOwner = (ServerPlayer) owner;

            // Low battery mechanical whimper warning sound under 20%
            if (getCharge() <= 20 && getCharge() > 0 && sOwner.tickCount % 80 == 0) {
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.4F, 0.5F + random.nextFloat() * 0.1F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ALLAY_HURT, SoundSource.NEUTRAL, 0.5F, 0.6F);
            }

            // Homing guiding code for Fireballs/WitherSkulls
            List<net.minecraft.world.entity.projectile.Projectile> projectiles = sLevel.getEntitiesOfClass(
                net.minecraft.world.entity.projectile.Projectile.class,
                this.getBoundingBox().inflate(32.0D),
                proj -> proj.getOwner() == this && (proj instanceof net.minecraft.world.entity.projectile.LargeFireball || proj instanceof net.minecraft.world.entity.projectile.WitherSkull)
            );
            for (net.minecraft.world.entity.projectile.Projectile proj : projectiles) {
                List<LivingEntity> potentialTargets = sLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    proj.getBoundingBox().inflate(16.0D),
                    entity -> isDroneCombatTarget(entity, sOwner)
                );
                LivingEntity homingTarget = null;
                double minDist = Double.MAX_VALUE;
                for (LivingEntity t : potentialTargets) {
                    double d = proj.distanceToSqr(t);
                    if (d < minDist) {
                        minDist = d;
                        homingTarget = t;
                    }
                }
                if (homingTarget != null) {
                    Vec3 targetPos = homingTarget.position().add(0, homingTarget.getEyeHeight() / 2.0D, 0);
                    Vec3 dir = targetPos.subtract(proj.position()).normalize();
                    double speed = proj.getDeltaMovement().length();
                    if (speed < 0.1D) speed = 0.5D;
                    Vec3 newMotion = proj.getDeltaMovement().scale(0.85D).add(dir.scale(0.15D * speed)).normalize().scale(speed);
                    proj.setDeltaMovement(newMotion);
                    if (proj instanceof net.minecraft.world.entity.projectile.LargeFireball fb) {
                        fb.xPower = newMotion.x * 0.1D;
                        fb.yPower = newMotion.y * 0.1D;
                        fb.zPower = newMotion.z * 0.1D;
                    } else if (proj instanceof net.minecraft.world.entity.projectile.WitherSkull ws) {
                        ws.xPower = newMotion.x * 0.1D;
                        ws.yPower = newMotion.y * 0.1D;
                        ws.zPower = newMotion.z * 0.1D;
                    }
                }
            }

            // 2. Kinetic Charge Management (Auto-charging near active Create machinery, natural depletion, auto-fuel)
            boolean nearCreate = sOwner.getPersistentData().getBoolean("nogeon_near_create");
            if (nearCreate) {
                if (sOwner.tickCount % 10 == 0 && getCharge() < 100) {
                    addCharge(2.0D);
                    sLevel.sendParticles(ParticleTypes.FALLING_NECTAR, this.getX(), this.getY() + 0.2D, this.getZ(), 4, 0.1D, 0.1D, 0.1D, 0.01D);
                    sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.01D);
                }
            } else {
                if (sOwner.tickCount % 100 == 0 && getCharge() > 0) {
                    consumeCharge(1.0D);
                }
                if (sOwner.tickCount % 100 == 0 && getCharge() < 100 && getCharge() > 0) {
                    if (level().isDay() && level().canSeeSky(this.blockPosition())) {
                        addCharge(1.0D);
                        sLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.2D, this.getZ(), 3, 0.1D, 0.1D, 0.1D, 0.0D);
                    }
                }
            }

            // Passive Recovery
            if (!isRidden && combatTicks <= 0 && getCharge() < 100) {
                if (sOwner.tickCount % 40 == 0) {
                    addCharge(1.0D);
                    sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.2D, this.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.0D);
                }
            }

            // Auto Refuel Scanning Logic (every 40 ticks / 2 seconds)
            if (sOwner.tickCount % 40 == 0) {
                String autoFuelId = sOwner.getPersistentData().getString("nogeon_engineer_drone_autofuel_item");
                if (!autoFuelId.isEmpty()) {
                    double fuelVal = getFuelPowerValue(autoFuelId);
                    if (getCharge() <= 100 - fuelVal) {
                        net.minecraft.resources.ResourceLocation fuelRes = net.minecraft.resources.ResourceLocation.tryParse(autoFuelId);
                        if (fuelRes != null) {
                            net.minecraft.world.item.Item targetFuelItem = BuiltInRegistries.ITEM.get(fuelRes);
                            if (targetFuelItem != Items.AIR) {
                                ItemStack targetStack = new ItemStack(targetFuelItem);
                                boolean found = false;
                                for (int i = 0; i < 36; i++) {
                                    ItemStack invStack = sOwner.getInventory().getItem(i);
                                    if (!invStack.isEmpty() && invStack.getItem() == targetFuelItem) {
                                        invStack.shrink(1);
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    List<ItemStack> backpacks = com.nogeon.economyland.player.ExtendedInventoryDelivery.findAllBackpacks(sOwner);
                                    for (ItemStack backpack : backpacks) {
                                        int consumed = com.nogeon.economyland.player.ExtendedInventoryDelivery.consumeInBackpack(backpack, targetStack, 1);
                                        if (consumed > 0) {
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                                if (found) {
                                    addCharge(fuelVal);
                                    triggerExpression(3, 40); // Cute expression + Sound
                                    sOwner.displayClientMessage(Component.literal("§6[오토 스크랩 드론] §e" + targetStack.getHoverName().getString() + "§f을(를) 자동 소모하여 동력을 충전했습니다. (현재 동력: §e" + getCharge() + "%§f)"), true);
                                }
                            }
                        }
                    }
                }
            }

            boolean overdrive = getCharge() > 0;
            if (overdrive) {
                // 3. Nanite Auto-Repair (1% of item max damage)
                int repairInterval = 10;
                if (sOwner.tickCount % repairInterval == 0) {
                    boolean repaired = false;
                    for (EquipmentSlot slotType : EquipmentSlot.values()) {
                        ItemStack equip = sOwner.getItemBySlot(slotType);
                        if (!equip.isEmpty() && equip.isDamaged()) {
                            int maxDmg = equip.getMaxDamage();
                            int repairVal = Math.max(5, (int) (maxDmg * 0.01D));
                            equip.setDamageValue(Math.max(0, equip.getDamageValue() - repairVal));
                            repaired = true;
                        }
                    }
                    if (repaired) {
                        consumeCharge(1.0D);
                    }
                }

                // 4. Nanite Healing (1 HP every 2s / 40 ticks)
                if (sOwner.tickCount % 40 == 0) {
                    if (sOwner.getHealth() < sOwner.getMaxHealth()) {
                        sOwner.heal(1.0F);
                        consumeCharge(2.0D);
                        int healthLvl = sOwner.getPersistentData().getInt("nogeon_engineer_drone_stat_health");
                        if (healthLvl <= 0) healthLvl = 1;
                        float healVol = 0.5F + healthLvl * 0.15F;
                        float healPitch = 1.5F - healthLvl * 0.1F;
                        sLevel.playSound(null, sOwner.getX(), sOwner.getY(), sOwner.getZ(),
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, healVol, healPitch);
                        if (healthLvl >= 1) {
                            sLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, sOwner.getX(), sOwner.getY() + 1.0D, sOwner.getZ(), 5 * healthLvl, 0.3D, 0.5D, 0.3D, 0.05D);
                        }
                        if (healthLvl >= 3) {
                            sLevel.sendParticles(ParticleTypes.HEART, sOwner.getX(), sOwner.getY() + 1.2D, sOwner.getZ(), healthLvl - 1, 0.2D, 0.4D, 0.2D, 0.0D);
                        }
                        if (healthLvl >= 5) {
                            sLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sOwner.getX(), sOwner.getY() + 1.0D, sOwner.getZ(), 10, 0.4D, 0.6D, 0.4D, 0.1D);
                        }
                    }
                }

                // 5. Support Fire targeting closest monster within range
                if (attackCooldown > 0) {
                    attackCooldown--;
                }
                
                // Burst Fire queue execution
                if (burstCount > 0) {
                    if (burstDelay > 0) {
                        burstDelay--;
                    } else {
                        if (isDroneCombatTarget(burstTarget, sOwner) && this.distanceToSqr(burstTarget) <= supportRangeSqr(sOwner)) {
                            boolean hasWeaponUpg = sOwner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_inventory");
                            boolean hasGun = hasWeaponUpg && sOwner.getPersistentData().contains("nogeon_engineer_drone_gun");
                            ItemStack gunStack = ItemStack.EMPTY;
                            GunType gunType = GunType.DEFAULT;
                            if (hasGun) {
                                gunStack = ItemStack.of(sOwner.getPersistentData().getCompound("nogeon_engineer_drone_gun"));
                                gunType = getGunType(gunStack);
                            }
                            int attackLvl = sOwner.getPersistentData().getInt("nogeon_engineer_drone_stat_attack");
                            if (attackLvl <= 0) attackLvl = 1;

                            boolean fired = executeFire(burstTarget, gunStack, gunType, attackLvl, sLevel, sOwner);
                            if (fired) {
                                burstCount--;
                                burstDelay = computeBurstDelay(gunStack);
                            } else {
                                if (burstCount > 0) {
                                    burstDelay = 2;
                                }
                            }
                        } else {
                            burstCount = 0;
                            burstTarget = null;
                        }
                    }
                }

                if (attackCooldown <= 0 && burstCount <= 0) {
                    double supportRange = supportRange(sOwner);
                    AABB targetBox = this.getBoundingBox().inflate(supportRange);
                    List<LivingEntity> targets = sLevel.getEntitiesOfClass(LivingEntity.class, targetBox,
                        entity -> isDroneCombatTarget(entity, sOwner)
                    );
                    if (!targets.isEmpty()) {
                        LivingEntity closest = null;
                        double minDist = Double.MAX_VALUE;
                        for (LivingEntity t : targets) {
                            double dist = this.distanceToSqr(t);
                            if (dist < minDist) {
                                minDist = dist;
                                closest = t;
                            }
                        }
                        if (closest != null) {
                            boolean hasWeaponUpg = sOwner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_inventory");
                            boolean hasGun = hasWeaponUpg && sOwner.getPersistentData().contains("nogeon_engineer_drone_gun");
                            int attackLvl = sOwner.getPersistentData().getInt("nogeon_engineer_drone_stat_attack");
                            if (attackLvl <= 0) attackLvl = 1;

                            ItemStack gunStack = ItemStack.EMPTY;
                            GunType gunType = GunType.DEFAULT;
                            if (hasGun) {
                                gunStack = ItemStack.of(sOwner.getPersistentData().getCompound("nogeon_engineer_drone_gun"));
                                gunType = getGunType(gunStack);
                            }

                            attackCooldown = computeAttackCooldown(gunType, attackLvl, gunStack);
                            burstCount = computeBurstCount(gunType, attackLvl, gunStack);
                            burstDelay = 0;
                            burstTarget = closest;
                        }
                    }
                }

                // 6. Shield absorption refills
                int shieldInterval = 100;
                if (sOwner.tickCount % shieldInterval == 0) {
                    float maxHealth = sOwner.getMaxHealth();
                    float maxCap = maxHealth * 0.3F;
                    float currentAbsorbed = sOwner.getAbsorptionAmount();
                    if (currentAbsorbed < maxCap) {
                        float refill = Math.min(maxCap - currentAbsorbed, maxHealth * 0.1F);
                        sOwner.setAbsorptionAmount(currentAbsorbed + refill);
                        consumeCharge(4.0D);
                        sOwner.level().playSound(null, sOwner.getX(), sOwner.getY(), sOwner.getZ(),
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.5F);
                        sLevel.sendParticles(ParticleTypes.END_ROD, sOwner.getX(), sOwner.getY() + 1.0D, sOwner.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.05D);
                    }
                }

                // 7. Salvage recovery
                if (sOwner.tickCount % 200 == 0) {
                    int transLvl = entityData.get(TRANS_LEVEL);
                    double salvageChance = 0.15D;
                    if (transLvl > 0) {
                        salvageChance = 0.15D + (transLvl - 1) * 0.0625D;
                    } else {
                        salvageChance = 0.10D;
                    }

                    if (sOwner.getRandom().nextDouble() < salvageChance) {
                        boolean sentToChest = false;
                        double r = sOwner.getRandom().nextDouble();
                        ItemStack salvageStack;
                        if (r < 0.5D) {
                            salvageStack = new ItemStack(Items.IRON_NUGGET, 1 + sOwner.getRandom().nextInt(3));
                        } else if (r < 0.8D) {
                            salvageStack = new ItemStack(Items.COPPER_INGOT);
                        } else {
                            salvageStack = new ItemStack(Items.IRON_INGOT);
                        }

                        if (transLvl > 0 && sOwner.getPersistentData().contains("nogeon_engineer_drone_linked_chest_pos")) {
                            long posLong = sOwner.getPersistentData().getLong("nogeon_engineer_drone_linked_chest_pos");
                            net.minecraft.core.BlockPos linkedPos = net.minecraft.core.BlockPos.of(posLong);
                            String linkedDim = sOwner.getPersistentData().getString("nogeon_engineer_drone_linked_chest_dim");
                            if (sOwner.level().dimension().location().toString().equals(linkedDim)) {
                                net.minecraft.world.level.block.entity.BlockEntity be = sOwner.level().getBlockEntity(linkedPos);
                                if (be != null) {
                                    net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> cap =
                                        be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, null);
                                    if (cap.isPresent()) {
                                        net.minecraftforge.items.IItemHandler handler = cap.resolve().get();
                                        ItemStack remaining = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(handler, salvageStack, false);
                                        if (remaining.isEmpty()) {
                                            sentToChest = true;
                                            sLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 8, 0.1D, 0.1D, 0.1D, 0.0D);
                                            sOwner.level().playSound(null, sOwner.getX(), sOwner.getY(), sOwner.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.5F);
                                            sOwner.displayClientMessage(Component.literal("§6[고철 회수] §f드론이 회수한 §e" + salvageStack.getHoverName().getString() + "§f을(를) 링크된 보관함으로 무선 전송했습니다."), true);
                                        }
                                    }
                                }
                            }
                        }

                        if (!sentToChest) {
                            if (sOwner.getInventory().add(salvageStack)) {
                                sOwner.level().playSound(null, sOwner.getX(), sOwner.getY(), sOwner.getZ(),
                                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.5F);
                                sOwner.displayClientMessage(Component.literal("§6[고철 회수] §f드론이 주변 고철에서 §e" + salvageStack.getHoverName().getString() + "§f을(를) 회수했습니다."), true);
                            }
                        }
                    } else {
                        int kineticLvl = Math.max(1, sOwner.getPersistentData().getInt("nogeon_engineer_kinetic_boost_level"));
                        int rangeLvl = Math.max(1, sOwner.getPersistentData().getInt("nogeon_engineer_drone_stat_range"));
                        double r = sOwner.getRandom().nextDouble();
                        String scrapName;
                        int baseReward;
                        if (r < 0.45D) {
                            scrapName = "녹슨 톱니 조각";
                            baseReward = 80;
                        } else if (r < 0.75D) {
                            scrapName = "찌그러진 구리 배관";
                            baseReward = 140;
                        } else if (r < 0.92D) {
                            scrapName = "마모된 철제 베어링";
                            baseReward = 240;
                        } else if (r < 0.985D) {
                            scrapName = "부서진 정밀 부품";
                            baseReward = 420;
                        } else {
                            scrapName = "고밀도 합금 파편";
                            baseReward = 850;
                        }

                        EconomyState econState = EconomyState.get(sOwner.server);
                        PlayerProfile econProfile = econState.profile(sOwner.getUUID());
                        int engineerLevel = Math.max(100, econProfile.job(JobType.ENGINEER).level());
                        int creditReward = Math.max(75, Math.round((float) (baseReward * (1.0D + transLvl * 0.55D + kineticLvl * 0.18D + rangeLvl * 0.18D + engineerLevel * 0.012D))));
                        econProfile.addCredits(creditReward);
                        econState.setDirty();
                        SyncCreditsPacket.send(sOwner, econProfile.credits());
                        sOwner.level().playSound(null, sOwner.getX(), sOwner.getY(), sOwner.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
                        sOwner.displayClientMessage(Component.literal("§6[고철 회수] §f드론이 §e" + scrapName + "§f을(를) 분류해 매각했습니다. §e+" + creditReward + " C"), true);
                    }
                }
            }

            // 8. Vacuum magnetic pulling
            boolean magnetDisabled = sOwner.getPersistentData().getBoolean("nogeon_engineer_drone_magnet_disabled");
            if (!magnetDisabled) {
                int levelLvl = skillLevel();
                int rangeLvl = sOwner.getPersistentData().getInt("nogeon_engineer_drone_stat_range");
                if (rangeLvl <= 0) rangeLvl = 1;
                double radius = 12.0D + levelLvl * 1.0D + rangeLvl * 2.0D;
                AABB magnetBox = sOwner.getBoundingBox().inflate(radius);
                List<ItemEntity> items = sLevel.getEntitiesOfClass(ItemEntity.class, magnetBox);
                int pulledCount = 0;
                for (ItemEntity item : items) {
                    if (item.isAlive()) {
                        item.setNoGravity(true);
                        Vec3 target = sOwner.position().add(0, 0.5D, 0);
                        Vec3 motion = target.subtract(item.position()).normalize().scale(0.35D);
                        item.setDeltaMovement(motion);
                        if (random.nextInt(3) == 0) {
                            double ix = item.getX();
                            double iy = item.getY() + 0.2D;
                            double iz = item.getZ();
                            for (int step = 1; step <= 3; step++) {
                                double ratio = step / 3.0D;
                                double px = ix + (target.x - ix) * ratio * 0.5D;
                                double py = iy + (target.y - iy) * ratio * 0.5D;
                                double pz = iz + (target.z - iz) * ratio * 0.5D;
                                if (rangeLvl >= 5) {
                                    sLevel.sendParticles(ParticleTypes.PORTAL, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                                } else if (rangeLvl >= 3) {
                                    sLevel.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                                } else {
                                    sLevel.sendParticles(ParticleTypes.WITCH, px, py, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                                }
                            }
                        }
                        pulledCount++;
                    }
                }
                if (pulledCount > 0 && sOwner.tickCount % 20 == 0) {
                    consumeCharge(0.2D * Math.min(5, pulledCount));
                }
            }

            // Transmitter direct chest suction routing
            int transLvl = entityData.get(TRANS_LEVEL);
            if (transLvl > 0 && sOwner.getPersistentData().contains("nogeon_engineer_drone_linked_chest_pos")) {
                long posLong = sOwner.getPersistentData().getLong("nogeon_engineer_drone_linked_chest_pos");
                net.minecraft.core.BlockPos linkedPos = net.minecraft.core.BlockPos.of(posLong);
                String linkedDim = sOwner.getPersistentData().getString("nogeon_engineer_drone_linked_chest_dim");
                if (sOwner.level().dimension().location().toString().equals(linkedDim)) {
                    net.minecraft.world.level.block.entity.BlockEntity be = sOwner.level().getBlockEntity(linkedPos);
                    if (be != null) {
                        net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> cap =
                            be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, null);
                        if (cap.isPresent()) {
                            net.minecraftforge.items.IItemHandler handler = cap.resolve().get();
                            List<ItemEntity> closeItems = sLevel.getEntitiesOfClass(ItemEntity.class, sOwner.getBoundingBox().inflate(2.0D));
                            for (ItemEntity closeItem : closeItems) {
                                if (closeItem.isAlive()) {
                                    ItemStack stack = closeItem.getItem();
                                    ItemStack remaining = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(handler, stack, false);
                                    if (remaining.getCount() < stack.getCount()) {
                                        if (remaining.isEmpty()) {
                                            closeItem.discard();
                                        } else {
                                            closeItem.setItem(remaining);
                                        }
                                        sLevel.sendParticles(ParticleTypes.PORTAL, closeItem.getX(), closeItem.getY(), closeItem.getZ(), 8, 0.1D, 0.1D, 0.1D, 0.0D);
                                        sLevel.playSound(null, closeItem.getX(), closeItem.getY(), closeItem.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.5F);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public enum GunType {
        SNIPER, SHOTGUN, SMG, LMG, HEAVY, RIFLE, DEFAULT
    }

    private GunType getGunType(ItemStack gunStack) {
        if (gunStack.isEmpty()) return GunType.DEFAULT;
        String path = BuiltInRegistries.ITEM.getKey(gunStack.getItem()).getPath().toLowerCase();
        String name = gunStack.getHoverName().getString().toLowerCase();
        
        if (path.contains("rpg") || path.contains("grenade") || path.contains("bazooka") || path.contains("launcher") || path.contains("m79") || path.contains("m32") ||
            name.contains("rpg") || name.contains("grenade") || name.contains("bazooka") || name.contains("launcher") || name.contains("m79") || name.contains("m32")) {
            return GunType.HEAVY;
        }
        if (path.contains("sniper") || path.contains("m24") || path.contains("awp") || path.contains("barrett") || path.contains("scout") || path.contains("kar98") || path.contains("mosin") || path.contains("tac50") ||
            name.contains("sniper") || name.contains("저격")) {
            return GunType.SNIPER;
        }
        if (path.contains("shotgun") || path.contains("m870") || path.contains("double_barrel") || path.contains("saiga") || path.contains("aa12") || path.contains("nova") || path.contains("db") ||
            name.contains("shotgun") || name.contains("산탄") || name.contains("더블배럴")) {
            return GunType.SHOTGUN;
        }
        if (path.contains("vector") || path.contains("mp5") || path.contains("p90") || path.contains("uzi") || path.contains("mp7") || path.contains("ump") || path.contains("bizon") || path.contains("mac10") || path.contains("smg") ||
            name.contains("smg") || name.contains("기관단총") || name.contains("벡터")) {
            return GunType.SMG;
        }
        if (path.contains("m249") || path.contains("m60") || path.contains("rpk") || path.contains("mg42") || path.contains("mg3") || path.contains("minigun") || path.contains("lmg") ||
            name.contains("lmg") || name.contains("기관총")) {
            return GunType.LMG;
        }
        if (path.contains("rifle") || path.contains("ak47") || path.contains("m4a1") || path.contains("scar") || path.contains("hk416") || path.contains("aug") || path.contains("g36") || path.contains("fal") || path.contains("galil") || path.contains("m16") ||
            name.contains("소총") || name.contains("라이플") || name.contains("에이케이") || name.contains("엠포")) {
            return GunType.RIFLE;
        }
        return GunType.RIFLE;
    }

    private int normalizedAttackLevel(int attackLvl) {
        return Math.max(1, Math.min(5, attackLvl));
    }

    private int gunEnhanceLevel(ItemStack gunStack) {
        if (gunStack.isEmpty()) {
            return 0;
        }
        return Math.max(0, com.nogeon.economyland.item.SmithingService.level(gunStack));
    }

    private double gunReforgeValue(ItemStack gunStack, String modifierId) {
        if (gunStack.isEmpty()) {
            return 0.0D;
        }
        return Math.max(0.0D, com.nogeon.economyland.item.SmithEvents.reforgeValue(gunStack, modifierId));
    }

    private float gunReforgeDamageMultiplier(ItemStack gunStack) {
        double projectileDamage = gunReforgeValue(gunStack, "projectile_damage");
        double piercing = gunReforgeValue(gunStack, "piercing");
        return (float) (1.0D + projectileDamage + Math.min(0.35D, piercing * 0.5D));
    }

    private double gunRapidFireBonus(ItemStack gunStack) {
        return Math.min(0.35D, gunReforgeValue(gunStack, "rapid_fire"));
    }

    private double gunReloadMasteryBonus(ItemStack gunStack) {
        return Math.min(0.40D, gunReforgeValue(gunStack, "reload_mastery"));
    }

    private double gunChargeEfficiency(ItemStack gunStack) {
        double recoilControl = Math.min(0.30D, gunReforgeValue(gunStack, "recoil_control"));
        double reloadMastery = gunReloadMasteryBonus(gunStack);
        return Math.max(0.65D, 1.0D - recoilControl * 0.55D - reloadMastery * 0.25D);
    }

    private float attackLevelFlatBonus(int attackLvl) {
        return switch (normalizedAttackLevel(attackLvl)) {
            case 1 -> 0.0F;
            case 2 -> 5.0F;
            case 3 -> 12.0F;
            case 4 -> 22.0F;
            default -> 35.0F;
        };
    }

    private float gunEnhanceFlatBonus(ItemStack gunStack, float perLevel, float maxBonus) {
        int enhanceLevel = gunEnhanceLevel(gunStack);
        if (enhanceLevel <= 0) {
            return 0.0F;
        }
        return Math.min(maxBonus, enhanceLevel * perLevel);
    }

    private float scaledWeaponDamage(ItemStack gunStack, float baseDamage, int attackLvl, float enhancePerLevel, float maxEnhanceBonus) {
        float boostedDamage = baseDamage + attackLevelFlatBonus(attackLvl) + gunEnhanceFlatBonus(gunStack, enhancePerLevel, maxEnhanceBonus);
        return boostedDamage * com.nogeon.economyland.item.SmithingService.damageMultiplier(gunStack) * gunReforgeDamageMultiplier(gunStack);
    }

    private double supportRange(ServerPlayer owner) {
        int sensorLvl = owner.getPersistentData().getInt("nogeon_engineer_drone_upgrade_sensor_level");
        if (sensorLvl <= 0 && owner.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_sensor")) {
            sensorLvl = 1;
        }
        return sensorLvl > 0 ? 12.0D + (sensorLvl - 1) * 5.0D : 12.0D;
    }

    private double supportRangeSqr(ServerPlayer owner) {
        double range = supportRange(owner);
        return range * range;
    }

    private boolean isDroneCombatTarget(LivingEntity entity, ServerPlayer owner) {
        if (entity == null || entity == owner || entity == this || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        if (entity.isAlliedTo(owner) || owner.isAlliedTo(entity)) {
            return false;
        }
        if (entity instanceof Enemy || entity instanceof Monster) {
            return true;
        }
        return entity instanceof Mob mob && mob.getTarget() == owner;
    }

    private int computeAttackCooldown(GunType gunType, int attackLvl, ItemStack gunStack) {
        int baseCooldown = switch (gunType) {
            case SNIPER -> 60;
            case SHOTGUN -> 35;
            case SMG -> 5;
            case LMG -> 10;
            case HEAVY -> 80;
            case RIFLE -> 15;
            case DEFAULT -> 20;
        };
        double levelMultiplier = switch (normalizedAttackLevel(attackLvl)) {
            case 1 -> 1.00D;
            case 2 -> 0.86D;
            case 3 -> 0.72D;
            case 4 -> 0.58D;
            default -> 0.44D;
        };
        double enhanceMultiplier = Math.max(0.72D, 1.0D - gunEnhanceLevel(gunStack) * 0.018D);
        double rapidFireMultiplier = Math.max(0.65D, 1.0D - gunRapidFireBonus(gunStack));
        int minCooldown = gunType == GunType.SMG ? 2 : 3;
        return Math.max(minCooldown, (int) Math.round(baseCooldown * levelMultiplier * enhanceMultiplier * rapidFireMultiplier));
    }

    private int computeBurstCount(GunType gunType, int attackLvl, ItemStack gunStack) {
        int normalizedLevel = normalizedAttackLevel(attackLvl);
        int enhanceLevel = gunEnhanceLevel(gunStack);
        return switch (gunType) {
            case SNIPER -> normalizedLevel >= 5 ? 2 : 1;
            case SHOTGUN -> normalizedLevel >= 4 ? 2 : 1;
            case HEAVY -> normalizedLevel >= 4 ? (enhanceLevel >= 14 ? 3 : 2) : 1;
            case DEFAULT, RIFLE, SMG, LMG -> {
                int burst = switch (normalizedLevel) {
                    case 1 -> 1;
                    case 2 -> 2;
                    case 3 -> 3;
                    case 4 -> 4;
                    default -> 5;
                };
                if (enhanceLevel >= 10) {
                    burst++;
                }
                int cap = gunType == GunType.SMG ? 6 : 5;
                yield Math.min(cap, burst);
            }
        };
    }

    private int computeBurstDelay(ItemStack gunStack) {
        double rapidFireBonus = gunRapidFireBonus(gunStack);
        if (rapidFireBonus >= 0.20D) {
            return 1;
        }
        if (rapidFireBonus >= 0.08D) {
            return 2;
        }
        return 3;
    }

    private void consumeDroneChargeForShot(double baseAmount, ItemStack gunStack) {
        consumeCharge(baseAmount * gunChargeEfficiency(gunStack));
    }

    private void spawnAttackDischarge(ServerLevel sLevel, GunType gunType, LivingEntity target, int attackLvl, ItemStack gunStack) {
        int normalizedLevel = normalizedAttackLevel(attackLvl);
        int enhanceLevel = gunEnhanceLevel(gunStack);
        int muzzleCount = 4 + normalizedLevel * 2 + Math.min(4, enhanceLevel / 3);
        sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.45D, this.getZ(), muzzleCount, 0.18D, 0.18D, 0.18D, 0.02D);
        sLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.35D, this.getZ(), 2 + normalizedLevel, 0.12D, 0.12D, 0.12D, 0.01D);

        if (gunType == GunType.HEAVY) {
            sLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.3D, this.getZ(), 6 + normalizedLevel * 3, 0.16D, 0.16D, 0.16D, 0.03D);
        } else if (normalizedLevel >= 4) {
            sLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5D), target.getZ(), 8 + normalizedLevel * 2, 0.3D, 0.3D, 0.3D, 0.08D);
        }

        if (normalizedLevel >= 5) {
            sLevel.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY(0.5D), target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void applyOverdrivePulse(LivingEntity target, float primaryDamage, int attackLvl, ServerLevel sLevel, ServerPlayer owner) {
        int normalizedLevel = normalizedAttackLevel(attackLvl);
        if (normalizedLevel < 4 || primaryDamage <= 0.0F) {
            return;
        }

        double radius = normalizedLevel >= 5 ? 4.5D : 3.0D;
        float splashDamage = primaryDamage * (normalizedLevel >= 5 ? 0.45F : 0.25F);
        int maxTargets = normalizedLevel >= 5 ? 4 : 2;
        int hitCount = 0;

        for (LivingEntity splashTarget : sLevel.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius),
            entity -> entity != target && isDroneCombatTarget(entity, owner))) {
            splashTarget.hurt(owner.damageSources().indirectMagic(this, owner), splashDamage);
            hitCount++;
            if (hitCount >= maxTargets) {
                break;
            }
        }

        if (hitCount > 0) {
            sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5D), target.getZ(), 12 + hitCount * 4, 0.45D, 0.35D, 0.45D, 0.12D);
            sLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5D), target.getZ(), 8 + hitCount * 2, 0.35D, 0.25D, 0.35D, 0.08D);
            sLevel.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.45F + hitCount * 0.08F, 1.3F);
        }
    }

    public void triggerExpression(int expr, int ticks) {
        this.tempExpr = expr;
        this.tempExprTicks = ticks;
        setExpression(expr);
        if (expr == 3 && !level().isClientSide) { // HAPPY
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS, 0.8F, 1.8F);
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.5F);
        } else if (expr == 4 && !level().isClientSide) { // WORRIED
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ALLAY_HURT, SoundSource.PLAYERS, 0.7F, 0.6F);
        }
    }

    private boolean shouldUseNativeTaczDroneFire(ItemStack gunStack) {
        return com.tacz.guns.api.item.IGun.getIGunOrNull(gunStack) != null;
    }

    private com.tacz.guns.api.entity.IGunOperator prepareNativeTaczDroneFire(ItemStack gunStack) {
        ItemStack heldGun = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!sameDroneGunLoadout(heldGun, gunStack)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, gunStack.copy());
            heldGun = this.getItemBySlot(EquipmentSlot.MAINHAND);
        }

        com.tacz.guns.api.item.IGun heldIGun = com.tacz.guns.api.item.IGun.getIGunOrNull(heldGun);
        if (heldIGun != null) {
            heldIGun.setCurrentAmmoCount(heldGun, Math.max(heldIGun.getCurrentAmmoCount(heldGun), 30));
            heldIGun.setDummyAmmoAmount(heldGun, Math.max(heldIGun.getDummyAmmoAmount(heldGun), 30));
            heldIGun.setMaxDummyAmmoAmount(heldGun, Math.max(heldIGun.getMaxDummyAmmoAmount(heldGun), 30));
        }

        com.tacz.guns.api.entity.IGunOperator operator = com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(this);
        com.tacz.guns.entity.shooter.ShooterDataHolder data = operator.getDataHolder();
        if (data.currentGunItem == null || !sameDroneGunLoadout(data.currentGunItem.get(), heldGun)) {
            operator.draw(() -> this.getItemBySlot(EquipmentSlot.MAINHAND));
            data = operator.getDataHolder();
        }

        long readyTime = System.currentTimeMillis() - 10000L;
        data.currentGunItem = () -> this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (data.drawTimestamp < 0L || data.drawTimestamp > readyTime) {
            data.drawTimestamp = readyTime;
        }
        data.reloadTimestamp = -1L;
        data.reloadStateType = com.tacz.guns.api.entity.ReloadState.StateType.NOT_RELOADING;
        data.sprintTimestamp = -1L;
        data.sprintTimeS = 0.0F;
        return operator;
    }

    private boolean sameDroneGunLoadout(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) {
            return false;
        }
        ItemStack aCopy = a.copy();
        ItemStack bCopy = b.copy();
        stripTaczRuntimeState(aCopy);
        stripTaczRuntimeState(bCopy);
        return ItemStack.matches(aCopy, bCopy);
    }

    private void stripTaczRuntimeState(ItemStack stack) {
        if (!stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        tag.remove("GunCurrentAmmoCount");
        tag.remove("DummyAmmo");
        tag.remove("MaxDummyAmmo");
        tag.remove("HasBulletInBarrel");
        tag.remove("HeatAmount");
        tag.remove("OverHeated");
    }

    private boolean executeFire(LivingEntity target, ItemStack gunStack, GunType type, int attackLvl, ServerLevel sLevel, ServerPlayer owner) {
        if (!target.isAlive() || getCharge() <= 0) return false;

        attackLvl = normalizedAttackLevel(attackLvl);
        int enhanceLevel = gunEnhanceLevel(gunStack);

        boolean isLaser = (type == GunType.DEFAULT);
        boolean hasAmmo = true;

        // tacz 진짜 총격 API 이식
        com.tacz.guns.api.item.IGun iGun = com.tacz.guns.api.item.IGun.getIGunOrNull(gunStack);
        if (iGun != null && shouldUseNativeTaczDroneFire(gunStack)) {
            hasAmmo = isLaser || hasDroneAmmo(owner, gunStack);
            if (!hasAmmo) {
                if (this.tickCount % 10 == 0) {
                    sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.2F);
                    if (this.tickCount % 40 == 0) {
                        owner.displayClientMessage(Component.literal("§c[오토 스크랩 드론] 장착된 총기의 탄약이 부족합니다!"), true);
                    }
                }
                combatTicks = 20;
                setExpression(2);
                burstCount = 0;
                return false;
            }

            double dx = target.getX() - this.getX();
            double dy = target.getY(0.5D) - this.getY();
            double dz = target.getZ() - this.getZ();
            double xzDist = Math.sqrt(dx * dx + dz * dz);
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, xzDist));
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;

            com.tacz.guns.api.entity.IGunOperator operator = prepareNativeTaczDroneFire(gunStack);
            com.tacz.guns.api.entity.ShootResult result = operator.shoot(() -> pitch, () -> yaw);

            if (result == com.tacz.guns.api.entity.ShootResult.SUCCESS) {
                if (!isLaser) {
                    consumeDroneAmmo(owner, gunStack);
                }
                double chargeCost = 1.0D;
                switch (type) {
                    case HEAVY -> chargeCost = 4.0D;
                    case SNIPER -> chargeCost = 5.0D;
                    case SHOTGUN -> chargeCost = 3.0D;
                    case SMG -> chargeCost = 0.4D;
                    case LMG -> chargeCost = 0.8D;
                    case RIFLE -> chargeCost = 1.2D;
                    case DEFAULT -> chargeCost = 1.0D;
                }
                consumeDroneChargeForShot(chargeCost, gunStack);

                combatTicks = 30;
                setExpression(2);
                return true;
            } else if (result == com.tacz.guns.api.entity.ShootResult.NEED_BOLT) {
                operator.bolt();
                return false;
            } else if (result == com.tacz.guns.api.entity.ShootResult.NO_AMMO || result == com.tacz.guns.api.entity.ShootResult.UNKNOWN_FAIL) {
                burstCount = 0;
                return false;
            } else {
                return false;
            }
        }

        hasAmmo = isLaser || consumeDroneAmmo(owner, gunStack);
        if (!hasAmmo) {
            if (this.tickCount % 10 == 0) {
                sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.2F);
                if (this.tickCount % 40 == 0) {
                    owner.displayClientMessage(Component.literal("§c[오토 스크랩 드론] 장착된 총기의 탄약이 부족합니다!"), true);
                }
            }
            combatTicks = 20;
            setExpression(2);
            burstCount = 0;
            return false;
        }

        spawnAttackDischarge(sLevel, type, target, attackLvl, gunStack);

        float primaryDamage = 0.0F;
        boolean triggerPulse = false;

        switch (type) {
            case HEAVY -> {
                double dx = target.getX() - this.getX();
                double dy = target.getY(0.5D) - this.getY();
                double dz = target.getZ() - this.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double normX = dist != 0.0 ? dx / dist : 0;
                double normY = dist != 0.0 ? dy / dist : 0;
                double normZ = dist != 0.0 ? dz / dist : 0;
                
                double spawnX = this.getX() + normX * 1.5D;
                double spawnY = this.getY() + 0.2D + normY * 1.5D;
                double spawnZ = this.getZ() + normZ * 1.5D;
                
                int explPower = 1 + attackLvl + Math.min(3, enhanceLevel / 4) + (int) Math.floor(gunReforgeValue(gunStack, "projectile_damage") * 8.0D);
                if (!hasAmmo) {
                    explPower = Math.max(1, explPower / 2);
                }
                net.minecraft.world.entity.projectile.LargeFireball fireball = new net.minecraft.world.entity.projectile.LargeFireball(level(), this, normX * 0.1D, normY * 0.1D, normZ * 0.1D, explPower);
                fireball.setPos(spawnX, spawnY, spawnZ);
                level().addFreshEntity(fireball);

                if (attackLvl >= 5 || enhanceLevel >= 14) {
                    net.minecraft.world.entity.projectile.LargeFireball extraFireball = new net.minecraft.world.entity.projectile.LargeFireball(level(), this, normX * 0.08D, normY * 0.08D, normZ * 0.08D, Math.max(2, explPower - 1));
                    extraFireball.setPos(spawnX + normZ * 0.35D, spawnY, spawnZ - normX * 0.35D);
                    level().addFreshEntity(extraFireball);
                }
                
                sLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spawnX, spawnY, spawnZ, 6 + attackLvl + Math.min(5, enhanceLevel / 2), 0.1D, 0.1D, 0.1D, 0.02D);
                if (attackLvl >= 3) {
                    sLevel.sendParticles(ParticleTypes.CRIT, spawnX, spawnY, spawnZ, attackLvl + Math.min(4, enhanceLevel / 3), 0.2D, 0.2D, 0.2D, 0.1D);
                }
                if (attackLvl >= 5) {
                    sLevel.sendParticles(ParticleTypes.DRAGON_BREATH, spawnX, spawnY, spawnZ, 8, 0.2D, 0.2D, 0.2D, 0.05D);
                }
                
                float volume = 1.2F + attackLvl * 0.15F;
                float pitch = 1.0F - attackLvl * 0.05F;
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, volume, pitch);
                consumeDroneChargeForShot(4.0D, gunStack);
            }
            case SNIPER -> {
                float damage = scaledWeaponDamage(gunStack, 45.0F + attackLvl * 10.0F, attackLvl, 2.0F, 30.0F);
                if (!hasAmmo) damage *= 0.5F;
                target.hurt(owner.damageSources().indirectMagic(this, owner), damage);
                primaryDamage = damage;
                triggerPulse = true;
                
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.5F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.8F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 1.5F);
                
                double startX = this.getX();
                double startY = this.getY();
                double startZ = this.getZ();
                double endX = target.getX();
                double endY = target.getY() + target.getEyeHeight() / 2.0D;
                double endZ = target.getZ();
                
                int steps = 24 + attackLvl * 8;
                for (int step = 0; step <= steps; step++) {
                    double ratio = step / (double) steps;
                    double lx = startX + (endX - startX) * ratio;
                    double ly = startY + (endY - startY) * ratio;
                    double lz = startZ + (endZ - startZ) * ratio;
                    sLevel.sendParticles(ParticleTypes.SONIC_BOOM, lx, ly, lz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    sLevel.sendParticles(ParticleTypes.END_ROD, lx, ly, lz, 2, 0.05D, 0.05D, 0.05D, 0.01D);
                }
                sLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, endX, endY, endZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                consumeDroneChargeForShot(5.0D, gunStack);
            }
            case SHOTGUN -> {
                int pellets = 5;
                float damagePerPellet = scaledWeaponDamage(gunStack, 8.0F + attackLvl * 2.0F, attackLvl, 0.9F, 16.0F);
                if (!hasAmmo) damagePerPellet *= 0.5F;
                target.hurt(owner.damageSources().indirectMagic(this, owner), damagePerPellet * 2.0F);
                primaryDamage = damagePerPellet * 2.0F;
                triggerPulse = true;
                
                AABB splashBox = target.getBoundingBox().inflate(3.0D);
                List<LivingEntity> splashTargets = sLevel.getEntitiesOfClass(LivingEntity.class, splashBox,
                    entity -> entity != target && isDroneCombatTarget(entity, owner)
                );
                for (LivingEntity st : splashTargets) {
                    st.hurt(owner.damageSources().indirectMagic(this, owner), damagePerPellet);
                }
                
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 1.2F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.0F, 0.8F);
                
                double startX = this.getX();
                double startY = this.getY();
                double startZ = this.getZ();
                double endX = target.getX();
                double endY = target.getY() + target.getEyeHeight() / 2.0D;
                double endZ = target.getZ();
                
                for (int p = 0; p < pellets; p++) {
                    double ox = (random.nextDouble() - 0.5D) * 1.5D;
                    double oy = (random.nextDouble() - 0.5D) * 1.5D;
                    double oz = (random.nextDouble() - 0.5D) * 1.5D;
                    int steps = 12;
                    for (int step = 0; step <= steps; step++) {
                        double ratio = step / (double) steps;
                        double lx = startX + (endX + ox - startX) * ratio;
                        double ly = startY + (endY + oy - startY) * ratio;
                        double lz = startZ + (endZ + oz - startZ) * ratio;
                        sLevel.sendParticles(ParticleTypes.SMOKE, lx, ly, lz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        sLevel.sendParticles(ParticleTypes.FLAME, lx, ly, lz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
                consumeDroneChargeForShot(3.0D, gunStack);
            }
            case SMG -> {
                float damage = scaledWeaponDamage(gunStack, 5.0F + attackLvl * 1.0F, attackLvl, 0.7F, 12.0F);
                if (!hasAmmo) damage *= 0.5F;
                target.hurt(owner.damageSources().indirectMagic(this, owner), damage);
                primaryDamage = damage;
                triggerPulse = true;
                
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS, 0.6F, 1.8F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 1.5F);
                
                double startX = this.getX();
                double startY = this.getY();
                double startZ = this.getZ();
                double endX = target.getX();
                double endY = target.getY() + target.getEyeHeight() / 2.0D;
                double endZ = target.getZ();
                
                int steps = 10;
                for (int step = 0; step <= steps; step++) {
                    double ratio = step / (double) steps;
                    double lx = startX + (endX - startX) * ratio;
                    double ly = startY + (endY - startY) * ratio;
                    double lz = startZ + (endZ - startZ) * ratio;
                    sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, lx, ly, lz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                consumeDroneChargeForShot(0.4D, gunStack);
            }
            case LMG -> {
                float damage = scaledWeaponDamage(gunStack, 10.0F + attackLvl * 2.0F, attackLvl, 1.1F, 20.0F);
                if (!hasAmmo) damage *= 0.5F;
                target.hurt(owner.damageSources().indirectMagic(this, owner), damage);
                primaryDamage = damage;
                triggerPulse = true;
                
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 1.6F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.7F, 1.8F);
                
                double startX = this.getX();
                double startY = this.getY();
                double startZ = this.getZ();
                double endX = target.getX();
                double endY = target.getY() + target.getEyeHeight() / 2.0D;
                double endZ = target.getZ();
                
                int steps = 12;
                for (int step = 0; step <= steps; step++) {
                    double ratio = step / (double) steps;
                    double lx = startX + (endX - startX) * ratio;
                    double ly = startY + (endY - startY) * ratio;
                    double lz = startZ + (endZ - startZ) * ratio;
                    sLevel.sendParticles(ParticleTypes.CRIT, lx, ly, lz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    sLevel.sendParticles(ParticleTypes.SMOKE, lx, ly, lz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                consumeDroneChargeForShot(0.8D, gunStack);
            }
            case RIFLE -> {
                float damage = scaledWeaponDamage(gunStack, 15.0F + attackLvl * 3.0F, attackLvl, 1.5F, 24.0F);
                if (!hasAmmo) damage *= 0.5F;
                target.hurt(owner.damageSources().indirectMagic(this, owner), damage);
                primaryDamage = damage;
                triggerPulse = true;
                
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.5F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.8F, 1.8F);
                
                double startX = this.getX();
                double startY = this.getY();
                double startZ = this.getZ();
                double endX = target.getX();
                double endY = target.getY() + target.getEyeHeight() / 2.0D;
                double endZ = target.getZ();
                
                int steps = 15;
                for (int step = 0; step <= steps; step++) {
                    double ratio = step / (double) steps;
                    double lx = startX + (endX - startX) * ratio;
                    double ly = startY + (endY - startY) * ratio;
                    double lz = startZ + (endZ - startZ) * ratio;
                    sLevel.sendParticles(ParticleTypes.CRIT, lx, ly, lz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, lx, ly, lz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                consumeDroneChargeForShot(1.2D, gunStack);
            }
            case DEFAULT -> {
                float damage = (6.0F + attackLvl * 2.0F + attackLevelFlatBonus(attackLvl)) * gunReforgeDamageMultiplier(gunStack);
                target.hurt(owner.damageSources().indirectMagic(this, owner), damage);
                primaryDamage = damage;
                triggerPulse = true;
                
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 2.0F);
                
                double startX = this.getX();
                double startY = this.getY();
                double startZ = this.getZ();
                double endX = target.getX();
                double endY = target.getY() + target.getEyeHeight() / 2.0D;
                double endZ = target.getZ();
                
                int steps = 12;
                for (int step = 0; step <= steps; step++) {
                    double ratio = step / (double) steps;
                    double lx = startX + (endX - startX) * ratio;
                    double ly = startY + (endY - startY) * ratio;
                    double lz = startZ + (endZ - startZ) * ratio;
                    sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, lx, ly, lz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                consumeDroneChargeForShot(1.0D, gunStack);
            }
        }

        if (triggerPulse) {
            applyOverdrivePulse(target, primaryDamage, attackLvl, sLevel, owner);
        }
        
        float skullChance = 0.10F + attackLvl * 0.05F + Math.min(0.10F, enhanceLevel * 0.005F);
        if (attackLvl >= 5 && random.nextFloat() < skullChance && type != GunType.HEAVY) {
            double dx = target.getX() - this.getX();
            double dy = target.getY(0.5D) - this.getY();
            double dz = target.getZ() - this.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist != 0.0) {
                double normX = dx / dist;
                double normY = dy / dist;
                double normZ = dz / dist;
                double spawnX = this.getX() + normX * 1.5D;
                double spawnY = this.getY() + 0.2D + normY * 1.5D;
                double spawnZ = this.getZ() + normZ * 1.5D;
                net.minecraft.world.entity.projectile.WitherSkull skull = new net.minecraft.world.entity.projectile.WitherSkull(level(), this, normX * 0.1D, normY * 0.1D, normZ * 0.1D);
                skull.setPos(spawnX, spawnY, spawnZ);
                level().addFreshEntity(skull);
                sLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
        }
        
        combatTicks = 30;
        setExpression(2);
        return true;
    }

    private boolean consumeDroneAmmo(ServerPlayer owner, ItemStack gunStack) {
        if (!owner.getPersistentData().contains("nogeon_engineer_drone_ammo")) {
            return false;
        }

        ItemStack ammoStack = ItemStack.of(owner.getPersistentData().getCompound("nogeon_engineer_drone_ammo"));
        if (ammoStack.isEmpty()) {
            owner.getPersistentData().remove("nogeon_engineer_drone_ammo");
            return false;
        }

        IAmmo ammo = IAmmo.getIAmmoOrNull(ammoStack);
        if (ammo != null && ammo.isAmmoOfGun(ammoStack, gunStack)) {
            if (!shouldConserveDroneAmmo(owner, gunStack)) {
                ammoStack.shrink(1);
            }
            saveDroneAmmo(owner, ammoStack);
            return true;
        }

        if (ammoStack.getItem() instanceof IAmmoBox ammoBox && ammoBox.isAmmoBoxOfGun(ammoStack, gunStack)) {
            if (ammoBox.isCreative(ammoStack) || ammoBox.isAllTypeCreative(ammoStack)) {
                saveDroneAmmo(owner, ammoStack);
                return true;
            }

            int count = ammoBox.getAmmoCount(ammoStack);
            if (count <= 0) {
                return false;
            }
            if (!shouldConserveDroneAmmo(owner, gunStack)) {
                ammoBox.setAmmoCount(ammoStack, count - 1);
            }
            saveDroneAmmo(owner, ammoStack);
            return true;
        }

        return false;
    }

    private void saveDroneAmmo(ServerPlayer owner, ItemStack ammoStack) {
        if (ammoStack.isEmpty()) {
            owner.getPersistentData().remove("nogeon_engineer_drone_ammo");
            owner.displayClientMessage(Component.literal("§c[오토 스크랩 드론] 장착된 탄약이 모두 소진되었습니다."), true);
        } else {
            owner.getPersistentData().put("nogeon_engineer_drone_ammo", ammoStack.save(new CompoundTag()));
        }
    }

    private boolean hasDroneAmmo(ServerPlayer owner, ItemStack gunStack) {
        if (!owner.getPersistentData().contains("nogeon_engineer_drone_ammo")) {
            return false;
        }

        ItemStack ammoStack = ItemStack.of(owner.getPersistentData().getCompound("nogeon_engineer_drone_ammo"));
        if (ammoStack.isEmpty()) {
            return false;
        }

        IAmmo ammo = IAmmo.getIAmmoOrNull(ammoStack);
        if (ammo != null && ammo.isAmmoOfGun(ammoStack, gunStack)) {
            return true;
        }

        if (ammoStack.getItem() instanceof IAmmoBox ammoBox && ammoBox.isAmmoBoxOfGun(ammoStack, gunStack)) {
            if (ammoBox.isCreative(ammoStack) || ammoBox.isAllTypeCreative(ammoStack)) {
                return true;
            }
            int count = ammoBox.getAmmoCount(ammoStack);
            if (count > 0) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldConserveDroneAmmo(ServerPlayer owner, ItemStack gunStack) {
        EconomyState state = EconomyState.get(owner.server);
        PlayerProfile profile = state.profile(owner.getUUID());
        double saveChance = 0.0D;
        if (profile.selectedJob() == JobType.ENGINEER) {
            int processOptLevel = profile.job(JobType.ENGINEER).nodeLevel(SkillNode.ENGINEER_PROCESS_OPTIMIZATION);
            saveChance += processOptLevel * 3.0D;
        }
        if (owner.getPersistentData().contains("nogeon_engineer_kinetic_boost_ticks")) {
            saveChance += 30.0D;
        }
        saveChance += gunReloadMasteryBonus(gunStack) * 100.0D;
        if (saveChance <= 0.0D) {
            return false;
        }
        boolean conserved = owner.getRandom().nextDouble() * 100.0D < saveChance;
        if (conserved) {
            owner.displayClientMessage(Component.literal("§a[오토 스크랩 드론] 탄약 절약!"), true);
        }
        return conserved;
    }

    private void fireDefaultLaser(ServerPlayer owner, LivingEntity closest, ServerLevel sLevel) {
        executeFire(closest, ItemStack.EMPTY, GunType.DEFAULT, owner.getPersistentData().getInt("nogeon_engineer_drone_stat_attack"), sLevel, owner);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        Optional<UUID> ownerUuidOpt = getOwnerUuid();
        if (ownerUuidOpt.isPresent() && ownerUuidOpt.get().equals(player.getUUID())) {
            ServerPlayer sPlayer = (ServerPlayer) player;
            ItemStack held = sPlayer.getItemInHand(hand);
            
            // 1. 렌치 및 톱니바퀴 충전 검사 (아이템 소지 시 최우선)
            if (!held.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                if (itemId.contains("wrench")) {
                    setCharge(getCharge() + 20);
                    triggerExpression(3, 40); // Joyful expression + sound
                    sPlayer.displayClientMessage(Component.literal("§6[오토 스크랩 드론] §f렌치로 태엽을 감아 동력을 충전했습니다. (현재 동력: §e" + getCharge() + "%§f)"), true);
                    return InteractionResult.CONSUME;
                } else if (itemId.equals("create:cogwheel")) {
                    held.shrink(1);
                    setCharge(getCharge() + 50);
                    triggerExpression(3, 40); // Joyful expression + sound
                    sPlayer.displayClientMessage(Component.literal("§6[오토 스크랩 드론] §f톱니바퀴를 급탄하여 동력을 충전했습니다. (현재 동력: §e" + getCharge() + "%§f)"), true);
                    return InteractionResult.CONSUME;
                }
            }
            
            // 2. 일반 우클릭 시 -> 탑승 비행
            if (!sPlayer.isShiftKeyDown()) {
                if (getCharge() > 0) {
                    if (!sPlayer.startRiding(this, true)) {
                        sPlayer.displayClientMessage(Component.literal("§c[오토 스크랩 드론] 드론 탑승에 실패했습니다."), true);
                        return InteractionResult.CONSUME;
                    }
                    sPlayer.level().playSound(null, sPlayer.getX(), sPlayer.getY(), sPlayer.getZ(),
                        SoundEvents.HORSE_ARMOR, SoundSource.PLAYERS, 0.8F, 1.5F);
                    sPlayer.displayClientMessage(Component.literal("§a[오토 스크랩 드론] 드론에 매달렸습니다! (W/S/A/D: 비행, Space: 상승, Shift: 하마)"), true);
                    consumeCharge(1.0D);
                } else {
                    sPlayer.displayClientMessage(Component.literal("§c[오토 스크랩 드론] 동력이 방전되어 드론에 탑승할 수 없습니다."), true);
                }
                return InteractionResult.CONSUME;
            }
            
            // 3. Shift 우클릭 시 -> 제어 GUI 개방
            sPlayer.level().playSound(null, sPlayer.getX(), sPlayer.getY(), sPlayer.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.4F, 1.8F);
            sPlayer.displayClientMessage(Component.literal("§6[오토 스크랩 드론] §f드론의 내부 분해 제어장치를 실행합니다."), true);
            DeconstructOpener.open(sPlayer, -1, null);
            return InteractionResult.CONSUME;
        }
        
        return super.mobInteract(player, hand);
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            moveFunction.accept(passenger, this.getX(), this.getY() - 1.35D, this.getZ());
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty()
            && passenger instanceof Player player
            && getOwnerUuid().isPresent()
            && getOwnerUuid().get().equals(player.getUUID());
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof LivingEntity ? (LivingEntity) passenger : null;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("SkillLevel", skillLevel());
        nbt.putDouble("PreciseCharge", preciseCharge);
        getOwnerUuid().ifPresent(owner -> nbt.putUUID("Owner", owner));
        nbt.putBoolean("UpgInv", hasUpgradeInventory());
        nbt.putBoolean("UpgTrans", hasUpgradeTransmitter());
        nbt.putBoolean("UpgBoost", hasUpgradeBooster());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 냉기, 호흡(질식/익사), 열(온도), 낙사, 마법 등 모든 비엔티티 환경 피해 면제
        if (source.getEntity() == null && source.getDirectEntity() == null) {
            return false;
        }
        if (source.is(net.minecraft.world.damagesource.DamageTypes.DROWN) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.FREEZE) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.STARVE) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.FALL) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (super.isInvulnerableTo(source)) {
            return true;
        }
        if (source.getEntity() == null && source.getDirectEntity() == null) {
            return true;
        }
        if (source.is(net.minecraft.world.damagesource.DamageTypes.DROWN) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.FREEZE) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.STARVE) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.FALL) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) {
            return true;
        }
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        int skillLevel = nbt.contains("SkillLevel") ? nbt.getInt("SkillLevel") : 1;
        entityData.set(SKILL_LEVEL, Math.max(1, skillLevel));
        preciseCharge = nbt.contains("PreciseCharge") ? nbt.getDouble("PreciseCharge") : 100.0;
        setCharge((int)preciseCharge);
        if (nbt.hasUUID("Owner")) {
            entityData.set(OWNER, Optional.of(nbt.getUUID("Owner")));
        }
        entityData.set(UPG_INV, nbt.contains("UpgInv") && nbt.getBoolean("UpgInv"));
        entityData.set(UPG_TRANS, nbt.contains("UpgTrans") && nbt.getBoolean("UpgTrans"));
        entityData.set(UPG_BOOST, nbt.contains("UpgBoost") && nbt.getBoolean("UpgBoost"));
        updateCustomName();
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        
        if (!level().isClientSide) {
            Optional<UUID> ownerUuidOpt = getOwnerUuid();
            if (ownerUuidOpt.isPresent()) {
                Player ownerPlayer = level().getPlayerByUUID(ownerUuidOpt.get());
                if (ownerPlayer != null) {
                    String[] goodbyeMsgs = {
                        "§c[" + getDroneName() + "] §7안녕... 주인님... 더는 움직일 수 없어요...",
                        "§c[" + getDroneName() + "] §7미안해요... 끝까지 지켜드리고 싶었는데...",
                        "§c[" + getDroneName() + "] §7시스템 손상 임계치 도달... 잘 있어...요...",
                        "§c[" + getDroneName() + "] §7동력이... 다해...갑니다... 부디... 살아남으...세요..."
                    };
                    String msg = goodbyeMsgs[random.nextInt(goodbyeMsgs.length)];
                    ownerPlayer.sendSystemMessage(Component.literal(msg));
                }
            }
            
            // Play sad whimpering sounds
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ALLAY_DEATH, SoundSource.NEUTRAL, 1.0F, 0.5F);
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.IRON_GOLEM_DEATH, SoundSource.NEUTRAL, 1.0F, 1.6F);
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        
        // Both client and server: shiver and fall down
        if (this.deathTime < 35) {
            double shiverX = (random.nextDouble() - 0.5D) * 0.08D;
            double shiverZ = (random.nextDouble() - 0.5D) * 0.08D;
            double fallY = -0.1D;
            this.setDeltaMovement(shiverX, this.getDeltaMovement().y + fallY, shiverZ);
            if (this.getDeltaMovement().y < -0.5D) {
                this.setDeltaMovement(shiverX, -0.5D, shiverZ);
            }
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        } else {
            // Linger/stay on the ground/fall slowly
            this.setDeltaMovement(0.0D, -0.1D, 0.0D);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        }

        if (level().isClientSide) {
            // Client-side visual shiver and smoke/sparks
            if (this.deathTime < 35) {
                double dx = (random.nextDouble() - 0.5D) * 0.15D;
                double dy = (random.nextDouble() - 0.5D) * 0.15D;
                double dz = (random.nextDouble() - 0.5D) * 0.15D;
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + dx, getY() + dy, getZ() + dz, 0.0D, 0.0D, 0.0D);
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + dx, getY() + dy, getZ() + dz, 0.0D, 0.0D, 0.0D);
            }
            if (this.deathTime == 35) {
                for (int i = 0; i < 20; i++) {
                    double px = getX() + (random.nextDouble() - 0.5D) * 1.0D;
                    double py = getY() + (random.nextDouble() - 0.5D) * 1.0D;
                    double pz = getZ() + (random.nextDouble() - 0.5D) * 1.0D;
                    level().addParticle(ParticleTypes.EXPLOSION, px, py, pz, 0.0D, 0.0D, 0.0D);
                }
            }
            return;
        }

        // Server-side
        if (this.deathTime < 35) {
            if (this.deathTime % 6 == 0) {
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ALLAY_HURT, SoundSource.NEUTRAL, 0.8F, 0.5F + random.nextFloat() * 0.2F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BAT_DEATH, SoundSource.NEUTRAL, 0.7F, 0.4F + random.nextFloat() * 0.2F);
            }
        }

        if (this.deathTime == 35) {
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 1.2F, 1.4F);
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 0.8F);
        }

        if (this.deathTime >= 65) { // linger for 65 ticks (3.25s) to stay black on ground
            this.discard();
        }
    }
}
