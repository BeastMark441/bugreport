package com.beastmark.bugreports.utils;

import com.beastmark.bugreports.BugReports;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker implements Listener {
    private final BugReports plugin;
    private final String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(BugReports plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        checkForUpdates();
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // GitHub API URL для получения последнего релиза
                URL url = new URL("https://api.github.com/repos/BeastMark441/bugreport/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Извлекаем версию из ответа
                    Pattern pattern = Pattern.compile("\"tag_name\":\"v?([0-9.]+)\"");
                    Matcher matcher = pattern.matcher(response.toString());
                    
                    if (matcher.find()) {
                        latestVersion = matcher.group(1);
                        updateAvailable = !currentVersion.equals(latestVersion);
                        
                        if (updateAvailable) {
                            plugin.getLogger().info("Доступна новая версия BugReports: " + latestVersion);
                            plugin.getLogger().info("Текущая версия: " + currentVersion);
                            plugin.getLogger().info("Скачать: https://github.com/BeastMark441/bugreport/releases/latest");
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось проверить обновления: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("bugreports.admin") && updateAvailable) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(MessageManager.getMessage("update-available",
                    "%current_version%", currentVersion,
                    "%new_version%", latestVersion));
            }, 40L); // Задержка в 2 секунды после входа
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
} 