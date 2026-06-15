package com.memorycard.dto.request;

import com.memorycard.entity.GameStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GameRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 100) String platform,
        @NotNull GameStatus status,
        @DecimalMin("0") @DecimalMax("99999.99") BigDecimal hoursPlayed,
        @DecimalMin("0") @DecimalMax("10") BigDecimal personalRating,
        @DecimalMin("0") @DecimalMax("10") BigDecimal externalRating,
        String notes,
        LocalDate startedAt,
        LocalDate completedAt
) {}
