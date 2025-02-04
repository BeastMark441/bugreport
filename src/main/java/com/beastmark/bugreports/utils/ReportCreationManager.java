package com.beastmark.bugreports.utils;

import com.beastmark.bugreports.model.ReportType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReportCreationManager {
    private static final Map<UUID, ReportCreationState> playerStates = new HashMap<>();

    public static class ReportCreationState {
        public final ReportType type;
        public final String category;

        public ReportCreationState(ReportType type, String category) {
            this.type = type;
            this.category = category;
        }
    }

    public static void setPlayerState(Player player, ReportType type, String category) {
        playerStates.put(player.getUniqueId(), new ReportCreationState(type, category));
    }

    public static ReportCreationState getPlayerState(Player player) {
        return playerStates.get(player.getUniqueId());
    }

    public static void removePlayerState(Player player) {
        playerStates.remove(player.getUniqueId());
    }
} 