package com.beastmark.bugreports.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiUtils {
    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(ChatColor.translateAlternateColorCodes('&', name)));
            
            List<net.kyori.adventure.text.Component> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(net.kyori.adventure.text.Component.text(ChatColor.translateAlternateColorCodes('&', line)));
            }
            meta.lore(loreList);
            
            item.setItemMeta(meta);
        }
        return item;
    }
} 