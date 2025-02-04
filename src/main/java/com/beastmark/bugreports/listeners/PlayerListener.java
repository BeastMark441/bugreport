package com.beastmark.bugreports.listeners;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.gui.ReportStatusGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {
    private final BugReports plugin;

    public PlayerListener(BugReports plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getConfig().getBoolean("notifications.notify-on-join")) {
            plugin.getNotificationManager().sendNotification(player);
        }
        
        if (plugin.getConfig().getBoolean("notifications.auto-open-status") && 
            plugin.getDatabaseManager().hasUnreadReports(player.getUniqueId())) {
            new ReportStatusGUI(player).open();
        }
    }
} 