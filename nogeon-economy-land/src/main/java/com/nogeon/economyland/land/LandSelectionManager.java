package com.nogeon.economyland.land;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class LandSelectionManager {
    private static final Map<UUID, SelectionSession> SELECTIONS = new HashMap<>();

    public enum Mode {
        DECISION, DESIGNATION
    }

    private LandSelectionManager() {
    }

    public static LandSelection get(ServerPlayer player) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        return session == null ? null : copy(session.selection);
    }

    public static BlockPos getPendingFirst(ServerPlayer player) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        return session == null ? null : session.pendingFirst;
    }

    public static boolean isPendingAdditive(ServerPlayer player) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        return session != null && session.pendingAdditive;
    }

    public static LandSelection start(ServerPlayer player, LandType type) {
        SelectionSession session = session(player, type, player.level().dimension());
        session.mode = Mode.DECISION;
        return copy(session.selection);
    }

    public static void enterDesignationMode(ServerPlayer player) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        if (session != null) {
            session.mode = Mode.DESIGNATION;
            session.pendingFirst = null;
        }
    }

    public static void enterDecisionMode(ServerPlayer player) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        if (session != null) {
            session.mode = Mode.DECISION;
            session.pendingFirst = null;
        }
    }

    public static SelectionStepResult handleInteraction(ServerPlayer player, BlockPos pos, boolean additive) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        if (session == null || !session.world.equals(player.level().dimension())) {
            return SelectionStepResult.invalid();
        }

        if (session.mode == Mode.DESIGNATION) {
            if (session.pendingFirst == null) {
                if (pos == null) return SelectionStepResult.invalid();
                session.pendingFirst = pos.immutable();
                session.pendingAdditive = additive;
                return new SelectionStepResult(copy(session.selection), session.pendingFirst, false, true);
            } else {
                if (pos == null) return SelectionStepResult.invalid();
                session.selection.addCuboid(session.pendingFirst, pos.immutable(), session.pendingAdditive);
                session.pendingFirst = null;
                session.mode = Mode.DECISION;
                return new SelectionStepResult(copy(session.selection), null, true, true);
            }
        }
        
        return SelectionStepResult.invalid();
    }

    public static LandSelection undo(ServerPlayer player) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        if (session == null || session.selection == null) {
            return null;
        }
        session.selection.removeLast();
        session.pendingFirst = null;
        session.mode = Mode.DECISION;
        return copy(session.selection);
    }

    public static LandSelection reset(ServerPlayer player) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        if (session == null) {
            return null;
        }
        session.selection = new LandSelection(session.type, session.world);
        session.pendingFirst = null;
        session.mode = Mode.DECISION;
        return copy(session.selection);
    }

    public static Mode getMode(ServerPlayer player) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        return session == null ? Mode.DECISION : session.mode;
    }

    public static LandType currentType(ServerPlayer player) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        return session == null ? null : session.type;
    }

    public static void clear(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
    }

    private static SelectionSession session(ServerPlayer player, LandType type, ResourceKey<Level> world) {
        SelectionSession session = SELECTIONS.get(player.getUUID());
        if (session == null || session.type != type || !session.world.equals(world)) {
            session = new SelectionSession(type, world);
            SELECTIONS.put(player.getUUID(), session);
        }
        return session;
    }

    private static LandSelection copy(LandSelection selection) {
        if (selection == null) {
            return null;
        }
        LandSelection copy = new LandSelection(selection.type(), selection.world());
        for (LandSelection.Cuboid cuboid : selection.cuboids()) {
            copy.addCuboid(cuboid.first().immutable(), cuboid.second().immutable(), cuboid.additive());
        }
        return copy;
    }

    private static final class SelectionSession {
        private final LandType type;
        private final ResourceKey<Level> world;
        private LandSelection selection;
        private BlockPos pendingFirst;
        private boolean pendingAdditive = true;
        private Mode mode = Mode.DECISION;

        private SelectionSession(LandType type, ResourceKey<Level> world) {
            this.type = type;
            this.world = world;
            this.selection = new LandSelection(type, world);
        }
    }

    public record SelectionStepResult(LandSelection selection, BlockPos pendingFirst, boolean cuboidCompleted, boolean success) {
        public static SelectionStepResult invalid() {
            return new SelectionStepResult(null, null, false, false);
        }
    }
}
