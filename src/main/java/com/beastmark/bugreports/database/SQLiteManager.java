package com.beastmark.bugreports.database;

import com.beastmark.bugreports.BugReports;
import com.beastmark.bugreports.model.Report;
import com.beastmark.bugreports.model.ReportType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLiteManager implements DatabaseManager {
    private final BugReports plugin;
    private Connection connection;

    public SQLiteManager(BugReports plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + 
                new File(plugin.getDataFolder(), "database.db").getAbsolutePath());

            // Создаем таблицы если они не существуют
            try (Statement statement = connection.createStatement()) {
                // Таблица репортов
                statement.execute("CREATE TABLE IF NOT EXISTS reports (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(16) NOT NULL," +
                    "category VARCHAR(50) NOT NULL," +
                    "description TEXT NOT NULL," +
                    "status VARCHAR(20) NOT NULL," +
                    "created_at TIMESTAMP NOT NULL," +
                    "type VARCHAR(10) NOT NULL" +
                    ")");

                // Таблица черного списка
                statement.execute("CREATE TABLE IF NOT EXISTS blacklist (" +
                    "player_uuid VARCHAR(36) PRIMARY KEY" +
                    ")");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка закрытия базы данных: " + e.getMessage());
        }
    }

    @Override
    public void saveReport(Report report) {
        checkConnection();
        String sql = "INSERT INTO reports (player_uuid, player_name, category, description, status, created_at, type) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, report.getPlayerUUID().toString());
            stmt.setString(2, report.getPlayerName());
            stmt.setString(3, report.getCategory());
            stmt.setString(4, report.getDescription());
            stmt.setString(5, report.getStatus());
            stmt.setTimestamp(6, Timestamp.valueOf(report.getCreatedAt()));
            stmt.setString(7, report.getType().name());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    report.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка сохранения репорта: " + e.getMessage());
        }
    }

    @Override
    public void deleteReport(int id) {
        checkConnection();
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM reports WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка удаления репорта: " + e.getMessage());
        }
    }

    @Override
    public void deleteAllReports() {
        checkConnection();
        try {
            connection.setAutoCommit(false);
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM reports");
                connection.commit();
                plugin.getLogger().info("Все репорты были успешно удалены");
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка удаления всех репортов: " + e.getMessage());
        }
    }

    private void checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                init();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка проверки соединения с базой данных: " + e.getMessage());
            init();
        }
    }

    @Override
    public void updateReportStatus(int id, String status) {
        checkConnection();
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE reports SET status = ? WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка обновления статуса репорта: " + e.getMessage());
        }
    }

    @Override
    public Report getReport(int id) {
        checkConnection();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM reports WHERE id = ?")) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return createReportFromResultSet(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка получения репорта: " + e.getMessage());
        }
        return null;
    }

    private Report createReportFromResultSet(ResultSet rs) throws SQLException {
        Report report = new Report(
            UUID.fromString(rs.getString("player_uuid")),
            rs.getString("player_name"),
            rs.getString("category"),
            rs.getString("description"),
            ReportType.valueOf(rs.getString("type"))
        );
        report.setId(rs.getInt("id"));
        report.setStatus(rs.getString("status"));
        return report;
    }

    @Override
    public List<Report> getPlayerReports(UUID playerId) {
        checkConnection();
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE player_uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                reports.add(createReportFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении репортов игрока: " + e.getMessage());
        }
        
        return reports;
    }

    @Override
    public List<Report> getAllReports() {
        checkConnection();
        List<Report> reports = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM reports ORDER BY created_at DESC")) {
            while (rs.next()) {
                reports.add(createReportFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка получения всех репортов: " + e.getMessage());
        }
        return reports;
    }

    @Override
    public int getTotalReports() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reports")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка подсчета репортов: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public int getPlayerCompletedReports(UUID playerUUID) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM reports WHERE player_uuid = ? AND status IN ('Завершено', 'Исправлено')")) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка подсчета завершенных репортов: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void addToBlacklist(UUID playerUUID) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO blacklist (player_uuid) VALUES (?)")) {
            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка добавления в черный список: " + e.getMessage());
        }
    }

    @Override
    public void removeFromBlacklist(UUID playerUUID) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM blacklist WHERE player_uuid = ?")) {
            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка удаления из черного списка: " + e.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(UUID playerUUID) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM blacklist WHERE player_uuid = ?")) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка проверки черного списка: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<UUID> getBlacklist() {
        List<UUID> blacklist = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_uuid FROM blacklist")) {
            while (rs.next()) {
                blacklist.add(UUID.fromString(rs.getString("player_uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка получения черного списка: " + e.getMessage());
        }
        return blacklist;
    }
} 