package com.beastmark.bugreports.commands;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.gui.ReportStatusGUI;
import com.beastmark.bugreports.utils.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportStatsCommand implements CommandExecutor {
    private final BugReports plugin;

    public ReportStatsCommand(BugReports plugin) {
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

        new ReportStatusGUI(player).open();
        return true;
    }
} 