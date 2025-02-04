package com.beastmark.bugreports.commands;

import com.beastmark.bugreports.BugReports;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportAdminTabCompleter implements TabCompleter {
    private final BugReports plugin;

    public ReportAdminTabCompleter(BugReports plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("bugreports.admin")) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(List.of("gui", "status", "delete", "blacklist", "unblacklist", "reload"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "status":
                case "delete":
                    // Добавляем ID всех репортов
                    completions.addAll(plugin.getDatabaseManager().getAllReports().stream()
                        .map(r -> String.valueOf(r.getId()))
                        .collect(Collectors.toList()));
                    break;
                case "blacklist":
                case "unblacklist":
                    // Добавляем имена онлайн игроков
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("status")) {
            // Добавляем доступные статусы
            completions.addAll(plugin.getConfig().getStringList("statuses"));
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
} 