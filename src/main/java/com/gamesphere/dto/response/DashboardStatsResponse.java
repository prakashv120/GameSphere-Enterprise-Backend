package com.gamesphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long totalUsers;
    private long totalTeams;
    private long totalTournaments;
    private long totalMatches;
    private long activeTournaments;
    private long completedMatches;
    private long scheduledMatches;
    private long liveMatches;
}
