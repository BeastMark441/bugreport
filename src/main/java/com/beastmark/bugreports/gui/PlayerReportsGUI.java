package com.beastmark.bugreports.gui;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.Report;
import com.beastmark.bugreports.utils.GuiUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerReportsGUI extends GUI {
    private final List<Report> reports;
    private int currentPage = 0;
    private String currentFilter = "Все";

    public PlayerReportsGUI(Player player) {
        super(player, "Ваши репорты", 54);
        this.reports = BugReports.getInstance().getDatabaseManager().getPlayerReports(player.getUniqueId());
        init();
    }

    @Override
    public void init() {
        inventory.clear();
        List<Report> filteredReports = filterReports();

        // Заполняем фон
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Отображаем репорты
        int startIndex = currentPage * 45;
        for (int i = 0; i < 45 && startIndex + i < filteredReports.size(); i++) {
            Report report = filteredReports.get(startIndex + i);
            Material material = getMaterialByStatus(report.getStatus());
            
            inventory.setItem(i, GuiUtils.createItem(
                material,
                "&6Репорт #" + report.getId(),
                "&7Тип: &f" + (report.getType().name().equals("BUG") ? "Баг" : "Предложение"),
                "&7Категория: &f" + report.getCategory(),
                "&7Статус: &f" + report.getStatus(),
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
                "&7Страница " + currentPage + " из " + (filteredReports.size() / 45 + 1)
            ));
        }
        
        if ((currentPage + 1) * 45 < filteredReports.size()) {
            inventory.setItem(53, GuiUtils.createItem(
                Material.ARROW,
                "&6Следующая страница",
                "&7Страница " + (currentPage + 1) + " из " + (filteredReports.size() / 45 + 1)
            ));
        }

        // Фильтр
        inventory.setItem(47, GuiUtils.createItem(
            Material.HOPPER,
            "&6Фильтр: &f" + currentFilter,
            "",
            "&7Нажмите для переключения",
            "&7между статусами"
        ));

        // Статистика
        inventory.setItem(49, GuiUtils.createItem(
            Material.BOOK,
            "&6Ваша статистика",
            "&7Всего репортов: &f" + reports.size(),
            "&7Новых: &f" + reports.stream().filter(r -> r.getStatus().equals("Новый")).count(),
            "&7В обработке: &f" + reports.stream().filter(r -> r.getStatus().equals("В обработке")).count(),
            "&7Завершено: &f" + reports.stream().filter(r -> r.getStatus().equals("Завершено")).count()
        ));

        // Кнопка создания нового репорта
        inventory.setItem(51, GuiUtils.createItem(
            Material.WRITABLE_BOOK,
            "&aСоздать новый репорт",
            "",
            "&7Нажмите, чтобы создать",
            "&7новый репорт"
        ));
    }

    private Material getMaterialByStatus(String status) {
        return switch (status) {
            case "Новый" -> Material.LIME_DYE;
            case "В обработке" -> Material.YELLOW_DYE;
            case "Завершено" -> Material.GREEN_DYE;
            case "Отклонено" -> Material.RED_DYE;
            default -> Material.GRAY_DYE;
        };
    }

    private List<Report> filterReports() {
        if (currentFilter.equals("Все")) {
            return new ArrayList<>(reports);
        }
        return reports.stream()
            .filter(r -> r.getStatus().equals(currentFilter))
            .collect(Collectors.toList());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == 47) { // Фильтр
            List<String> statuses = new ArrayList<>();
            statuses.add("Все");
            statuses.addAll(BugReports.getInstance().getConfig().getStringList("statuses"));
            
            int currentIndex = statuses.indexOf(currentFilter);
            currentFilter = statuses.get((currentIndex + 1) % statuses.size());
            init();
        }
        else if (slot == 51) { // Создать новый
            player.closeInventory();
            new ReportCreationGUI(player, null).open();
        }
        else if (slot == 45 && currentPage > 0) {
            currentPage--;
            init();
        }
        else if (slot == 53 && (currentPage + 1) * 45 < filterReports().size()) {
            currentPage++;
            init();
        }
    }
} 