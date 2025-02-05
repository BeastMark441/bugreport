package com.beastmark.bugreports.telegram;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.Report;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.*;
import java.util.logging.Level;

public class TelegramManager extends TelegramLongPollingBot {
    private final BugReports plugin;
    private final Map<String, UUID> verificationCodes = new HashMap<>();
    private final Map<Long, AdminSession> adminSessions = new HashMap<>();

    private static class AdminSession {
        UUID minecraftUUID;
        String currentAction;
        Integer selectedReportId;

        AdminSession(UUID minecraftUUID) {
            this.minecraftUUID = minecraftUUID;
        }
    }

    public TelegramManager(BugReports plugin) {
        super(plugin.getConfig().getString("telegram.bot-token", ""));
        this.plugin = plugin;
        
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            plugin.getLogger().info("Telegram бот успешно запущен!");
        } catch (TelegramApiException e) {
            plugin.getLogger().severe("Ошибка при запуске Telegram бота: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return plugin.getConfig().getString("telegram.bot-username", "BugReportsBot");
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String message = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                if (message.startsWith("/start")) {
                    // Проверяем, не привязан ли уже этот чат
                    if (isAdminAuthorized(chatId)) {
                        String uuid = plugin.getConfig().getString("telegram.linked-accounts." + chatId);
                        // Создаем сессию для авторизованного пользователя
                        if (uuid != null) {
                            adminSessions.put(chatId, new AdminSession(UUID.fromString(uuid)));
                        }
                        sendMessage(chatId, "Ваш Telegram аккаунт уже привязан!\n" +
                            "Используйте следующие команды для управления:\n" +
                            "📋 /reports - Список репортов\n" +
                            "📊 /menu - Главное меню\n" +
                            "❓ /help - Помощь");
                    } else {
                        sendMessage(chatId, "Добро пожаловать в BugReports Bot!\n" +
                            "Для привязки аккаунта используйте команду /link <код>\n" +
                            "Код можно получить в игре, написав /reportadmin telegram");
                    }
                    return;
                }

                if (message.startsWith("/link")) {
                    handleLinkCommand(chatId, message);
                    return;
                }

                // Проверяем, авторизован ли администратор
                if (!isAdminAuthorized(chatId)) {
                    sendMessage(chatId, "Вы не авторизованы. Используйте /link <код> для привязки аккаунта.");
                    return;
                }

                // Создаем сессию, если её нет
                if (!adminSessions.containsKey(chatId)) {
                    String uuid = plugin.getConfig().getString("telegram.linked-accounts." + chatId);
                    if (uuid != null) {
                        adminSessions.put(chatId, new AdminSession(UUID.fromString(uuid)));
                    }
                }

                handleAdminCommand(chatId, message);
            }
            
            // Обработка нажатий на инлайн кнопки
            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при обработке сообщения в Telegram", e);
        }
    }

    private void handleLinkCommand(long chatId, String message) {
        String[] args = message.split(" ");
        if (args.length != 2) {
            sendMessage(chatId, "Используйте: /link <код>");
            return;
        }

        String code = args[1];
        UUID playerUUID = verificationCodes.get(code);
        if (playerUUID == null) {
            sendMessage(chatId, "Неверный код или срок его действия истек.");
            return;
        }

        // Сохраняем привязку в конфигурации
        plugin.getConfig().set("telegram.linked-accounts." + chatId, playerUUID.toString());
        plugin.saveConfig();
        verificationCodes.remove(code);

        adminSessions.put(chatId, new AdminSession(playerUUID));
        sendMessage(chatId, "Аккаунт успешно привязан! Теперь вы будете получать уведомления о новых репортах.");
        sendMainMenu(chatId);
    }

    private void handleAdminCommand(long chatId, String message) {
        AdminSession session = adminSessions.get(chatId);
        if (session == null) {
            plugin.getLogger().warning("Сессия не найдена для chatId: " + chatId);
            return;
        }

        if (session.currentAction != null) {
            switch (session.currentAction) {
                case "WAITING_STATUS":
                    if (session.selectedReportId != null) {
                        updateReportStatus(chatId, session.selectedReportId, message);
                        session.currentAction = null;
                        session.selectedReportId = null;
                    }
                    break;
                case "WAITING_MESSAGE":
                    if (session.selectedReportId != null) {
                        sendMessageToReportAuthor(chatId, session.selectedReportId, message);
                        session.currentAction = null;
                        session.selectedReportId = null;
                    }
                    break;
            }
            return;
        }

        plugin.getLogger().info("Обработка команды: " + message + " для chatId: " + chatId);

        switch (message.toLowerCase()) {
            case "/reports":
            case "репорты":
                plugin.getLogger().info("Отправка списка репортов для chatId: " + chatId);
                sendReportsList(chatId);
                break;
            case "/menu":
            case "меню":
                plugin.getLogger().info("Отправка главного меню для chatId: " + chatId);
                sendMainMenu(chatId);
                break;
            case "/help":
                plugin.getLogger().info("Отправка справки для chatId: " + chatId);
                sendHelpMessage(chatId);
                break;
            default:
                plugin.getLogger().warning("Неизвестная команда: " + message + " от chatId: " + chatId);
                sendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра списка команд.");
                break;
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        AdminSession session = adminSessions.get(chatId);
        
        if (session == null) return;

        String[] data = callbackData.split(":");
        String action = data[0];
        
        plugin.getLogger().info("Получен callback: " + callbackData + " от chatId: " + chatId);
        
        switch (action) {
            case "view":
                int reportId = Integer.parseInt(data[1]);
                sendReportDetails(chatId, reportId);
                break;
            case "status":
                session.currentAction = "WAITING_STATUS";
                session.selectedReportId = Integer.parseInt(data[1]);
                sendAvailableStatuses(chatId);
                break;
            case "message":
                session.currentAction = "WAITING_MESSAGE";
                session.selectedReportId = Integer.parseInt(data[1]);
                sendMessage(chatId, "Введите сообщение для автора репорта:");
                break;
            case "delete":
                int deleteReportId = Integer.parseInt(data[1]);
                deleteReport(chatId, deleteReportId);
                break;
            case "block":
                UUID playerUUID = UUID.fromString(data[1]);
                blockPlayer(chatId, playerUUID);
                break;
            case "back":
                sendMainMenu(chatId);
                break;
            case "reports":
                if (data.length > 1) {
                    switch (data[1]) {
                        case "all":
                            sendReportsList(chatId);
                            break;
                        case "new":
                            sendNewReportsList(chatId);
                            break;
                    }
                }
                break;
            case "settings":
                if (data.length > 1) {
                    switch (data[1]) {
                        case "security":
                            sendSecuritySettings(chatId);
                            break;
                        case "blacklist":
                            sendBlacklistMenu(chatId);
                            break;
                        default:
                            sendSettingsMenu(chatId);
                            break;
                    }
                } else {
                    sendSettingsMenu(chatId);
                }
                break;
            case "setstatus":
                if (session.selectedReportId != null && data.length > 1) {
                    updateReportStatus(chatId, session.selectedReportId, data[1]);
                    session.currentAction = null;
                    session.selectedReportId = null;
                }
                break;
        }
        
        // Отвечаем на callback
        try {
            plugin.getLogger().info("Отвечаем на callback");
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(update.getCallbackQuery().getId());
            execute(answer);
        } catch (TelegramApiException e) {
            plugin.getLogger().severe("Ошибка при ответе на callback: " + e.getMessage());
        }
    }

    private void sendMainMenu(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Первый ряд кнопок
        keyboard.add(Arrays.asList(
            createInlineButton("📋 Все репорты", "reports:all"),
            createInlineButton("🆕 Новые", "reports:new")
        ));

        // Второй ряд кнопок
        keyboard.add(Arrays.asList(
            createInlineButton("⚙️ Настройки", "settings"),
            createInlineButton("❓ Помощь", "help")
        ));

        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Главное меню BugReports");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке главного меню", e);
        }
    }

    private void sendReportsList(long chatId) {
        List<Report> reports = plugin.getDatabaseManager().getAllReports();
        if (reports.isEmpty()) {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(Arrays.asList(createInlineButton("◀️ Вернуться в меню", "back")));
            markup.setKeyboard(keyboard);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("📝 *Список репортов пуст*\n\nНовых репортов пока нет.");
            message.setParseMode("Markdown");
            message.setReplyMarkup(markup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке сообщения об отсутствии репортов", e);
            }
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Report report : reports) {
            String status = report.getStatus();
            String emoji = getStatusEmoji(status);
            String buttonText = String.format("%s #%d - %s", emoji, report.getId(), 
                truncateString(report.getDescription(), 30));
            
            keyboard.add(Arrays.asList(createInlineButton(buttonText, "view:" + report.getId())));
        }

        keyboard.add(Arrays.asList(createInlineButton("◀️ Назад", "back")));
        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Список репортов:");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке списка репортов", e);
        }
    }

    private void sendReportDetails(long chatId, int reportId) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report == null) {
            sendMessage(chatId, "Репорт не найден.");
            return;
        }

        String emoji = getStatusEmoji(report.getStatus());
        StringBuilder message = new StringBuilder();
        message.append(String.format("📝 *Репорт #%d*\n", report.getId()));
        message.append(String.format("👤 *Игрок:* %s\n", report.getPlayerName()));
        message.append(String.format("📂 *Категория:* %s\n", report.getCategory()));
        message.append(String.format("🏷 *Статус:* %s %s\n", emoji, report.getStatus()));
        message.append(String.format("📅 *Создан:* %s\n", report.getCreatedAt()));
        message.append(String.format("📌 *Тип:* %s\n\n", 
            report.getType().name().equals("BUG") ? "Баг" : "Предложение"));
        message.append("📄 *Описание:*\n");
        message.append(report.getDescription());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Кнопки действий
        keyboard.add(Arrays.asList(
            createInlineButton("📝 Изменить статус", "status:" + reportId),
            createInlineButton("✉️ Написать игроку", "message:" + reportId)
        ));

        // Кнопки управления игроком и репортом
        keyboard.add(Arrays.asList(
            createInlineButton("🚫 Заблокировать игрока", "block:" + report.getPlayerUUID()),
            createInlineButton("❌ Удалить репорт", "delete:" + reportId)
        ));

        // Кнопка "Назад"
        keyboard.add(Arrays.asList(createInlineButton("◀️ Назад к списку", "back")));

        markup.setKeyboard(keyboard);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message.toString());
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке деталей репорта", e);
        }
    }

    private void sendAvailableStatuses(long chatId) {
        List<String> statuses = plugin.getConfig().getStringList("statuses");
        if (statuses.isEmpty()) {
            sendMessage(chatId, "❌ Ошибка: список статусов пуст");
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (String status : statuses) {
            keyboard.add(Arrays.asList(
                createInlineButton(getStatusEmoji(status) + " " + status, 
                    "setstatus:" + status)
            ));
        }

        keyboard.add(Arrays.asList(createInlineButton("◀️ Отмена", "back")));
        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите новый статус:");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке списка статусов", e);
        }
    }

    private String getStatusEmoji(String status) {
        // Получаем список статусов из конфига для определения порядка
        List<String> configStatuses = plugin.getConfig().getStringList("statuses");
        if (configStatuses.isEmpty()) return "📝";

        // Определяем позицию статуса
        int position = configStatuses.indexOf(status);
        if (position == -1) return "📝";
        
        // Первый статус обычно "Новый"
        if (position == 0) return "🆕";
        // Последний статус обычно "Завершено" или "Отклонено"
        if (position == configStatuses.size() - 1) return "✅";
        // Промежуточные статусы
        return "⚙️";
    }

    private void updateReportStatus(long chatId, int reportId, String newStatus) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report == null) {
            sendMessage(chatId, "Репорт не найден.");
            return;
        }

        plugin.getDatabaseManager().updateReportStatus(reportId, newStatus);
        
        // Добавляем уведомление для игрока
        plugin.getNotificationManager().addNotification(report.getPlayerUUID(), reportId);
        
        // Отправляем сообщение в Telegram
        sendMessage(chatId, String.format("Статус репорта #%d изменен на: %s", reportId, newStatus));
        sendReportDetails(chatId, reportId);
    }

    private void sendMessageToReportAuthor(long chatId, int reportId, String message) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report == null) {
            sendMessage(chatId, "Репорт не найден.");
            return;
        }

        // Сохраняем сообщение для игрока (будет показано при входе)
        plugin.getNotificationManager().addMessageNotification(report.getPlayerUUID(), message);
        
        // Пытаемся отправить сообщение сразу, если игрок онлайн
        plugin.getNotificationManager().sendMessageToPlayer(report.getPlayerUUID(), message);
        
        sendMessage(chatId, "Сообщение отправлено игроку " + report.getPlayerName());
        sendReportDetails(chatId, reportId);
    }

    public void notifyAdminsNewReport(Report report) {
        String message = String.format("""
            🆕 *Новый %s*
            📝 *ID:* #%d
            👤 *Игрок:* %s
            📂 *Категория:* %s
            
            📄 *Описание:*
            %s""",
            report.getType().name().equals("BUG") ? "баг-репорт" : "предложение",
            report.getId(),
            report.getPlayerName(),
            report.getCategory(),
            report.getDescription()
        );

        // Получаем секцию привязанных аккаунтов
        ConfigurationSection linkedAccounts = plugin.getConfig().getConfigurationSection("telegram.linked-accounts");
        if (linkedAccounts != null) {
            for (String chatId : linkedAccounts.getKeys(false)) {
                try {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(message);
                    sendMessage.setParseMode("Markdown");
                    
                    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                    keyboard.add(Arrays.asList(
                        createInlineButton("📝 Открыть репорт", "view:" + report.getId())
                    ));
                    markup.setKeyboard(keyboard);
                    sendMessage.setReplyMarkup(markup);
                    
                    execute(sendMessage);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, 
                        "Ошибка при отправке уведомления в Telegram для chatId: " + chatId, e);
                }
            }
        }
    }

    private InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private String truncateString(String str, int length) {
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

    private boolean isAdminAuthorized(long chatId) {
        return plugin.getConfig().contains("telegram.linked-accounts." + chatId);
    }

    public String generateVerificationCode(UUID playerUUID) {
        String code = UUID.randomUUID().toString().substring(0, 6);
        verificationCodes.put(code, playerUUID);
        
        // Удаляем код через настроенное время
        int timeout = plugin.getConfig().getInt("telegram.security.verification-code-timeout", 5);
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, 
            () -> verificationCodes.remove(code), 20 * 60 * timeout);
        
        return code;
    }

    private void sendMessage(long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке сообщения в Telegram", e);
        }
    }

    public boolean unlinkAccount(UUID playerUUID) {
        String chatIdToRemove = null;
        ConfigurationSection linkedAccounts = plugin.getConfig().getConfigurationSection("telegram.linked-accounts");
        
        if (linkedAccounts != null) {
            for (String chatId : linkedAccounts.getKeys(false)) {
                String uuid = linkedAccounts.getString(chatId);
                if (uuid != null && uuid.equals(playerUUID.toString())) {
                    chatIdToRemove = chatId;
                    break;
                }
            }
        }

        if (chatIdToRemove != null) {
            plugin.getConfig().set("telegram.linked-accounts." + chatIdToRemove, null);
            plugin.saveConfig();
            return true;
        }
        return false;
    }

    public boolean isPlayerLinked(UUID playerUUID) {
        ConfigurationSection linkedAccounts = plugin.getConfig().getConfigurationSection("telegram.linked-accounts");
        if (linkedAccounts != null) {
            for (String chatId : linkedAccounts.getKeys(false)) {
                String uuid = linkedAccounts.getString(chatId);
                if (uuid != null && uuid.equals(playerUUID.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendHelpMessage(long chatId) {
        String helpText = """
            *📚 Список доступных команд:*
            
            📋 /reports - Просмотр всех репортов
            📊 /menu - Открыть главное меню
            ❓ /help - Показать это сообщение
            
            *Управление репортами:*
            • Используйте кнопки под сообщениями для управления репортами
            • Вы можете изменять статус репортов
            • Отправлять сообщения игрокам
            • Получать уведомления о новых репортах
            
            *Примечание:* Все действия синхронизируются с игрой
            """;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(helpText);
        message.setParseMode("Markdown");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке справки", e);
        }
    }

    private void sendNewReportsList(long chatId) {
        List<Report> reports = plugin.getDatabaseManager().getAllReports().stream()
            .filter(r -> r.getStatus().equals("Новый"))
            .toList();

        if (reports.isEmpty()) {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(Arrays.asList(createInlineButton("◀️ Вернуться в меню", "back")));
            markup.setKeyboard(keyboard);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("📝 *Новых репортов нет*");
            message.setParseMode("Markdown");
            message.setReplyMarkup(markup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке сообщения об отсутствии новых репортов", e);
            }
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Report report : reports) {
            String buttonText = String.format("🆕 #%d - %s", report.getId(), 
                truncateString(report.getDescription(), 30));
            keyboard.add(Arrays.asList(createInlineButton(buttonText, "view:" + report.getId())));
        }

        keyboard.add(Arrays.asList(createInlineButton("◀️ Назад", "back")));
        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Список новых репортов:");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке списка новых репортов", e);
        }
    }

    private void sendSettingsMenu(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(Arrays.asList(
            createInlineButton("🔔 Уведомления", "settings:notifications"),
            createInlineButton("🔒 Безопасность", "settings:security")
        ));

        keyboard.add(Arrays.asList(
            createInlineButton("👥 Заблокированные", "settings:blacklist")
        ));

        keyboard.add(Arrays.asList(createInlineButton("◀️ Назад", "back")));
        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚙️ *Настройки*\nВыберите категорию настроек:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке меню настроек", e);
        }
    }

    private void sendSecuritySettings(long chatId) {
        String text = """
            🔒 *Настройки безопасности*
            
            • Время действия кода: %d минут
            • Максимум попыток: %d
            
            _Эти настройки можно изменить в config.yml_
            """.formatted(
                plugin.getConfig().getInt("telegram.security.verification-code-timeout", 5),
                plugin.getConfig().getInt("telegram.security.max-verification-attempts", 3)
            );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Arrays.asList(createInlineButton("◀️ Назад к настройкам", "settings")));
        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке настроек безопасности", e);
        }
    }

    private void sendBlacklistMenu(long chatId) {
        List<UUID> blacklist = plugin.getDatabaseManager().getBlacklist();
        StringBuilder text = new StringBuilder("👥 *Заблокированные пользователи*\n\n");

        if (blacklist.isEmpty()) {
            text.append("_Список пуст_");
        } else {
            for (UUID uuid : blacklist) {
                String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                if (playerName != null) {
                    text.append("• ").append(playerName).append("\n");
                }
            }
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Arrays.asList(createInlineButton("◀️ Назад к настройкам", "settings")));
        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отправке списка заблокированных", e);
        }
    }

    private void deleteReport(long chatId, int reportId) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report == null) {
            sendMessage(chatId, "Репорт не найден.");
            return;
        }

        plugin.getDatabaseManager().deleteReport(reportId);
        sendMessage(chatId, String.format("✅ Репорт #%d успешно удален", reportId));
        sendReportsList(chatId);
    }

    private void blockPlayer(long chatId, UUID playerUUID) {
        String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
        if (playerName == null) {
            sendMessage(chatId, "❌ Игрок не найден");
            return;
        }

        if (plugin.getDatabaseManager().isBlacklisted(playerUUID)) {
            plugin.getDatabaseManager().removeFromBlacklist(playerUUID);
            sendMessage(chatId, String.format("✅ Игрок %s разблокирован", playerName));
        } else {
            plugin.getDatabaseManager().addToBlacklist(playerUUID);
            sendMessage(chatId, String.format("🚫 Игрок %s заблокирован", playerName));
        }
    }
} 