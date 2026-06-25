package com.gamesphere.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMatchRequest {

    @NotNull(message = "Tournament ID is required")
    private Long tournamentId;

    @NotNull(message = "Team A ID is required")
    private Long teamAId;

    @NotNull(message = "Team B ID is required")
    private Long teamBId;

    @NotNull(message = "Scheduled date/time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledAt;

    private String notes;
}
