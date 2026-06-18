package com.memorycard.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    private String platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.PLAYING;

    @Column(name = "hours_played", precision = 8, scale = 2)
    private BigDecimal hoursPlayed = BigDecimal.ZERO;

    @Column(name = "personal_rating", precision = 3, scale = 1)
    private BigDecimal personalRating;

    @Column(name = "external_rating", precision = 5, scale = 1)
    private BigDecimal externalRating;

    @Column(name = "rating_source", length = 20)
    private String ratingSource;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_type")
    private CompletionType completionType;

    @Column(length = 500)
    private String tags;

    @Column(name = "is_retro", nullable = false)
    private boolean retro;

    private String emulator;

    @Column(name = "retro_achievements_game_id")
    private Integer retroAchievementsGameId;

    @Column(name = "retro_console_id")
    private Integer retroConsoleId;

    @Column(name = "retro_progress_percent", precision = 5, scale = 2)
    private java.math.BigDecimal retroProgressPercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public BigDecimal getHoursPlayed() {
        return hoursPlayed;
    }

    public void setHoursPlayed(BigDecimal hoursPlayed) {
        this.hoursPlayed = hoursPlayed;
    }

    public BigDecimal getPersonalRating() {
        return personalRating;
    }

    public void setPersonalRating(BigDecimal personalRating) {
        this.personalRating = personalRating;
    }

    public BigDecimal getExternalRating() {
        return externalRating;
    }

    public void setExternalRating(BigDecimal externalRating) {
        this.externalRating = externalRating;
    }

    public String getRatingSource() { return ratingSource; }
    public void setRatingSource(String ratingSource) { this.ratingSource = ratingSource; }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public LocalDate getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDate startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDate getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDate completedAt) {
        this.completedAt = completedAt;
    }

    public CompletionType getCompletionType() { return completionType; }
    public void setCompletionType(CompletionType completionType) { this.completionType = completionType; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public boolean isRetro() { return retro; }
    public void setRetro(boolean retro) { this.retro = retro; }
    public String getEmulator() { return emulator; }
    public void setEmulator(String emulator) { this.emulator = emulator; }
    public Integer getRetroAchievementsGameId() { return retroAchievementsGameId; }
    public void setRetroAchievementsGameId(Integer retroAchievementsGameId) { this.retroAchievementsGameId = retroAchievementsGameId; }
    public Integer getRetroConsoleId() { return retroConsoleId; }
    public void setRetroConsoleId(Integer retroConsoleId) { this.retroConsoleId = retroConsoleId; }
    public java.math.BigDecimal getRetroProgressPercent() { return retroProgressPercent; }
    public void setRetroProgressPercent(java.math.BigDecimal retroProgressPercent) { this.retroProgressPercent = retroProgressPercent; }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
