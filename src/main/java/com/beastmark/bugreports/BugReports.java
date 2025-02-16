package com.beastmark.bugreports;

import com.beastmark.bugreports.database.DatabaseManager;
import com.beastmark.bugreports.database.SQLiteManager;
import com.beastmark.bugreports.utils.MessageManager;
import com.beastmark.bugreports.placeholders.BugReportsExpansion;
import com.beastmark.bugreports.commands.BugReportCommand;
import com.beastmark.bugreports.commands.SuggestionCommand;
import com.beastmark.bugreports.commands.ReportAdminCommand;
import com.beastmark.bugreports.commands.ReportAdminTabCompleter;
import com.beastmark.bugreports.commands.ReportStatsCommand;
import com.beastmark.bugreports.commands.TelegramCommand;
import com.beastmark.bugreports.listeners.GUIListener;
import com.beastmark.bugreports.listeners.ChatListener;
import com.beastmark.bugreports.listeners.PlayerListener;
import com.beastmark.bugreports.listeners.AdminChatListener;
import com.beastmark.bugreports.utils.NotificationManager;
import com.beastmark.bugreports.utils.UpdateChecker;
import com.beastmark.bugreports.telegram.TelegramManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BugReports extends JavaPlugin {
    private static BugReports instance;
    private DatabaseManager databaseManager;
    private NotificationManager notificationManager;
    private TelegramManager telegramManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Создаем бэкапы и загружаем конфигурацию
        createConfigBackups();
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        // Инициализация менеджеров
        MessageManager.load();
        initDatabase();
        notificationManager = new NotificationManager(this);
        
        // Инициализация Telegram бота
        if (getConfig().getBoolean("telegram.enabled", false)) {
            telegramManager = new TelegramManager(this);
        }
        
        // Регистрация команд и слушателей
        registerCommands();
        registerListeners();
        
        // Проверка обновлений
        if (getConfig().getBoolean("updates.enabled", true)) {
            new UpdateChecker(this);
        }
        
        // Регистрация PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BugReportsExpansion(this).register();
        }
        
        getLogger().info("BugReports успешно загружен!");
    }
    
    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("BugReports выключен!");
    }
    
    private void initDatabase() {
        databaseManager = new SQLiteManager(this);
        databaseManager.init();
    }
    
    private void registerCommands() {
        ReportAdminCommand adminCommand = new ReportAdminCommand(this);
        getCommand("reportadmin").setExecutor(adminCommand);
        getCommand("reportadmin").setTabCompleter(new ReportAdminTabCompleter(this));
        
        getCommand("bugreport").setExecutor(new BugReportCommand(this));
        getCommand("suggestion").setExecutor(new SuggestionCommand(this));
        getCommand("reportstats").setExecutor(new ReportStatsCommand(this));
        getCommand("telegram").setExecutor(new TelegramCommand(this));
    }
    
    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new GUIListener(), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new AdminChatListener(), this);
    }
    
    private void createConfigBackups() {
        try {
            createBackup("config.yml");
            createBackup("messages.yml");
        } catch (IOException e) {
            getLogger().warning("Не удалось создать бэкап файлов конфигурации: " + e.getMessage());
        }
    }
    
    private void createBackup(String fileName) throws IOException {
        File configFile = new File(getDataFolder(), fileName);
        if (configFile.exists()) {
            File backupDir = new File(getDataFolder(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            File backupFile = new File(backupDir, fileName.replace(".yml", "") + "_" + timestamp + ".yml");
            
            java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            getLogger().info("Создан бэкап файла " + fileName + ": " + backupFile.getName());
            
            // Удаляем старые бэкапы (оставляем только 5 последних)
            File[] backups = backupDir.listFiles((dir, name) -> name.startsWith(fileName.replace(".yml", "")));
            if (backups != null && backups.length > 5) {
                java.util.Arrays.sort(backups, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                for (int i = 5; i < backups.length; i++) {
                    backups[i].delete();
                }
            }
        }
    }
    
    public static BugReports getInstance() {
        return instance;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
    
    public TelegramManager getTelegramManager() {
        return telegramManager;
    }
} 