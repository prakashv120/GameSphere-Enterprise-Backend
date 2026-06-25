package com.gamesphere.dto.response;

import com.gamesphere.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tournamentId;
    private String tournamentName;
    private TeamInfo teamA;
    private TeamInfo teamB;
    private TeamInfo winner;
    private int teamAScore;
    private int teamBScore;
    private MatchStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime playedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamInfo implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String name;
        private String tag;
    }
}
