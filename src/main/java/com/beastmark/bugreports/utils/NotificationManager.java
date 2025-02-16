package com.beastmark.bugreports.utils;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.Report;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class NotificationManager {
    private static final int MAX_MESSAGES_PER_PLAYER = 50;
    private static final int MAX_NOTIFICATIONS_PER_PLAYER = 100;
    private final Map<UUID, Set<Integer>> pendingNotifications = new HashMap<>();
    private final Map<UUID, List<String>> pendingMessages = new HashMap<>();
    private final BugReports plugin;

    public NotificationManager(BugReports plugin) {
        this.plugin = plugin;
        startNotificationTask();
        startCleanupTask();
    }

    private void startCleanupTask() {
        // Очистка старых уведомлений каждый час
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            cleanupOldNotifications();
        }, 20L * 3600L, 20L * 3600L);
    }

    private void cleanupOldNotifications() {
        // Очищаем уведомления для оффлайн игроков, которые не заходили более 30 дней
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L);
        
        for (UUID playerId : new HashSet<>(pendingNotifications.keySet())) {
            long lastSeen = Bukkit.getOfflinePlayer(playerId).getLastSeen();
            if (lastSeen > 0 && lastSeen < thirtyDaysAgo) {
                pendingNotifications.remove(playerId);
                pendingMessages.remove(playerId);
                plugin.getConfig().set("offline_notifications." + playerId.toString(), null);
            }
        }
        plugin.saveConfig();
    }

    public void addNotification(UUID playerId, int reportId) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report != null) {
            Set<Integer> notifications = pendingNotifications.computeIfAbsent(playerId, k -> new HashSet<>());
            
            // Ограничиваем количество уведомлений
            if (notifications.size() >= MAX_NOTIFICATIONS_PER_PLAYER) {
                notifications.clear(); // Очищаем старые уведомления если достигнут лимит
            }
            
            notifications.add(reportId);
            
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                sendNotification(player);
            } else {
                saveOfflineNotification(playerId, reportId);
            }
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

        // Отправляем уведомление в Telegram
        if (plugin.getTelegramManager() != null && 
            plugin.getConfig().getBoolean("telegram.notifications.new-reports", true)) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                Report report = plugin.getDatabaseManager().getLatestReport();
                if (report != null) {
                    plugin.getTelegramManager().notifyAdminsNewReport(report);
                }
            });
        }
    }

    public void addMessageNotification(UUID playerId, String message) {
        List<String> messages = pendingMessages.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        // Ограничиваем количество сообщений
        if (messages.size() >= MAX_MESSAGES_PER_PLAYER) {
            messages.remove(0); // Удаляем самое старое сообщение
        }
        
        messages.add(message);
        
        // Если игрок онлайн, сразу отправляем сообщение
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            sendPendingMessages(player);
        }
    }

    public void sendNotification(Player player) {
        Set<Integer> reports = pendingNotifications.get(player.getUniqueId());
        if (reports != null && !reports.isEmpty()) {
            player.sendMessage(MessageManager.getMessage("report-updates-header"));
            
            for (int reportId : reports) {
                Report report = plugin.getDatabaseManager().getReport(reportId);
                if (report != null) {
                    net.kyori.adventure.text.TextComponent message = net.kyori.adventure.text.Component.text()
                        .content(MessageManager.getMessage("report-notification-format",
                            "%id%", String.valueOf(reportId),
                            "%status%", report.getStatus(),
                            "%type%", report.getType().name().equals("BUG") ? "Баг" : "Предложение"))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            net.kyori.adventure.text.Component.text(MessageManager.getMessage("click-to-view"))))
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/bugreport status"))
                        .build();
                    
                    player.sendMessage(message);
                }
            }
            
            pendingNotifications.remove(player.getUniqueId());
            playNotificationSound(player);
        }

        sendPendingMessages(player);
    }

    public void sendMessageToPlayer(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(MessageManager.getPrefix() + " " + message);
            playNotificationSound(player);
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
        String path = "offline_notifications." + playerId.toString();
        List<Integer> notifications = plugin.getConfig().getIntegerList(path);
        
        // Ограничиваем количество оффлайн уведомлений
        if (notifications.size() >= MAX_NOTIFICATIONS_PER_PLAYER) {
            notifications.clear();
        }
        
        if (!notifications.contains(reportId)) {
            notifications.add(reportId);
            plugin.getConfig().set(path, notifications);
            plugin.saveConfig();
        }
    }

    private void sendPendingMessages(Player player) {
        List<String> messages = pendingMessages.get(player.getUniqueId());
        if (messages != null && !messages.isEmpty()) {
            player.sendMessage(MessageManager.getMessage("messages-header"));
            for (String message : messages) {
                player.sendMessage(MessageManager.getPrefix() + " " + message);
            }
            playNotificationSound(player);
            pendingMessages.remove(player.getUniqueId());
        }
    }
} 