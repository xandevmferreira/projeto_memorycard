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

    @Column(length = 30, unique = true)
    private String nick;

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

    @Column(name = "share_notes_with_friends", nullable = false)
    private boolean shareNotesWithFriends = true;

    @Column(name = "share_journal_with_friends", nullable = false)
    private boolean shareJournalWithFriends = true;

    @Column(name = "share_screenshots_with_friends", nullable = false)
    private boolean shareScreenshotsWithFriends = true;

    @Column(name = "sync_token_hash", length = 64)
    private String syncTokenHash;

    @Column(name = "sync_token_created_at")
    private Instant syncTokenCreatedAt;

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

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getDisplayNick() {
        return nick != null && !nick.isBlank() ? nick.trim() : "Jogador";
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
    public boolean isShareNotesWithFriends() { return shareNotesWithFriends; }
    public void setShareNotesWithFriends(boolean shareNotesWithFriends) { this.shareNotesWithFriends = shareNotesWithFriends; }
    public boolean isShareJournalWithFriends() { return shareJournalWithFriends; }
    public void setShareJournalWithFriends(boolean shareJournalWithFriends) { this.shareJournalWithFriends = shareJournalWithFriends; }
    public boolean isShareScreenshotsWithFriends() { return shareScreenshotsWithFriends; }
    public void setShareScreenshotsWithFriends(boolean shareScreenshotsWithFriends) { this.shareScreenshotsWithFriends = shareScreenshotsWithFriends; }
    public String getSyncTokenHash() { return syncTokenHash; }
    public void setSyncTokenHash(String syncTokenHash) { this.syncTokenHash = syncTokenHash; }
    public Instant getSyncTokenCreatedAt() { return syncTokenCreatedAt; }
    public void setSyncTokenCreatedAt(Instant syncTokenCreatedAt) { this.syncTokenCreatedAt = syncTokenCreatedAt; }
}
