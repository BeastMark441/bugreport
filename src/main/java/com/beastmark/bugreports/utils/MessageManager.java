package com.beastmark.bugreports.utils;

import com.beastmark.bugreports.BugReports;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageManager {
    private static FileConfiguration messages;
    private static String prefix;

    public static void load() {
        File file = new File(BugReports.getInstance().getDataFolder(), "messages.yml");
        if (!file.exists()) {
            BugReports.getInstance().saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
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