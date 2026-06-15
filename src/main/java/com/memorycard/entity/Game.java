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

    @Column(name = "external_rating", precision = 3, scale = 1)
    private BigDecimal externalRating;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "completed_at")
    private LocalDate completedAt;

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
