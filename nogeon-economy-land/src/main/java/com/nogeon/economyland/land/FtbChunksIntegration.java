package com.nogeon.economyland.land;

import com.nogeon.economyland.state.EconomyState;
import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class FtbChunksIntegration {
    private FtbChunksIntegration() {
    }

    public static void init() {
        if (ModList.get().isLoaded("ftbchunks")) {
            registerEvents();
        }
    }

    private static void registerEvents() {
        ClaimedChunkEvent.BEFORE_CLAIM.register(FtbChunksIntegration::onBeforeClaimOrLoad);
        ClaimedChunkEvent.BEFORE_LOAD.register(FtbChunksIntegration::onBeforeClaimOrLoad);
    }

    private static CompoundEventResult<ClaimResult> onBeforeClaimOrLoad(CommandSourceStack source, dev.ftb.mods.ftbchunks.api.ClaimedChunk chunk) {
        // Operators are always allowed to claim/load
        if (source.hasPermission(2)) {
            return CompoundEventResult.pass();
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return CompoundEventResult.pass();
        }

        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        var dimension = chunk.getPos().dimension();

        EconomyState state = EconomyState.get(player.server);
        boolean allowed = false;

        for (LandRegion land : state.lands()) {
            if (land.type() == LandType.INDUSTRIAL && (land.owner().equals(player.getUUID()) || land.canBuild(player.getUUID()))) {
                if (land.world().equals(dimension)) {
                    // Check X and Z overlap between Industrial Land cuboid and 16x16 Chunk
                    boolean xOverlaps = land.max().getX() >= (chunkX * 16) && land.min().getX() <= (chunkX * 16 + 15);
                    boolean zOverlaps = land.max().getZ() >= (chunkZ * 16) && land.min().getZ() <= (chunkZ * 16 + 15);
                    if (xOverlaps && zOverlaps) {
                        allowed = true;
                        break;
                    }
                }
            }
        }

        if (!allowed) {
            return CompoundEventResult.interruptFalse(ClaimResult.customProblem("§c[보호 오류] §f일반 유저는 자신의 §e산업 토지§f 구역 내에서만 청크를 점유/로딩할 수 있습니다."));
        }

        return CompoundEventResult.pass();
    }
}
