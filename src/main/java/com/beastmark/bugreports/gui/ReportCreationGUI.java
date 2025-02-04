package com.beastmark.bugreports.gui;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.ReportType;
import com.beastmark.bugreports.utils.GuiUtils;
import com.beastmark.bugreports.utils.MessageManager;
import com.beastmark.bugreports.utils.ReportCreationManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ReportCreationGUI extends GUI {
    private final ReportType type;
    private final List<String> categories;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 21; // Оставляем место для навигации

    public ReportCreationGUI(Player player, ReportType type) {
        super(player, type == null ? "Выберите тип репорта" : 
            (type == ReportType.BUG ? "Создание баг-репорта" : "Создание предложения"), 54); // Увеличиваем размер до 54
        this.type = type;
        this.categories = BugReports.getInstance().getConfig().getStringList(
            "categories." + (type == ReportType.BUG ? "bugs" : "suggestions"));
        init();
    }

    @Override
    public void init() {
        if (type == null) {
            // Меню выбора типа
            inventory.setItem(20, GuiUtils.createItem(
                Material.REDSTONE,
                "&cСообщить о баге",
                "",
                "&7Нажмите, чтобы сообщить",
                "&7об ошибке или баге"
            ));

            inventory.setItem(24, GuiUtils.createItem(
                Material.EMERALD,
                "&aПредложить идею",
                "",
                "&7Нажмите, чтобы предложить",
                "&7улучшение или новую функцию"
            ));
        } else {
            // Заполняем фон
            ItemStack background = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, background);
            }

            // Информация
            inventory.setItem(4, GuiUtils.createItem(
                Material.BOOK,
                "&6Создание " + (type == ReportType.BUG ? "баг-репорта" : "предложения"),
                "",
                "&7Выберите категорию для",
                "&7вашего " + (type == ReportType.BUG ? "баг-репорта" : "предложения"),
                "",
                "&7Страница &f" + (currentPage + 1) + "&7/&f" + 
                    ((categories.size() - 1) / ITEMS_PER_PAGE + 1)
            ));

            Material categoryMaterial = type == ReportType.BUG ? Material.REDSTONE : Material.EMERALD;
            
            // Отображаем категории для текущей страницы
            int startIndex = currentPage * ITEMS_PER_PAGE;
            for (int i = 0; i < ITEMS_PER_PAGE && startIndex + i < categories.size(); i++) {
                String category = categories.get(startIndex + i);
                inventory.setItem(10 + i + (i / 7) * 2, GuiUtils.createItem(
                    categoryMaterial,
                    "&6" + category,
                    "",
                    "&7Нажмите, чтобы выбрать",
                    "&7эту категорию"
                ));
            }

            // Кнопки навигации
            if (currentPage > 0) {
                inventory.setItem(45, GuiUtils.createItem(
                    Material.ARROW,
                    "&6Предыдущая страница",
                    "&7Страница " + currentPage + " из " + 
                        ((categories.size() - 1) / ITEMS_PER_PAGE + 1)
                ));
            }
            
            if ((currentPage + 1) * ITEMS_PER_PAGE < categories.size()) {
                inventory.setItem(53, GuiUtils.createItem(
                    Material.ARROW,
                    "&6Следующая страница",
                    "&7Страница " + (currentPage + 2) + " из " + 
                        ((categories.size() - 1) / ITEMS_PER_PAGE + 1)
                ));
            }
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        if (type == null) {
            if (event.getSlot() == 20) {
                new ReportCreationGUI(player, ReportType.BUG).open();
            } else if (event.getSlot() == 24) {
                new ReportCreationGUI(player, ReportType.SUGGESTION).open();
            }
        } else {
            int slot = event.getSlot();
            
            if (slot == 45 && currentPage > 0) {
                currentPage--;
                init();
                return;
            }
            
            if (slot == 53 && (currentPage + 1) * ITEMS_PER_PAGE < categories.size()) {
                currentPage++;
                init();
                return;
            }
            
            // Проверяем, является ли слот слотом категории
            if (slot >= 10 && slot <= 43) {
                int row = (slot - 10) / 9;
                int col = (slot - 10) % 9;
                if (col < 7) { // Проверяем, что клик был в пределах допустимой области
                    int index = currentPage * ITEMS_PER_PAGE + row * 7 + col;
                    if (index < categories.size()) {
                        String category = categories.get(index);
                        player.closeInventory();
                        ReportCreationManager.setPlayerState(player, type, category);
                        player.sendMessage(MessageManager.getMessage("enter-description"));
                    }
                }
            }
        }
    }
} 