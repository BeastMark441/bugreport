package com.beastmark.bugreports.utils;

import com.beastmark.bugreports.BugReports;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {
    private final BugReports plugin;
    private String latestVersion;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/BeastMark441/bugreport/releases/latest";

    public UpdateChecker(BugReports plugin) {
        this.plugin = plugin;
        
        // Показываем информацию о текущей версии при запуске
        if (plugin.getConfig().getBoolean("updates.show-version-info", true)) {
            plugin.getLogger().info("Текущая версия плагина: " + plugin.getDescription().getVersion());
        }
        
        // Запускаем периодическую проверку обновлений
        long interval = plugin.getConfig().getLong("updates.check-interval", 24) * 20L * 3600L;
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForUpdates();
            }
        }.runTaskTimerAsynchronously(plugin, 20L, interval);
    }

    public void checkForUpdates() {
        try {
            URL url = new URL(GITHUB_API_URL);
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

                // Извлекаем версию из JSON ответа
                String jsonResponse = response.toString();
                latestVersion = jsonResponse.split("\"tag_name\":\"")[1].split("\"")[0].replace("v", "");
                String currentVersion = plugin.getDescription().getVersion();

                if (!currentVersion.equals(latestVersion)) {
                    String updateMessage = "\n" +
                        "§8§m                                                    §r\n" +
                        "§6Доступно обновление BugReports!\n" +
                        "§7Текущая версия: §f" + currentVersion + "\n" +
                        "§7Новая версия: §f" + latestVersion + "\n" +
                        "§7Скачать: §fhttps://github.com/BeastMark441/bugreport/releases/latest\n" +
                        "§8§m                                                    §r";
                    
                    plugin.getLogger().info("Доступно обновление! Текущая версия: " + currentVersion + ", Новая версия: " + latestVersion);
                    
                    // Отправляем сообщение всем админам онлайн
                    if (plugin.getConfig().getBoolean("updates.notify-admins", true)) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.hasPermission("bugreports.admin")) {
                                player.sendMessage(updateMessage);
                            }
                        }
                    }
                } else if (plugin.getConfig().getBoolean("updates.show-version-info", true)) {
                    plugin.getLogger().info("У вас установлена последняя версия плагина!");
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось проверить наличие обновлений: " + e.getMessage());
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }
} 