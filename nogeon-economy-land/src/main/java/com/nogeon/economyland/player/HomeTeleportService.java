package com.nogeon.economyland.player;

import com.nogeon.economyland.state.EconomyState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

public final class HomeTeleportService {
    private static final double MOVE_CANCEL_DISTANCE_SQR = 0.04D;
    private static final Map<UUID, PendingTeleport> PENDING = new HashMap<>();

    private HomeTeleportService() {
    }

    public static boolean request(ServerPlayer player, HomeEntry home) {
        if (player.isPassenger()) {
            player.displayClientMessage(Component.literal("앉아 있는 상태에서는 귀환할 수 없습니다.").withStyle(ChatFormatting.RED), false);
            return false;
        }

        ServerLevel level = player.server.getLevel(home.worldKey());
        if (level == null) {
            player.displayClientMessage(Component.translatable("command.nogeon_economy_land.home.dimension_missing", home.name())
                .withStyle(ChatFormatting.RED), false);
            return false;
        }

        SocialClass socialClass = EconomyState.get(player.server).profile(player.getUUID()).socialClass();
        int delaySeconds = socialClass.homeTeleportDelaySeconds();
        long executeAtTick = player.server.overworld().getGameTime() + delaySeconds * 20L;
        PENDING.put(player.getUUID(), new PendingTeleport(home.name(), home.worldKey(), home.pos(), player.level().dimension(),
            player.position(), executeAtTick, socialClass.homeAllowsMovement(), false, -1));
        Component message = banner(ChatFormatting.DARK_AQUA)
            .append(Component.translatable("message.nogeon_economy_land.home.start", delaySeconds, home.name())
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.displayClientMessage(message, false);

        if (socialClass.homeAllowsMovement()) {
            player.displayClientMessage(banner(ChatFormatting.GOLD)
                .append(Component.translatable("message.nogeon_economy_land.home.move_allowed",
                    Component.translatable(socialClass.translationKey()))
                    .withStyle(ChatFormatting.YELLOW)), false);
        } else {
            player.displayClientMessage(banner(ChatFormatting.GRAY)
                .append(Component.translatable("message.nogeon_economy_land.home.move_cancels")
                    .withStyle(ChatFormatting.GRAY)), false);
        }
        return true;
    }

    public static void requestSpawn(ServerPlayer player) {
        if (player.isPassenger()) {
            player.displayClientMessage(Component.literal("앉아 있는 상태에서는 귀환할 수 없습니다.").withStyle(ChatFormatting.RED), false);
            return;
        }
        SocialClass socialClass = EconomyState.get(player.server).profile(player.getUUID()).socialClass();
        int delaySeconds = socialClass.spawnReturnDelaySeconds();
        long executeAtTick = player.server.overworld().getGameTime() + delaySeconds * 20L;
        PENDING.put(player.getUUID(), new PendingTeleport("", Level.OVERWORLD, player.server.overworld().getSharedSpawnPos(),
            player.level().dimension(), player.position(), executeAtTick, false, true, -1));
        player.displayClientMessage(banner(ChatFormatting.DARK_AQUA)
            .append(Component.translatable("message.nogeon_economy_land.spawn_return.start", delaySeconds)
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)), false);
        player.displayClientMessage(banner(ChatFormatting.GRAY)
            .append(Component.translatable("message.nogeon_economy_land.spawn_return.move_cancels")
                .withStyle(ChatFormatting.GRAY)), false);
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        PendingTeleport pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return;
        }

        if (!player.isAlive()) {
            PENDING.remove(player.getUUID());
            return;
        }

        long currentTick = player.server.overworld().getGameTime();
        if (!pending.allowsMovement() && moved(player, pending)) {
            PENDING.remove(player.getUUID());
            String cancelKey = pending.spawnReturn()
                ? "message.nogeon_economy_land.spawn_return.cancelled_move"
                : "message.nogeon_economy_land.home.cancelled_move";
            player.displayClientMessage(banner(ChatFormatting.RED)
                .append(Component.translatable(cancelKey)
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)), false);
            return;
        }

        int secondsRemaining = (int) Math.ceil(Math.max(0L, pending.executeAtTick() - currentTick) / 20.0D);
        if (secondsRemaining > 0 && secondsRemaining <= 3 && secondsRemaining != pending.lastCountdownSecond()) {
            PENDING.put(player.getUUID(), pending.withLastCountdownSecond(secondsRemaining));
            String key = pending.spawnReturn()
                ? "message.nogeon_economy_land.spawn_return.countdown"
                : pending.allowsMovement()
                ? "message.nogeon_economy_land.home.countdown_mobile"
                : "message.nogeon_economy_land.home.countdown";
            player.displayClientMessage(banner(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key, secondsRemaining).withStyle(ChatFormatting.LIGHT_PURPLE)), false);
            return;
        }

        if (currentTick < pending.executeAtTick()) {
            return;
        }

        PENDING.remove(player.getUUID());
        ServerLevel level = player.server.getLevel(pending.targetWorld());
        if (level == null) {
            player.displayClientMessage(Component.translatable("command.nogeon_economy_land.home.dimension_missing", pending.homeName())
                .withStyle(ChatFormatting.RED), false);
            return;
        }
        if (player.isPassenger()) {
            player.stopRiding();
        }
        player.teleportTo(level, pending.targetPos().getX() + 0.5D, pending.targetPos().getY(), pending.targetPos().getZ() + 0.5D,
            player.getYRot(), player.getXRot());
        if (pending.spawnReturn()) {
            player.displayClientMessage(banner(ChatFormatting.GREEN)
                .append(Component.translatable("message.nogeon_economy_land.spawn_return.complete")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)), false);
            return;
        }
        player.displayClientMessage(banner(ChatFormatting.GREEN)
            .append(Component.translatable("message.nogeon_economy_land.home.complete", pending.homeName())
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)), false);
    }

    private static boolean moved(ServerPlayer player, PendingTeleport pending) {
        if (!player.level().dimension().equals(pending.originWorld())) {
            return true;
        }
        return player.position().distanceToSqr(pending.originPos()) > MOVE_CANCEL_DISTANCE_SQR;
    }

    private static MutableComponent banner(ChatFormatting color) {
        return Component.literal("[ HOME ] ").withStyle(color, ChatFormatting.BOLD);
    }

    private record PendingTeleport(
        String homeName,
        ResourceKey<Level> targetWorld,
        BlockPos targetPos,
        ResourceKey<Level> originWorld,
        Vec3 originPos,
        long executeAtTick,
        boolean allowsMovement,
        boolean spawnReturn,
        int lastCountdownSecond
    ) {
        private PendingTeleport withLastCountdownSecond(int value) {
            return new PendingTeleport(homeName, targetWorld, targetPos, originWorld, originPos, executeAtTick, allowsMovement, spawnReturn, value);
        }
    }
}
