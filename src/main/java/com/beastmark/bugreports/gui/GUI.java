package com.beastmark.bugreports.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.Event.Result;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class GUI implements InventoryHolder {
    protected final Player player;
    protected final Inventory inventory;
    protected final String title;

    public GUI(Player player, String title, int size) {
        this.player = player;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public abstract void init();

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void handleClick(InventoryClickEvent event);

    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
        event.setResult(Result.DENY);
    }

    protected void preventItemMovement(InventoryClickEvent event) {
        event.setCancelled(true);
        event.setResult(Result.DENY);
        
        // Отменяем любые манипуляции с предметами
        if (event.getClickedInventory() != null) {
            event.setCurrentItem(event.getCurrentItem());
        }
        
        // Отменяем все возможные действия с инвентарем
        switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY:
            case HOTBAR_MOVE_AND_READD:
            case HOTBAR_SWAP:
            case SWAP_WITH_CURSOR:
            case COLLECT_TO_CURSOR:
                event.setCancelled(true);
                event.setResult(Result.DENY);
                break;
        }
    }
} 