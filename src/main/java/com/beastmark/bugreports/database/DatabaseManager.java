package com.beastmark.bugreports.database;

import com.beastmark.bugreports.model.Report;
import java.util.List;
import java.util.UUID;


public interface DatabaseManager {
    void init();
    void close();
    
    // Методы для работы с репортами
    void saveReport(Report report);
    void deleteReport(int id);
    void updateReportStatus(int id, String status);
    Report getReport(int id);
    List<Report> getPlayerReports(UUID playerUUID);
    List<Report> getAllReports();
    int getTotalReports();
    int getPlayerCompletedReports(UUID playerUUID);
    void deleteAllReports();
    
    // Методы для работы с черным списком
    void addToBlacklist(UUID playerUUID);
    void removeFromBlacklist(UUID playerUUID);
    boolean isBlacklisted(UUID playerUUID);
    List<UUID> getBlacklist();
    
    default boolean hasUnreadReports(UUID playerId) {
        return getPlayerReports(playerId).stream()
            .anyMatch(report -> report.getStatus().equals("Новый") || 
                              report.getStatus().equals("В обработке"));
    }

    Report getLatestReport();
} 