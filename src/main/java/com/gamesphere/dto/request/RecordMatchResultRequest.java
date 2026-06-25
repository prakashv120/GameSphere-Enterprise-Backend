package com.gamesphere.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordMatchResultRequest {

    @NotNull(message = "Team A score is required")
    @Min(value = 0, message = "Score must be non-negative")
    private Integer teamAScore;

    @NotNull(message = "Team B score is required")
    @Min(value = 0, message = "Score must be non-negative")
    private Integer teamBScore;

    private String notes;
}
