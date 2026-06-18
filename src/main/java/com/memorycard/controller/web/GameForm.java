package com.memorycard.controller.web;

import com.memorycard.entity.CompletionType;
import com.memorycard.entity.GameStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class GameForm {

    @NotBlank
    @Size(max = 255)
    private String title = "";

    @Size(max = 100)
    private String platform = "";

    @NotNull
    private GameStatus status = GameStatus.PLAYING;

    @DecimalMin("0")
    @DecimalMax("99999.99")
    private BigDecimal hoursPlayed = BigDecimal.ZERO;

    @DecimalMin("0")
    @DecimalMax("10")
    private BigDecimal personalRating;

    private String notes = "";

    private LocalDate startedAt;

    private LocalDate completedAt;

    private CompletionType completionType;

    @Size(max = 500)
    private String tags = "";

    private boolean retro;

    @Size(max = 100)
    private String emulator = "";

    private Integer retroAchievementsGameId;

    private Integer retroConsoleId;

    private String externalCoverUrl = "";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public BigDecimal getHoursPlayed() { return hoursPlayed; }
    public void setHoursPlayed(BigDecimal hoursPlayed) { this.hoursPlayed = hoursPlayed; }
    public BigDecimal getPersonalRating() { return personalRating; }
    public void setPersonalRating(BigDecimal personalRating) { this.personalRating = personalRating; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDate startedAt) { this.startedAt = startedAt; }
    public LocalDate getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDate completedAt) { this.completedAt = completedAt; }
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
    public String getExternalCoverUrl() { return externalCoverUrl; }
    public void setExternalCoverUrl(String externalCoverUrl) { this.externalCoverUrl = externalCoverUrl; }

    public com.memorycard.dto.request.GameRequest toRequest() {
        return new com.memorycard.dto.request.GameRequest(
                title, platform, status, hoursPlayed,
                personalRating, notes, startedAt, completedAt,
                completionType, tags, retro, emulator, retroAchievementsGameId, retroConsoleId
        );
    }
}
