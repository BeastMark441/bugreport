package com.beastmark.bugreports.gui;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.Report;
import com.beastmark.bugreports.utils.GuiUtils;
import com.beastmark.bugreports.utils.MessageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class StatusChangeGUI extends GUI {
    private final List<Report> reports;
    private final List<String> statuses;

    public StatusChangeGUI(Player player, List<Report> reports) {
        super(player, "Изменить статус", 27);
        this.reports = reports;
        this.statuses = BugReports.getInstance().getConfig().getStringList("statuses");
        init();
    }

    @Override
    public void init() {
        inventory.clear();

        // Заполняем фон
        ItemStack background = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, background);
        }

        // Отображаем доступные статусы
        for (int i = 0; i < statuses.size() && i < inventory.getSize(); i++) {
            String status = statuses.get(i);
            inventory.setItem(i, GuiUtils.createItem(
                getStatusMaterial(status),
                getStatusColor(status) + status,
                "",
                "&7Нажмите, чтобы установить статус",
                "&7для " + reports.size() + " репорт(ов)"
            ));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        preventItemMovement(event);
        
        int slot = event.getSlot();
        if (slot >= 0 && slot < statuses.size()) {
            String newStatus = statuses.get(slot);
            changeStatus(newStatus);
        }
    }

    private void changeStatus(String newStatus) {
        if (!statuses.contains(newStatus)) {
            player.sendMessage(MessageManager.getMessage("invalid-status", 
                "%statuses%", String.join(", ", statuses)));
            return;
        }

        int count = 0;
        for (Report report : reports) {
            String oldStatus = report.getStatus();
            if (!oldStatus.equals(newStatus)) {
                report.setStatus(newStatus);
                BugReports.getInstance().getDatabaseManager().updateReport(report);
                
                // Уведомляем игрока об изменении статуса
                Player target = BugReports.getInstance().getServer().getPlayer(report.getPlayerUUID());
                if (target != null) {
                    target.sendMessage(MessageManager.getMessage("report-status-changed",
                        "%id%", String.valueOf(report.getId()),
                        "%status%", newStatus));
                }
                count++;
            }
        }

        player.sendMessage(MessageManager.getMessage("reports-status-updated", 
            "%count%", String.valueOf(count)));
        player.closeInventory();
    }

    private Material getStatusMaterial(String status) {
        return switch (status) {
            case "Новый" -> Material.RED_CONCRETE;
            case "В обработке" -> Material.YELLOW_CONCRETE;
            case "Завершено" -> Material.GREEN_CONCRETE;
            case "Отклонено" -> Material.GRAY_CONCRETE;
            default -> Material.WHITE_CONCRETE;
        };
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "Новый" -> "&c";
            case "В обработке" -> "&e";
            case "Завершено" -> "&a";
            case "Отклонено" -> "&7";
            default -> "&f";
        };
    }
} 