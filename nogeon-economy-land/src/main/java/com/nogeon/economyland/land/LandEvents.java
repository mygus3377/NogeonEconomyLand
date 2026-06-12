package com.nogeon.economyland.land;

import com.nogeon.economyland.item.LandDeedItem;
import com.nogeon.economyland.entity.EconomyTraderEntity;
import com.nogeon.economyland.menu.LandClaimOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.network.SyncLandSelectionPacket;
import com.nogeon.economyland.state.EconomyState;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class LandEvents {
    private static final Map<UUID, Integer> LAST_LAND_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, ViolationState> LAND_VIOLATIONS = new HashMap<>();
    private static final long VIOLATION_WINDOW_MS = 10_000L;
    private static final int VIOLATION_BAN_THRESHOLD = 4;

    private LandEvents() {
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (shouldBlockProtectedDamage(event.getEntity(), event.getSource())) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            return;
        }
    }

    public static void onLivingDamage(LivingDamageEvent event) {
        if (shouldBlockProtectedDamage(event.getEntity(), event.getSource())) {
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }

    public static void onLivingAttack(LivingAttackEvent event) {
        if (shouldBlockProtectedDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
            || !(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)
            || mob instanceof EconomyTraderEntity
            || mob.tickCount % 20 != 0) {
            return;
        }
        LandRegion land = EconomyState.get(level.getServer()).landAt(level.dimension(), mob.blockPosition());
        if (land != null && land.type() == LandType.ADMIN) {
            mob.discard();
        }
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget().level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity().hasPermissions(2)) {
            return;
        }
        LandRegion land = EconomyState.get(level.getServer()).landAt(level.dimension(), event.getTarget().blockPosition());
        if (land != null && land.type() == LandType.ADMIN) {
            event.setCanceled(true);
        }
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        // 드론이 발사한 투사체의 폭발은 지형을 파괴하지 않도록 차단
        net.minecraft.world.entity.Entity exploder = event.getExplosion().getExploder();
        if (exploder instanceof Projectile projectile) {
            if (projectile.getOwner() instanceof com.nogeon.economyland.entity.ScrapDroneEntity) {
                event.getAffectedBlocks().clear();
            }
        }

        EconomyState state = EconomyState.get(level.getServer());
        event.getAffectedBlocks().removeIf(pos -> {
            LandRegion land = state.landAt(level.dimension(), pos);
            return land != null && land.type() == LandType.ADMIN;
        });
        event.getAffectedEntities().removeIf(entity -> {
            LandRegion land = state.landAt(level.dimension(), entity.blockPosition());
            return land != null && land.type() == LandType.ADMIN;
        });
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermissions(2)) {
            return;
        }
        LandRegion land = EconomyState.get(level.getServer()).landAt(level.dimension(), event.getPos());
        if (land != null && land.type().protectedLand() && !land.canBuild(player.getUUID())) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.protected"), true);
            recordProtectedViolation(player, "protected block break");
        }
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (isFireBlock(event.getPlacedBlock())) {
            LandRegion fireLand = protectedLandAt(level, event.getPos());
            if (fireLand != null) {
                boolean allowed = false;
                if (event.getEntity() instanceof Player player) {
                    if (player.hasPermissions(2) || fireLand.canBuild(player.getUUID())) {
                        allowed = true;
                    }
                }
                if (!allowed) {
                    event.setCanceled(true);
                    if (event.getEntity() instanceof Player player && !player.hasPermissions(2)) {
                        recordProtectedViolation(player, "protected fire placement");
                    }
                    return;
                }
            }
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.hasPermissions(2)) {
            return;
        }
        LandRegion land = EconomyState.get(level.getServer()).landAt(level.dimension(), event.getPos());
        
        if (land != null && land.type().protectedLand() && !land.canBuild(player.getUUID())) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.protected"), true);
            recordProtectedViolation(player, "protected block place");
        }
    }

    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = new BlockPos((int) event.getX(), (int) event.getY(), (int) event.getZ());
        LandRegion land = EconomyState.get(level.getServer()).landAt(level.dimension(), pos);
        if (land != null && land.flag(LandFlag.DENY_MONSTER_SPAWN)) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }

    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LandRegion land = EconomyState.get(level.getServer()).landAt(level.dimension(), event.getPos());
        if (land != null && land.flag(LandFlag.DENY_TRAMPLE)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Player player && !player.hasPermissions(2)) {
                recordProtectedViolation(player, "protected farmland trample");
            }
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getEntity();
        if (player.hasPermissions(2)) {
            return;
        }
        if (heldLandType(player) != null) {
            return;
        }
        LandRegion land = EconomyState.get(level.getServer()).landAt(level.dimension(), event.getPos());
        if (land != null && land.type() != LandType.ADMIN && land.type().protectedLand()) {
            if (!land.canInteract(player.getUUID())) {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.protected_interaction"), true);
                recordProtectedViolation(player, "protected interaction");
            } else {
                // 상호작용 권한이 정상적으로 있는 유저의 경우, 타 모드(FTB Chunks 등)의 이벤트 취소 간섭을 우회하도록 강제 ALLOW 설정.
                // 단, 플레이어가 웅크린(Shift) 상태로 아이템을 들고 우클릭한 경우는 블록 설치 등을 우선해야 하므로 ALLOW를 우회하지 않습니다.
                if (!(player.isSecondaryUseActive() && !player.getItemInHand(event.getHand()).isEmpty())) {
                    event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
                } else {
                    event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
                    event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
                }
            }
        }
    }

    public static void handleDeedClick(ServerPlayer player, boolean rightClick, BlockPos pos) {
        LandType heldType = heldLandType(player);
        if (heldType == null) {
            return;
        }

        LandSelection selection = LandSelectionManager.get(player);
        LandSelectionManager.Mode mode = LandSelectionManager.getMode(player);

        if (mode == LandSelectionManager.Mode.DESIGNATION) {
            if (pos == null) return;

            LandSelectionManager.SelectionStepResult result = LandSelectionManager.handleInteraction(player, pos, rightClick);
            if (result.success()) {
                SyncLandSelectionPacket.send(player, result.selection() != null ? result.selection() : selection, result.pendingFirst(), LandSelectionManager.isPendingAdditive(player));
                if (result.cuboidCompleted()) {
                    player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.cuboid_added"), true);
                } else {
                    player.displayClientMessage(Component.translatable(rightClick ? "message.nogeon_economy_land.land.first_point" : "message.nogeon_economy_land.land.first_point_subtraction"), true);
                }
            }
            return;
        }

        // DECISION mode
        if (pos == null) {
            // Air click in DECISION mode: open the appropriate GUI
            if (selection == null || selection.cuboids().isEmpty()) {
                LandClaimOpener.openSelectionPrompt(player);
            } else {
                LandClaimOpener.open(player);
            }
            return;
        }

        // Block click in DECISION mode: start a new designation
        LandSelectionManager.enterDesignationMode(player);
        LandSelectionManager.SelectionStepResult result = LandSelectionManager.handleInteraction(player, pos, rightClick);
        if (result.success()) {
            SyncLandSelectionPacket.send(player, result.selection(), result.pendingFirst(), LandSelectionManager.isPendingAdditive(player));
            player.displayClientMessage(Component.translatable(rightClick ? "message.nogeon_economy_land.land.first_point" : "message.nogeon_economy_land.land.first_point_subtraction"), true);
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        announceLandEntry(player);
        if (player.tickCount % 20 == 0) {
            clearProtectedFireNear(player);
            healInSpawnLand(player);
        }

        LandType heldType = heldLandType(player);
        LandType activeType = LandSelectionManager.currentType(player);
        
        if (heldType != null) {
            if (activeType == null || activeType != heldType) {
                LandSelection selection = LandSelectionManager.start(player, heldType);
                SyncLandSelectionPacket.send(player, selection, null, false);
            }
            return;
        }
        
        if (activeType != null) {
            LandSelectionManager.clear(player);
            SyncLandSelectionPacket.clear(player);
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
        }
    }

    private static void announceLandEntry(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        LandRegion land = state.landColumnAt(player.level().dimension(), player.blockPosition());
        int currentId = land == null ? 0 : land.id();
        Integer previousId = LAST_LAND_BY_PLAYER.put(player.getUUID(), currentId);
        if (currentId == 0 || (previousId != null && previousId == currentId)) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 34, 12));
        if (land.type() == LandType.ADMIN) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.nogeon_economy_land.land.enter_spawn")));
        } else {
            Component ownerName = Component.literal(state.knownPlayerName(land.owner()));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.nogeon_economy_land.land.enter_title", ownerName)));
        }
    }

    private static LandType heldLandType(Player player) {
        if (player.getMainHandItem().getItem() instanceof LandDeedItem deed) {
            return deed.landType();
        }
        return null;
    }

    private static void clearProtectedFireNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 4, 8))) {
            if (isFireBlock(level.getBlockState(pos)) && adminLandAt(level, pos) != null) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static void healInSpawnLand(ServerPlayer player) {
        LandRegion land = EconomyState.get(player.server).landColumnAt(player.level().dimension(), player.blockPosition());
        if (land == null || land.type() != LandType.ADMIN || player.isDeadOrDying()) {
            return;
        }
        float maxHealth = player.getMaxHealth();
        if (maxHealth > 0.0F && player.getHealth() < maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    private static LandRegion protectedLandAt(ServerLevel level, BlockPos pos) {
        EconomyState state = EconomyState.get(level.getServer());
        LandRegion exact = state.landAt(level.dimension(), pos);
        if (exact != null && exact.type().protectedLand()) {
            return exact;
        }
        LandRegion column = state.landColumnAt(level.dimension(), pos);
        return column != null && column.type() == LandType.ADMIN ? column : null;
    }

    private static boolean isFireBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
    }

    private static void recordProtectedViolation(Player player, String reason) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.hasPermissions(2)) {
            return;
        }
        long now = System.currentTimeMillis();
        ViolationState state = LAND_VIOLATIONS.compute(serverPlayer.getUUID(), (uuid, current) -> {
            if (current == null || now - current.firstSeenMs > VIOLATION_WINDOW_MS) {
                return new ViolationState(now, 1);
            }
            current.count++;
            return current;
        });
        if (state.count >= VIOLATION_BAN_THRESHOLD) {
            LAND_VIOLATIONS.remove(serverPlayer.getUUID());
            Date nowDate = new Date(now);
            Date expires = new Date(now + 1_000L);
            serverPlayer.server.getPlayerList().getBans().add(new UserBanListEntry(
                serverPlayer.getGameProfile(),
                nowDate,
                "NoGeon Economy Land",
                expires,
                "Protected land violation: " + reason
            ));
            serverPlayer.connection.disconnect(Component.literal("보호 구역을 반복해서 건드려 1초 동안 차단되었습니다."));
            return;
        }
        serverPlayer.displayClientMessage(Component.literal("§c[보호 구역 경고] §f다른 사람 토지나 스폰지점은 건드릴 수 없습니다. 반복 시 잠시 차단됩니다. §7(" + state.count + "/" + VIOLATION_BAN_THRESHOLD + ")"), true);
    }

    private static boolean isOperatorAttack(net.minecraft.world.entity.Entity attacker) {
        return attacker instanceof Player player && player.hasPermissions(2);
    }

    private static boolean shouldBlockProtectedDamage(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return false;
        }
        ServerPlayer attacker = playerAttacker(source);
        boolean operatorAttack = attacker != null ? attacker.hasPermissions(2) : isOperatorAttack(source.getEntity());
        LandRegion land = adminLandAt(level, victim.blockPosition());
        if (land != null && !operatorAttack) {
            return true;
        }
        if (attacker == null || attacker == victim || attacker.hasPermissions(2)) {
            return false;
        }
        if (victim instanceof ServerPlayer target) {
            EconomyState state = EconomyState.get(level.getServer());
            PlayerProfile attackerProfile = state.profile(attacker.getUUID());
            PlayerProfile targetProfile = state.profile(target.getUUID());
            return attackerProfile.peacefulFlag() || targetProfile.peacefulFlag();
        }
        return false;
    }

    private static LandRegion adminLandAt(ServerLevel level, BlockPos pos) {
        EconomyState state = EconomyState.get(level.getServer());
        LandRegion exact = state.landAt(level.dimension(), pos);
        if (exact != null && exact.type() == LandType.ADMIN) {
            return exact;
        }
        LandRegion column = state.landColumnAt(level.dimension(), pos);
        return column != null && column.type() == LandType.ADMIN ? column : null;
    }

    private static ServerPlayer playerAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            return player;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof ServerPlayer player) {
            return player;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        if (direct instanceof OwnableEntity ownable && ownable.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static final class ViolationState {
        private final long firstSeenMs;
        private int count;

        private ViolationState(long firstSeenMs, int count) {
            this.firstSeenMs = firstSeenMs;
            this.count = count;
        }
    }
}
