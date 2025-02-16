package com.beastmark.bugreports.listeners;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.Report;
import com.beastmark.bugreports.utils.MessageManager;
import com.beastmark.bugreports.utils.ReportCreationManager;
import com.beastmark.bugreports.utils.ReportCreationManager.ReportCreationState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;


public class ChatListener implements Listener {
    private final BugReports plugin;

    public ChatListener(BugReports plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ReportCreationState state = ReportCreationManager.getPlayerState(player);
        
        if (state == null) {
            return;
        }

        event.setCancelled(true);
        Component message = event.message();
        String description;
        if (message instanceof TextComponent) {
            description = ((TextComponent) message).content();
        } else {
            description = message.toString();
        }

        if (description.equalsIgnoreCase("cancel")) {
            ReportCreationManager.removePlayerState(player);
            player.sendMessage(MessageManager.getMessage("report-cancelled"));
            return;
        }

        int minLength = plugin.getConfig().getInt("limits.min-description-length", 10);
        int maxLength = plugin.getConfig().getInt("limits.max-description-length", 500);

        if (description.length() < minLength) {
            player.sendMessage(MessageManager.getMessage("too-short", 
                "%min%", String.valueOf(minLength)));
            return;
        }

        if (description.length() > maxLength) {
            player.sendMessage(MessageManager.getMessage("too-long", 
                "%max%", String.valueOf(maxLength)));
            return;
        }

        // Создаем репорт
        Report report = new Report(
            player.getUniqueId(),
            player.getName(),
            state.category,
            description,
            state.type
        );
        
        plugin.getDatabaseManager().saveReport(report);
        ReportCreationManager.removePlayerState(player);
        
        player.sendMessage(MessageManager.getMessage("report-created", 
            "%id%", String.valueOf(report.getId())));

        // Уведомляем администраторов
        plugin.getNotificationManager().addAdminNotification();
    }
} 