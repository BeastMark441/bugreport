package com.beastmark.bugreports.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Report {
    private int id;
    private final UUID playerUUID;
    private final String playerName;
    private final String category;
    private final String description;
    private final ReportType type;
    private String status;
    private LocalDateTime createdAt;

    public Report(UUID playerUUID, String playerName, String category, String description, ReportType type) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.category = category;
        this.description = description;
        this.type = type;
        this.status = "Новый";
        this.createdAt = LocalDateTime.now();
    }

    // Геттеры
    public int getId() { return id; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public ReportType getType() { return type; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Сеттеры
    public void setId(int id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 