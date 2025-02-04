package com.beastmark.bugreports.placeholders;

import com.beastmark.bugreports.BugReports;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BugReportsExpansion extends PlaceholderExpansion {
    private final BugReports plugin;

    public BugReportsExpansion(BugReports plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bugreports";
    }

    @Override
    public @NotNull String getAuthor() {
        return "beastmark";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        switch (identifier) {
            case "total":
                return String.valueOf(plugin.getDatabaseManager().getTotalReports());
            case "player_reports":
                return String.valueOf(plugin.getDatabaseManager().getPlayerReports(player.getUniqueId()).size());
            case "is_blacklisted":
                return plugin.getDatabaseManager().isBlacklisted(player.getUniqueId()) ? "да" : "нет";
            case "player_report_done":
                return String.valueOf(plugin.getDatabaseManager().getPlayerCompletedReports(player.getUniqueId()));
            default:
                return null;
        }
    }
} 