package com.beastmark.bugreports.commands;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.gui.ReportCreationGUI;
import com.beastmark.bugreports.gui.ReportStatusGUI;
import com.beastmark.bugreports.gui.PlayerReportsGUI;
import com.beastmark.bugreports.model.Report;
import com.beastmark.bugreports.model.ReportType;
import com.beastmark.bugreports.utils.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BugReportCommand implements CommandExecutor {
    private final BugReports plugin;

    public BugReportCommand(BugReports plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageManager.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("bugreports.create")) {
            player.sendMessage(MessageManager.getMessage("no-permission"));
            return true;
        }

        if (plugin.getDatabaseManager().isBlacklisted(player.getUniqueId())) {
            player.sendMessage(MessageManager.getMessage("blacklisted"));
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "status":
                    new ReportStatusGUI(player).open();
                    break;
                case "list":
                    new PlayerReportsGUI(player).open();
                    break;
                case "help":
                    sendHelp(player);
                    break;
                default:
                    new ReportCreationGUI(player, ReportType.BUG).open();
                    break;
            }
        } else {
            new ReportCreationGUI(player, ReportType.BUG).open();
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageManager.getMessage("player-help"));
    }
} 