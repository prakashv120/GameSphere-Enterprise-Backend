package com.gamesphere.dto.response;

import com.gamesphere.enums.TournamentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentSummaryResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private TournamentStatus status;
    private int maxTeams;
    private int registeredTeamsCount;
    private Double prizePool;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
