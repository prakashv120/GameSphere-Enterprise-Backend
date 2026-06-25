package com.gamesphere.service;

import com.gamesphere.dto.response.DashboardStatsResponse;
import com.gamesphere.dto.response.LeaderboardEntryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeaderboardService {

    /**
     * Get team leaderboard ranked by win rate (paginated, Redis-cached).
     */
    Page<LeaderboardEntryResponse> getTeamLeaderboard(Pageable pageable);

    /**
     * Get platform dashboard statistics (Redis-cached with shorter TTL).
     */
    DashboardStatsResponse getDashboardStats();
}
