package com.nogeon.economyland.network;

import com.nogeon.economyland.job.JobEvents;
import com.nogeon.economyland.player.JobProgress;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.network.NetworkEvent;

public final class MinerChargePacket {
    private static final Queue<ScheduledBreak> BREAK_QUEUE = new ArrayDeque<>();
    private final int action; // 1: start, 2: release

    public MinerChargePacket(int action) {
        this.action = action;
    }

    public static void encode(MinerChargePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.action);
    }

    public static MinerChargePacket decode(FriendlyByteBuf buffer) {
        return new MinerChargePacket(buffer.readInt());
    }

    public static void handle(MinerChargePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            if (profile.selectedJob() != JobType.MINER) {
                return;
            }

            int skillLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_DEEP_BREATH);
            if (skillLevel >= 0) {
                return;
            }

            String startKey = "EarthShatterChargeStart_" + sender.getUUID();
            if (packet.action == 1) {
                sender.getPersistentData().putLong(startKey, System.currentTimeMillis());
                sender.getPersistentData().putBoolean("nogeon_miner_charging", true);
                sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 0.7F);
            } else if (packet.action == 2 && sender.getPersistentData().contains(startKey)) {
                long start = sender.getPersistentData().getLong(startKey);
                sender.getPersistentData().remove(startKey);
                sender.getPersistentData().putBoolean("nogeon_miner_charging", false);
                double durationSec = (System.currentTimeMillis() - start) / 1000.0D;
                if (durationSec >= 0.2D) {
                    double chargeRatio = Math.min(1.0D, durationSec / 3.0D);
                    performEarthShatter(sender, profile.job(JobType.MINER), skillLevel, chargeRatio);
                }
            }
        });
        context.setPacketHandled(true);
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        long gameTime = server.overworld().getGameTime();

        // 1. 차징 중인 광부 플레이어 고퀄리티 충전 이펙트 및 비콘 루프음 연출
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            net.minecraft.nbt.CompoundTag pData = player.getPersistentData();
            if (pData.getBoolean("nogeon_miner_charging")) {
                String startKey = "EarthShatterChargeStart_" + player.getUUID();
                if (pData.contains(startKey)) {
                    long startTime = pData.getLong(startKey);
                    double chargeSec = (System.currentTimeMillis() - startTime) / 1000.0D;
                    double chargeRatio = Math.min(1.0D, chargeSec / 3.0D);

                    ServerLevel level = player.serverLevel();
                    int particleCount = (int) (1 + chargeRatio * 6);
                    double radius = 0.5D + (1.0D - chargeRatio) * 1.2D;

                    for (int i = 0; i < particleCount; i++) {
                        double angle = level.random.nextDouble() * 2.0D * Math.PI;
                        double px = player.getX() + Math.cos(angle) * radius;
                        double py = player.getY() + level.random.nextDouble() * 2.0D;
                        double pz = player.getZ() + Math.sin(angle) * radius;

                        double dx = (player.getX() - px) * 0.15D;
                        double dy = 0.05D;
                        double dz = (player.getZ() - pz) * 0.15D;

                        level.sendParticles(ParticleTypes.GLOW, px, py, pz, 1, dx, dy, dz, 0.01D);
                        level.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, dx, dy, dz, 0.02D);
                        if (chargeRatio > 0.6D) {
                            level.sendParticles(ParticleTypes.ENCHANT, px, py + 0.5D, pz, 1, dx, dy + 0.1D, dz, 0.05D);
                        }
                    }

                    if (gameTime % 6 == 0) {
                        float pitch = 0.5F + (float) chargeRatio * 0.8F;
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.35F + (float) chargeRatio * 0.25F, pitch);
                    }
                }
            }
        }

        // 2. 예정된 대지분쇄 블록 파괴 순차 진행
        int processed = 0;
        while (!BREAK_QUEUE.isEmpty() && BREAK_QUEUE.peek().dueTick <= gameTime && processed < 28) {
            ScheduledBreak task = BREAK_QUEUE.poll();
            ServerLevel level = server.getLevel(task.level.dimension());
            ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
            if (level != null && player != null) {
                breakOne(level, player, task.pos);
            }
            processed++;
        }
    }

    private static void performEarthShatter(ServerPlayer player, JobProgress progress, int skillLevel, double chargeRatio) {
        ServerLevel level = player.serverLevel();
        Direction dir = Direction.getNearest(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z);
        int maxDistance = Math.max(3, (int) Math.round(chargeRatio * Math.min(18.0D, 4 + skillLevel * 0.8D + progress.level() / 60.0D)));
        int maxBlocks = Math.max(10, (int) Math.round(chargeRatio * Math.min(96.0D, 16 + skillLevel * 4.0D)));

        // 수직/수평 폭 반경 너프 계산식 적용
        int baseHeightRadius = skillLevel >= 5 ? 1 : 0;
        int baseWidthRadius = skillLevel / 8;

        baseHeightRadius = Math.min(1, baseHeightRadius);
        baseWidthRadius = Math.min(2, baseWidthRadius);

        int heightRadius = (int) Math.round(chargeRatio * baseHeightRadius);
        int widthRadius = (int) Math.round(chargeRatio * baseWidthRadius);

        HitResult hit = player.pick(6.0D, 0.0F, false);
        BlockPos origin = hit.getType() == HitResult.Type.BLOCK
            ? ((BlockHitResult) hit).getBlockPos()
            : player.blockPosition().relative(dir);
        List<BlockPos> targets = collectLineTargets(level, player, origin, dir, maxDistance, heightRadius, widthRadius);
        if (targets.size() > maxBlocks) {
            targets = new ArrayList<>(targets.subList(0, maxBlocks));
        }
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.literal("§6[대지분쇄] §7파괴할 광맥이 없습니다."), true);
            return;
        }

        long startTick = level.getGameTime();
        UUID playerId = player.getUUID();
        int index = 0;
        for (BlockPos pos : targets) {
            BREAK_QUEUE.add(new ScheduledBreak(level, playerId, pos, startTick + index / Math.max(1, 3 + widthRadius)));
            index++;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.55F + (float) chargeRatio * 0.4F);
        player.displayClientMessage(Component.literal("§6[대지분쇄] §f충전 " + Math.round(chargeRatio * 100.0D)
            + "%, 예정 파괴 " + targets.size() + "개, 폭(상하 " + (heightRadius * 2 + 1) + ", 좌우 " + (widthRadius * 2 + 1) + ")"), true);
    }

    private static List<BlockPos> collectLineTargets(ServerLevel level, ServerPlayer player, BlockPos origin, Direction dir, int distance, int heightRadius, int widthRadius) {
        List<BlockPos> targets = new ArrayList<>();
        for (int step = 0; step < distance; step++) {
            BlockPos center = origin.relative(dir, step);
            if (dir.getAxis() == Direction.Axis.Y) {
                // 위나 아래를 채굴할 때: X, Z축을 각각 폭으로 사용
                for (int a = -widthRadius; a <= widthRadius; a++) {
                    for (int b = -heightRadius; b <= heightRadius; b++) {
                        BlockPos pos = center.offset(a, 0, b);
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && isDestructible(state) && canBreak(level, player, pos, state)) {
                            targets.add(pos.immutable());
                        }
                    }
                }
            } else {
                // 수평(X or Z)을 채굴할 때:
                // 수직은 Y축 (heightRadius)
                // 수평은 perpendicular horizontal axis (widthRadius)
                for (int yOffset = -heightRadius; yOffset <= heightRadius; yOffset++) {
                    for (int hOffset = -widthRadius; hOffset <= widthRadius; hOffset++) {
                        BlockPos pos;
                        if (dir.getAxis() == Direction.Axis.X) {
                            pos = center.offset(0, yOffset, hOffset);
                        } else {
                            pos = center.offset(hOffset, yOffset, 0);
                        }
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && isDestructible(state) && canBreak(level, player, pos, state)) {
                            targets.add(pos.immutable());
                        }
                    }
                }
            }
        }
        return targets;
    }

    private static boolean canBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
        return postProtectedBreakEvent(player, breakEvent);
    }

    private static void breakOne(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !isDestructible(state) || !canBreak(level, player, pos, state)) {
            return;
        }
        ItemStack pickaxe = player.getMainHandItem();
        
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
            8, 0.2D, 0.2D, 0.2D, 0.05D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
            2, 0.15D, 0.15D, 0.15D, 0.01D);
            
        if (level.random.nextDouble() < 0.25D) {
            level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, state),
            pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 12, 0.25D, 0.25D, 0.25D, 0.08D);

        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.75F, 0.7F + level.random.nextFloat() * 0.35F);
        level.destroyBlock(pos, false, player);
        Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, pickaxe);
        JobEvents.handleMinerBreakEvents(player, state, pos, true);
        if (pickaxe.isDamageableItem()) {
            pickaxe.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
    }

    private static boolean postProtectedBreakEvent(ServerPlayer player, BlockEvent.BreakEvent breakEvent) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        boolean previous = data.getBoolean("nogeon_suppress_miner_break_event");
        data.putBoolean("nogeon_suppress_miner_break_event", true);
        try {
            MinecraftForge.EVENT_BUS.post(breakEvent);
            return !breakEvent.isCanceled();
        } finally {
            if (previous) {
                data.putBoolean("nogeon_suppress_miner_break_event", true);
            } else {
                data.remove("nogeon_suppress_miner_break_event");
            }
        }
    }

    private static boolean isDestructible(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.BEDROCK || block == Blocks.BARRIER) {
            return false;
        }
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    private record ScheduledBreak(ServerLevel level, UUID playerId, BlockPos pos, long dueTick) {
    }
}
