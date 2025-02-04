package com.beastmark.bugreports.listeners;

import com.beastmark.bugreports.utils.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdminChatListener implements Listener {
    private static final Map<UUID, Player> playersInMessageMode = new HashMap<>();

    public static void addPlayerInMessageMode(Player admin, Player target) {
        playersInMessageMode.put(admin.getUniqueId(), target);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player admin = event.getPlayer();
        Player target = playersInMessageMode.get(admin.getUniqueId());

        if (target != null) {
            event.setCancelled(true);
            String message = event.getMessage();

            if (message.equalsIgnoreCase("cancel")) {
                playersInMessageMode.remove(admin.getUniqueId());
                admin.sendMessage(MessageManager.getMessage("report-cancelled"));
                return;
            }

            // Отправляем сообщение игроку
            String prefix = MessageManager.getRawMessage("prefix");
            String coloredMessage = ChatColor.translateAlternateColorCodes('&', "&f" + message);
            target.sendMessage(prefix + " " + coloredMessage);
            
            admin.sendMessage(MessageManager.getMessage("message-sent", 
                "%player%", target.getName()));

            // Удаляем админа из режима сообщения
            playersInMessageMode.remove(admin.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Удаляем игрока из режима сообщения при выходе
        playersInMessageMode.remove(event.getPlayer().getUniqueId());
    }
} 