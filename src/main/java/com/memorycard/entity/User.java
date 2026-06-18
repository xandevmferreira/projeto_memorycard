package com.memorycard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.FREE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "community_visible", nullable = false)
    private boolean communityVisible;

    @Column(name = "retro_achievements_username", length = 100)
    private String retroAchievementsUsername;

    @Column(name = "retro_achievements_api_key")
    private String retroAchievementsApiKey;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCommunityVisible() { return communityVisible; }
    public void setCommunityVisible(boolean communityVisible) { this.communityVisible = communityVisible; }
    public String getRetroAchievementsUsername() { return retroAchievementsUsername; }
    public void setRetroAchievementsUsername(String retroAchievementsUsername) { this.retroAchievementsUsername = retroAchievementsUsername; }
    public String getRetroAchievementsApiKey() { return retroAchievementsApiKey; }
    public void setRetroAchievementsApiKey(String retroAchievementsApiKey) { this.retroAchievementsApiKey = retroAchievementsApiKey; }
}
