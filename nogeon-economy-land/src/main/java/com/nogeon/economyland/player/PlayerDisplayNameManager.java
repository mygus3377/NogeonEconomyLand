package com.nogeon.economyland.player;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

public final class PlayerDisplayNameManager {
    private static final String TEAM_PREFIX = "ngel_";

    private PlayerDisplayNameManager() {
    }

    public static void refresh(ServerPlayer player, PlayerProfile profile) {
        ServerScoreboard scoreboard = player.server.getScoreboard();
        String teamName = teamName(player.getUUID());
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }

        team.setPlayerPrefix(buildBadge(profile));
        team.setPlayerSuffix(Component.empty());
        team.setColor(ChatFormatting.WHITE);

        String entry = player.getScoreboardName();
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(entry);
        if (currentTeam != null && currentTeam != team) {
            scoreboard.removePlayerFromTeam(entry);
        }
        scoreboard.addPlayerToTeam(entry, team);
    }

    private static String teamName(UUID uuid) {
        String compact = uuid.toString().replace("-", "");
        return TEAM_PREFIX + compact.substring(0, 11);
    }

    private static Component buildBadge(PlayerProfile profile) {
        return Component.literal("[")
            .append(Component.translatable(profile.socialClass().translationKey()).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("/"))
            .append(Component.translatable("job.nogeon_economy_land." + profile.selectedJob().id()).withStyle(ChatFormatting.GREEN))
            .append(Component.literal("] "));
    }
}