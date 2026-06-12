package com.nogeon.economyland.entity;

import com.nogeon.economyland.land.LandFlag;
import com.nogeon.economyland.land.LandRegion;
import com.nogeon.economyland.menu.AdminShopOpener;
import com.nogeon.economyland.menu.ShopOpener;
import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EconomyTraderEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> SPEECH_KEY = SynchedEntityData.defineId(EconomyTraderEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SPEECH_TICKS = SynchedEntityData.defineId(EconomyTraderEntity.class, EntityDataSerializers.INT);
    private static final int GREET_SPEECH_TICKS = 90;
    private static final int IDLE_SPEECH_TICKS = 80;
    private static final int IDLE_SPEECH_BASE_COOLDOWN = 140;
    private static final int IDLE_SPEECH_RANDOM_COOLDOWN = 80;
    private static final double PLAYER_LOOK_RANGE = 8.0D;

    private TraderKind traderKind = TraderKind.GENERAL;
    private String traderDatabaseId = UUID.randomUUID().toString();
    private int idleSpeechCooldown;

    public EconomyTraderEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SPEECH_KEY, "");
        entityData.define(SPEECH_TICKS, 0);
    }

    public TraderKind traderKind() {
        return traderKind;
    }

    public Component speech() {
        String key = entityData.get(SPEECH_KEY);
        if (key.isBlank() || entityData.get(SPEECH_TICKS) <= 0) {
            return null;
        }
        return Component.translatable(key);
    }

    public String traderDatabaseId() {
        return traderDatabaseId;
    }

    public void setTraderKind(TraderKind traderKind) {
        this.traderKind = traderKind;
        setCustomName(Component.translatable(traderKind.translationKey()));
        setCustomNameVisible(true);
    }

    public void setTraderDatabaseId(String traderDatabaseId) {
        if (traderDatabaseId == null || traderDatabaseId.isBlank()) {
            this.traderDatabaseId = UUID.randomUUID().toString();
            return;
        }
        this.traderDatabaseId = traderDatabaseId;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (super.isInvulnerableTo(source)) {
            return true;
        }
        return isInvincible();
    }

    private boolean isInvincible() {
        if (level().isClientSide) {
            return false;
        }
        LandRegion land = EconomyState.get(level().getServer()).landAt(level().dimension(), blockPosition());
        return land != null && land.flag(LandFlag.NPC_INVINCIBLE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("TraderKind", traderKind.id());
        nbt.putString("TraderDatabaseId", traderDatabaseId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        setTraderKind(TraderKind.byId(nbt.getString("TraderKind")));
        setTraderDatabaseId(nbt.getString("TraderDatabaseId"));
    }

    @Override
    public void tick() {
        super.tick();
        lookAtNearbyPlayer();
        if (level().isClientSide) {
            return;
        }
        int speechTicks = entityData.get(SPEECH_TICKS);
        if (speechTicks > 0) {
            speechTicks--;
            entityData.set(SPEECH_TICKS, speechTicks);
            if (speechTicks == 0) {
                entityData.set(SPEECH_KEY, "");
            }
        }
        if (idleSpeechCooldown > 0) {
            idleSpeechCooldown--;
            return;
        }
        if (entityData.get(SPEECH_TICKS) > 0 || tickCount % 20 != 0) {
            return;
        }
        Player nearby = level().getNearestPlayer(this, PLAYER_LOOK_RANGE);
        if (nearby != null && !nearby.isSpectator()) {
            setSpeech("idle", IDLE_SPEECH_TICKS);
        }
    }

    private void lookAtNearbyPlayer() {
        Player nearby = level().getNearestPlayer(this, PLAYER_LOOK_RANGE);
        if (nearby == null || nearby.isSpectator()) {
            return;
        }
        double dx = nearby.getX() - getX();
        double dz = nearby.getZ() - getZ();
        if (dx * dx + dz * dz < 1.0E-4D) {
            return;
        }
        float yaw = (float)(Mth.atan2(dz, dx) * (180.0F / (float)Math.PI)) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (!serverPlayer.hasPermissions(2)) {
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("관리자 편집은 OP 권한이 필요합니다."), false);
                return InteractionResult.CONSUME;
            }
            AdminShopOpener.open(serverPlayer, traderKind, traderDatabaseId);
            return InteractionResult.CONSUME;
        }

        setSpeech("greet", GREET_SPEECH_TICKS);
        ShopOpener.open(serverPlayer, traderKind, traderDatabaseId);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (level().isClientSide) {
            return;
        }
        ItemStack stack = preservedSpawner();
        if (!stack.isEmpty()) {
            spawnAtLocation(stack);
        }
    }

    private void setSpeech(String suffix, int ticks) {
        String resolvedSuffix = suffix;
        if (traderKind.supportsInventoryShop() && level().getServer() != null) {
            long day = level().getServer().overworld().getDayTime() / 24000L;
            TraderShopState shopState = TraderShopState.get(level().getServer());
            shopState.refreshShopDay(EconomyState.get(level().getServer()), day);
            resolvedSuffix = shopState.demandSpeechSuffix(traderKind, traderDatabaseId, day);
            if ("idle".equals(resolvedSuffix) && "greet".equals(suffix)) {
                resolvedSuffix = "greet";
            }
        }
        entityData.set(SPEECH_KEY, "speech.nogeon_economy_land." + traderKind.id() + "." + resolvedSuffix);
        entityData.set(SPEECH_TICKS, ticks);
        idleSpeechCooldown = IDLE_SPEECH_BASE_COOLDOWN + random.nextInt(IDLE_SPEECH_RANDOM_COOLDOWN + 1);
    }

    private ItemStack preservedSpawner() {
        ItemStack stack = switch (traderKind) {
            case GENERAL -> new ItemStack(ModItems.GENERAL_TRADER_SPAWNER.get());
            case CROP -> new ItemStack(ModItems.CROP_TRADER_SPAWNER.get());
            case FISHER -> new ItemStack(ModItems.FISHER_TRADER_SPAWNER.get());
            case MINER -> new ItemStack(ModItems.MINER_TRADER_SPAWNER.get());
            case CHEF -> new ItemStack(ModItems.CHEF_TRADER_SPAWNER.get());
            case LOTTERY -> new ItemStack(ModItems.LOTTERY_TRADER_SPAWNER.get());
            case GAMBLER -> new ItemStack(ModItems.GAMBLER_TRADER_SPAWNER.get());
            case GACHA -> new ItemStack(ModItems.GACHA_TRADER_SPAWNER.get());
            case POTION -> new ItemStack(ModItems.POTION_TRADER_SPAWNER.get());
            case SMITH -> new ItemStack(ModItems.SMITH_TRADER_SPAWNER.get());
            case ENGINEER -> new ItemStack(ModItems.ENGINEER_TRADER_SPAWNER.get());
            case GUN -> new ItemStack(ModItems.GUN_TRADER_SPAWNER.get());
            case LAND -> new ItemStack(ModItems.LAND_TRADER_SPAWNER.get());
            case AUCTION -> new ItemStack(ModItems.AUCTION_TRADER_SPAWNER.get());
            case HUNTER -> new ItemStack(ModItems.HUNTER_TRADER_SPAWNER.get());
        };
        stack.getOrCreateTag().putString("TraderDatabaseId", traderDatabaseId);
        return stack;
    }
}
