package com.beastmark.bugreports.gui;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.Report;
import com.beastmark.bugreports.utils.GuiUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class ReportStatusGUI extends GUI {
    private final List<Report> reports;
    private int currentPage = 0;

    public ReportStatusGUI(Player player) {
        super(player, "Ваши репорты", 54);
        this.reports = BugReports.getInstance().getDatabaseManager().getPlayerReports(player.getUniqueId());
        init();
    }

    @Override
    public void init() {
        inventory.clear();

        // Заполняем фон
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Отображаем репорты
        int startIndex = currentPage * 45;
        for (int i = 0; i < 45 && startIndex + i < reports.size(); i++) {
            Report report = reports.get(startIndex + i);
            Material material = report.getType().name().equals("BUG") ? Material.REDSTONE : Material.EMERALD;
            
            inventory.setItem(i, GuiUtils.createItem(
                material,
                "&6Репорт #" + report.getId(),
                "&7Категория: &f" + report.getCategory(),
                "&7Статус: &f" + report.getStatus(),
                "&7Тип: &f" + (report.getType().name().equals("BUG") ? "Баг" : "Предложение"),
                "",
                "&7Описание:",
                "&f" + report.getDescription()
            ));
        }

        // Кнопки навигации
        if (currentPage > 0) {
            inventory.setItem(45, GuiUtils.createItem(
                Material.ARROW,
                "&6Предыдущая страница",
                "&7Страница " + currentPage + " из " + (reports.size() / 45 + 1)
            ));
        }
        
        if ((currentPage + 1) * 45 < reports.size()) {
            inventory.setItem(53, GuiUtils.createItem(
                Material.ARROW,
                "&6Следующая страница",
                "&7Страница " + (currentPage + 1) + " из " + (reports.size() / 45 + 1)
            ));
        }

        // Статистика
        inventory.setItem(49, GuiUtils.createItem(
            Material.BOOK,
            "&6Ваша статистика",
            "&7Всего репортов: &f" + reports.size(),
            "&7Новых: &f" + reports.stream().filter(r -> r.getStatus().equals("Новый")).count(),
            "&7В обработке: &f" + reports.stream().filter(r -> r.getStatus().equals("В обработке")).count(),
            "&7Завершено: &f" + reports.stream().filter(r -> r.getStatus().equals("Завершено")).count()
        ));

        // Добавим кнопку обновления
        inventory.setItem(50, GuiUtils.createItem(
            Material.COMPASS,
            "&6Обновить",
            "",
            "&7Нажмите, чтобы обновить",
            "&7список репортов"
        ));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getSlot();
        if (slot == 45 && currentPage > 0) {
            currentPage--;
            init();
        } else if (slot == 53 && (currentPage + 1) * 45 < reports.size()) {
            currentPage++;
            init();
        } else if (slot == 50) {
            // Обновляем список репортов
            reports.clear();
            reports.addAll(BugReports.getInstance().getDatabaseManager()
                .getPlayerReports(player.getUniqueId()));
            init();
        }
    }
} 