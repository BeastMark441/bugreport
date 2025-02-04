package com.beastmark.bugreports.commands;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.gui.AdminGUI;
import com.beastmark.bugreports.model.Report;
import com.beastmark.bugreports.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ReportAdminCommand implements CommandExecutor {
    private final BugReports plugin;

    public ReportAdminCommand(BugReports plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bugreports.admin")) {
            sender.sendMessage(MessageManager.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(MessageManager.getMessage("player-only"));
                    return true;
                }
                new AdminGUI((Player) sender).open();
                break;

            case "status":
                if (args.length < 3) {
                    sender.sendMessage(MessageManager.getMessage("invalid-args"));
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    String status = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                    List<String> availableStatuses = BugReports.getInstance().getConfig().getStringList("statuses");
                    if (!availableStatuses.contains(status)) {
                        sender.sendMessage(MessageManager.getMessage("invalid-status",
                            "%statuses%", String.join(", ", availableStatuses)));
                        return true;
                    }
                    updateStatus(sender, id, status);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageManager.getMessage("invalid-id"));
                }
                break;

            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(MessageManager.getMessage("invalid-args"));
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    Report report = plugin.getDatabaseManager().getReport(id);
                    if (report == null) {
                        sender.sendMessage(MessageManager.getMessage("report-not-found"));
                        return true;
                    }
                    plugin.getDatabaseManager().deleteReport(id);
                    sender.sendMessage(MessageManager.getMessage("report-deleted", "%id%", String.valueOf(id)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageManager.getMessage("invalid-id"));
                }
                break;

            case "blacklist":
                if (args.length < 2) {
                    sender.sendMessage(MessageManager.getMessage("invalid-args"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(MessageManager.getMessage("player-not-found"));
                    return true;
                }
                plugin.getDatabaseManager().addToBlacklist(target.getUniqueId());
                sender.sendMessage(MessageManager.getMessage("player-blacklisted", 
                    "%player%", target.getName()));
                break;

            case "unblacklist":
                if (args.length < 2) {
                    sender.sendMessage(MessageManager.getMessage("invalid-args"));
                    return true;
                }
                Player targetUnblacklist = Bukkit.getPlayer(args[1]);
                if (targetUnblacklist == null) {
                    sender.sendMessage(MessageManager.getMessage("player-not-found"));
                    return true;
                }
                plugin.getDatabaseManager().removeFromBlacklist(targetUnblacklist.getUniqueId());
                sender.sendMessage(MessageManager.getMessage("player-unblacklisted", 
                    "%player%", targetUnblacklist.getName()));
                break;

            case "reload":
                plugin.reloadConfig();
                MessageManager.load();
                sender.sendMessage(MessageManager.getMessage("config-reloaded"));
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageManager.getMessage("admin-help"));
    }

    private void updateStatus(CommandSender sender, int id, String status) {
        Report report = plugin.getDatabaseManager().getReport(id);
        if (report != null) {
            plugin.getDatabaseManager().updateReportStatus(id, status);
            sender.sendMessage(MessageManager.getMessage("status-changed", 
                "%id%", String.valueOf(id),
                "%status%", status));
            
            // Отправляем уведомление владельцу репорта
            plugin.getNotificationManager().addNotification(report.getPlayerUUID(), id);
        } else {
            sender.sendMessage(MessageManager.getMessage("report-not-found"));
        }
    }
} 