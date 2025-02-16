package com.beastmark.bugreports.utils;

import com.beastmark.bugreports.BugReports;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class MessageManager {
    private static FileConfiguration messages;
    private static String prefix;

    public static void load() {
        File file = new File(BugReports.getInstance().getDataFolder(), "messages.yml");
        
        // Сохраняем дефолтный файл, если его нет
        if (!file.exists()) {
            BugReports.getInstance().saveResource("messages.yml", false);
        }
        
        // Загружаем файл сообщений
        messages = YamlConfiguration.loadConfiguration(file);
        
        // Проверяем и добавляем новые сообщения
        FileConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
            new InputStreamReader(BugReports.getInstance().getResource("messages.yml")));
        
        boolean needsSave = false;
        for (String key : defaultMessages.getKeys(true)) {
            if (!messages.contains(key)) {
                messages.set(key, defaultMessages.get(key));
                needsSave = true;
            }
        }
        
        if (needsSave) {
            try {
                messages.save(file);
                BugReports.getInstance().getLogger().info("Файл messages.yml обновлен новыми сообщениями");
            } catch (IOException e) {
                BugReports.getInstance().getLogger().severe("Ошибка при сохранении messages.yml: " + e.getMessage());
            }
        }
        
        prefix = color(messages.getString("prefix", "&8[&6BugReports&8]"));
    }

    public static String getMessage(String path) {
        return color(prefix + " " + messages.getString("messages." + path, "Message not found: " + path));
    }

    public static String getMessageWithoutPrefix(String path) {
        return color(messages.getString("messages." + path, "Message not found: " + path));
    }

    public static String getRawMessage(String path) {
        return color(messages.getString(path, "Message not found: " + path));
    }

    public static String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    public static String getPrefix() {
        return prefix;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
} 