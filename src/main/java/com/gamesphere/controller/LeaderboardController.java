package com.gamesphere.controller;

import com.gamesphere.dto.response.ApiResponse;
import com.gamesphere.dto.response.DashboardStatsResponse;
import com.gamesphere.dto.response.LeaderboardEntryResponse;
import com.gamesphere.service.LeaderboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/leaderboard  → Team leaderboard (paginated, public)
    // -------------------------------------------------------------------------
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<Page<LeaderboardEntryResponse>>> getTeamLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LeaderboardEntryResponse> leaderboard = leaderboardService.getTeamLeaderboard(pageable);
        return ResponseEntity.ok(ApiResponse.success("Leaderboard fetched successfully.", leaderboard));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/dashboard  → Platform dashboard stats (ADMIN only)
    // -------------------------------------------------------------------------
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = leaderboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully.", stats));
    }
}
