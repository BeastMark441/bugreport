package com.beastmark.bugreports.utils;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.Report;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class NotificationManager {
    private static final Map<UUID, Set<Integer>> pendingNotifications = new HashMap<>();
    private static final Set<UUID> newReportsForAdmin = new HashSet<>();
    private final BugReports plugin;

    public NotificationManager(BugReports plugin) {
        this.plugin = plugin;
        startNotificationTask();
    }

    public void addNotification(UUID playerId, int reportId) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report != null) {
            pendingNotifications.computeIfAbsent(playerId, k -> new HashSet<>()).add(reportId);
            
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                sendNotification(player);
            }
            
            // Сохраняем уведомление для оффлайн игроков
            saveOfflineNotification(playerId, reportId);
        }
    }

    public void addAdminNotification() {
        if (plugin.getConfig().getBoolean("admin.notify-new-reports")) {
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("bugreports.admin"))
                .forEach(admin -> {
                    admin.sendMessage(MessageManager.getMessage("new-reports-admin"));
                    playNotificationSound(admin);
                });
        }
    }

    public void sendNotification(Player player) {
        Set<Integer> reports = pendingNotifications.get(player.getUniqueId());
        if (reports != null && !reports.isEmpty()) {
            player.sendMessage(MessageManager.getMessage("report-updates-header"));
            
            for (int reportId : reports) {
                Report report = plugin.getDatabaseManager().getReport(reportId);
                if (report != null) {
                    // Создаем кликабельное сообщение
                    net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(
                        MessageManager.getMessage("report-notification-format",
                            "%id%", String.valueOf(reportId),
                            "%status%", report.getStatus(),
                            "%type%", report.getType().name().equals("BUG") ? "Баг" : "Предложение")
                    );
                    
                    // Добавляем подсказку и команду при клике
                    message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.ComponentBuilder("Нажмите, чтобы открыть статус репортов")
                            .create()
                    ));
                    message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                        "/bugreport status"
                    ));
                    
                    player.spigot().sendMessage(message);
                }
            }
            
            pendingNotifications.remove(player.getUniqueId());
        }
    }

    private void playNotificationSound(Player player) {
        try {
            String soundName = plugin.getConfig().getString("notifications.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound in config: " + e.getMessage());
        }
    }

    private void startNotificationTask() {
        int interval = plugin.getConfig().getInt("notifications.check-interval", 300) * 20;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (pendingNotifications.containsKey(player.getUniqueId())) {
                    sendNotification(player);
                }
            }
        }, interval, interval);
    }

    private void saveOfflineNotification(UUID playerId, int reportId) {
        // Можно добавить сохранение в базу данных для оффлайн игроков
        // Или использовать файл для хранения
    }
} 