package com.beastmark.bugreports.commands;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.utils.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TelegramCommand implements CommandExecutor {
    private final BugReports plugin;

    public TelegramCommand(BugReports plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageManager.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bugreports.telegram")) {
            player.sendMessage(MessageManager.getMessage("no-permission"));
            return true;
        }

        if (!plugin.getConfig().getBoolean("telegram.enabled", false)) {
            player.sendMessage(MessageManager.getMessage("telegram-not-enabled"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(MessageManager.getMessage("telegram-help"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "link":
                String code = plugin.getTelegramManager().generateVerificationCode(player.getUniqueId());
                player.sendMessage(MessageManager.getMessage("telegram-code", 
                    "%code%", code));
                break;
            case "unlink":
                // Логика отвязки аккаунта будет реализована в TelegramManager
                if (plugin.getTelegramManager().unlinkAccount(player.getUniqueId())) {
                    player.sendMessage(MessageManager.getMessage("telegram-unlinked"));
                } else {
                    player.sendMessage(MessageManager.getMessage("telegram-status-not-linked"));
                }
                break;
            case "status":
                if (plugin.getTelegramManager().isPlayerLinked(player.getUniqueId())) {
                    player.sendMessage(MessageManager.getMessage("telegram-status-linked"));
                } else {
                    player.sendMessage(MessageManager.getMessage("telegram-status-not-linked"));
                }
                break;
            default:
                player.sendMessage(MessageManager.getMessage("telegram-help"));
                break;
        }

        return true;
    }
} 