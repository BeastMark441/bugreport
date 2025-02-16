package com.beastmark.bugreports.gui;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.Report;
import com.beastmark.bugreports.model.ReportType;
import com.beastmark.bugreports.utils.GuiUtils;
import com.beastmark.bugreports.utils.MessageManager;
import com.beastmark.bugreports.listeners.AdminChatListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdminGUI extends GUI {
    private final List<Report> reports;
    private int currentPage = 0;
    private String currentFilter = "Все";
    private boolean sortAscending = true;

    public AdminGUI(Player player) {
        super(player, "Управление репортами", 54);
        this.reports = new ArrayList<>();
        updateReports();
        init();
    }

    private void updateReports() {
        reports.clear();
        reports.addAll(BugReports.getInstance().getDatabaseManager().getAllReports());
    }

    @Override
    public void init() {
        inventory.clear();
        List<Report> filteredReports = filterAndSortReports();

        // Заполняем фон
        ItemStack background = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // Отображаем репорты
        int startIndex = currentPage * 45;
        for (int i = 0; i < 45 && startIndex + i < filteredReports.size(); i++) {
            Report report = filteredReports.get(startIndex + i);
            Material material = getReportMaterial(report);
            
            inventory.setItem(i, GuiUtils.createItem(
                material,
                getReportColor(report.getStatus()) + "Репорт #" + report.getId(),
                "&7Игрок: &f" + report.getPlayerName(),
                "&7Категория: &f" + report.getCategory(),
                "&7Статус: " + getStatusColor(report.getStatus()) + report.getStatus(),
                "&7Тип: &f" + (report.getType().name().equals("BUG") ? "Баг" : "Предложение"),
                "",
                "&7Описание:",
                "&f" + formatDescription(report.getDescription()),
                "",
                "&eЛКМ &7- Изменить статус",
                "&eПКМ &7- Удалить",
                "&eShift+ЛКМ &7- Телепортироваться к игроку",
                "&eShift+ПКМ &7- Написать игроку"
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

        // Фильтр и сортировка
        inventory.setItem(46, GuiUtils.createItem(
            Material.HOPPER,
            "&6Фильтр: &f" + currentFilter,
            "",
            "&7Текущий фильтр: &f" + currentFilter,
            "",
            "&eЛКМ &7- Следующий фильтр",
            "&eПКМ &7- Предыдущий фильтр"
        ));

        inventory.setItem(47, GuiUtils.createItem(
            Material.COMPARATOR,
            "&6Сортировка: &f" + (sortAscending ? "По возрастанию" : "По убыванию"),
            "",
            "&eЛКМ &7- По ID " + (sortAscending ? "▲" : "▼"),
            "&eПКМ &7- По дате " + (sortAscending ? "▲" : "▼")
        ));

        // Статистика
        inventory.setItem(49, GuiUtils.createItem(
            Material.BOOK,
            "&6Статистика",
            "&7Всего репортов: &f" + filteredReports.size(),
            "&aНовых: &f" + countReportsByStatus(filteredReports, "Новый"),
            "&eВ обработке: &f" + countReportsByStatus(filteredReports, "В обработке"),
            "&bЗавершено: &f" + countReportsByStatus(filteredReports, "Завершено"),
            "&cОтклонено: &f" + countReportsByStatus(filteredReports, "Отклонено"),
            "",
            "&7Баги: &f" + countReportsByType(filteredReports, ReportType.BUG),
            "&7Предложения: &f" + countReportsByType(filteredReports, ReportType.SUGGESTION)
        ));

        // Массовые действия
        inventory.setItem(51, GuiUtils.createItem(
            Material.ANVIL,
            "&6Массовые действия",
            "",
            "&eЛКМ &7- Изменить статус всех",
            "&eПКМ &7- Удалить все с текущим фильтром",
            "",
            "&7Выбрано: &f" + filteredReports.size() + " репортов"
        ));

        // Кнопка очистки
        inventory.setItem(52, GuiUtils.createItem(
            Material.BARRIER,
            "&cОчистка репортов",
            "",
            "&7Удалить все репорты со статусом",
            "&7'Завершено' или 'Отклонено'",
            "",
            "&eЛКМ &7- Удалить завершенные",
            "&eShift+ЛКМ &7- Удалить все"
        ));
    }

    private String formatDescription(String description) {
        if (description.length() > 30) {
            return description.substring(0, 27) + "...";
        }
        return description;
    }

    private Material getReportMaterial(Report report) {
        return switch (report.getStatus()) {
            case "Новый" -> report.getType() == ReportType.BUG ? Material.RED_CONCRETE : Material.LIME_CONCRETE;
            case "В обработке" -> Material.YELLOW_CONCRETE;
            case "Завершено" -> Material.GREEN_CONCRETE;
            case "Отклонено" -> Material.GRAY_CONCRETE;
            default -> Material.WHITE_CONCRETE;
        };
    }

    private String getReportColor(String status) {
        return switch (status) {
            case "Новый" -> "&c";
            case "В обработке" -> "&e";
            case "Завершено" -> "&a";
            case "Отклонено" -> "&7";
            default -> "&f";
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

    private long countReportsByStatus(List<Report> reports, String status) {
        return reports.stream().filter(r -> r.getStatus().equals(status)).count();
    }

    private long countReportsByType(List<Report> reports, ReportType type) {
        return reports.stream().filter(r -> r.getType() == type).count();
    }

    private List<Report> filterAndSortReports() {
        List<Report> filtered = new ArrayList<>(reports);
        
        // Применяем фильтр
        if (!currentFilter.equals("Все")) {
            filtered.removeIf(r -> !r.getStatus().equals(currentFilter));
        }
        
        // Сортируем
        filtered.sort((r1, r2) -> {
            int compare = Integer.compare(r1.getId(), r2.getId());
            return sortAscending ? compare : -compare;
        });
        
        return filtered;
    }

    private List<String> getAvailableStatuses() {
        List<String> statuses = new ArrayList<>();
        statuses.add("Все");
        statuses.addAll(BugReports.getInstance().getConfig().getStringList("statuses"));
        return statuses;
    }

    private String formatStatusList(List<String> statuses) {
        return String.join(", ", statuses);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // Применяем базовую защиту от перемещения предметов
        preventItemMovement(event);
        
        int slot = event.getSlot();

        if (slot == 46) { // Фильтр
            List<String> statuses = getAvailableStatuses();
            int currentIndex = statuses.indexOf(currentFilter);
            
            if (event.isLeftClick()) {
                currentFilter = statuses.get((currentIndex + 1) % statuses.size());
            } else if (event.isRightClick()) {
                currentFilter = statuses.get((currentIndex - 1 + statuses.size()) % statuses.size());
            }
            player.sendMessage(MessageManager.getMessage("invalid-status", 
                "%statuses%", formatStatusList(BugReports.getInstance().getConfig().getStringList("statuses"))));
            init();
        }
        else if (slot == 47) { // Сортировка
            if (event.isLeftClick()) {
                sortAscending = !sortAscending;
            }
            init();
        }
        else if (slot == 51) { // Массовое действие
            List<Report> filtered = filterAndSortReports();
            if (!filtered.isEmpty()) {
                if (event.isLeftClick()) {
                    new StatusChangeGUI(player, filtered).open();
                } else if (event.isRightClick() && event.isShiftClick()) {
                    for (Report report : filtered) {
                        BugReports.getInstance().getDatabaseManager().deleteReport(report.getId());
                    }
                    reports.removeAll(filtered);
                    player.sendMessage(MessageManager.getMessage("reports-deleted", 
                        "%count%", String.valueOf(filtered.size())));
                    init();
                }
            } else {
                player.sendMessage(MessageManager.getMessage("no-reports-filtered"));
            }
        }
        else if (slot == 52) { // Очистка
            if (event.isShiftClick()) {
                int count = reports.size();
                BugReports.getInstance().getDatabaseManager().deleteAllReports();
                reports.clear(); // Очищаем существующий список
                player.sendMessage(MessageManager.getMessage("reports-deleted", 
                    "%count%", String.valueOf(count)));
                init();
            } else {
                List<Report> toDelete = reports.stream()
                    .filter(r -> r.getStatus().equals("Завершено") || r.getStatus().equals("Отклонено"))
                    .collect(Collectors.toList());
                    
                for (Report report : toDelete) {
                    BugReports.getInstance().getDatabaseManager().deleteReport(report.getId());
                }
                
                updateReports(); // Обновляем список после массового удаления
                player.sendMessage(MessageManager.getMessage("reports-deleted", 
                    "%count%", String.valueOf(toDelete.size())));
                init();
            }
        }
        else if (slot < 45 && slot >= 0) {
            int reportIndex = currentPage * 45 + slot;
            List<Report> filteredReports = filterAndSortReports();
            if (reportIndex < filteredReports.size()) {
                Report report = filteredReports.get(reportIndex);
                if (event.isLeftClick()) {
                    if (event.isShiftClick()) {
                        // Телепортация к игроку
                        Player target = Bukkit.getPlayer(report.getPlayerUUID());
                        if (target != null) {
                            player.teleport(target);
                            player.sendMessage(MessageManager.getMessage("teleported-to-player", 
                                "%player%", target.getName()));
                        } else {
                            player.sendMessage(MessageManager.getMessage("player-offline"));
                        }
                    } else {
                        List<Report> singleReport = Arrays.asList(report);
                        new StatusChangeGUI(player, singleReport).open();
                    }
                } else if (event.isRightClick()) {
                    if (event.isShiftClick()) {
                        // Написать игроку
                        Player target = Bukkit.getPlayer(report.getPlayerUUID());
                        if (target != null) {
                            player.closeInventory();
                            player.sendMessage(MessageManager.getMessage("enter-message-to-player",
                                "%player%", target.getName()));
                            // Добавляем игрока в режим ожидания сообщения
                            AdminChatListener.addPlayerInMessageMode(player, target);
                        } else {
                            player.sendMessage(MessageManager.getMessage("player-offline"));
                        }
                    } else {
                        BugReports.getInstance().getDatabaseManager().deleteReport(report.getId());
                        reports.remove(report);
                        init();
                    }
                }
            }
        } else if (slot == 45 && currentPage > 0) {
            currentPage--;
            init();
        } else if (slot == 53 && (currentPage + 1) * 45 < reports.size()) {
            currentPage++;
            init();
        }
    }
} 