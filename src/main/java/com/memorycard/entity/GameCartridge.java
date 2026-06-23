package com.memorycard.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "game_cartridges")
public class GameCartridge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String memories;

    @Column(name = "session_date")
    private LocalDate sessionDate;

    @Column(name = "emulator_hint", length = 100)
    private String emulatorHint;

    @Column(name = "snapshot_path", length = 500)
    private String snapshotPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getMemories() { return memories; }
    public void setMemories(String memories) { this.memories = memories; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public String getEmulatorHint() { return emulatorHint; }
    public void setEmulatorHint(String emulatorHint) { this.emulatorHint = emulatorHint; }
    public String getSnapshotPath() { return snapshotPath; }
    public void setSnapshotPath(String snapshotPath) { this.snapshotPath = snapshotPath; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
