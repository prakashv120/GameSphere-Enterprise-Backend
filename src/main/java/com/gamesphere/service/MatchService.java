package com.gamesphere.service;

import com.gamesphere.dto.request.RecordMatchResultRequest;
import com.gamesphere.dto.request.ScheduleMatchRequest;
import com.gamesphere.dto.response.MatchResponse;
import com.gamesphere.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchService {

    /**
     * Schedule a new match within a tournament. Only ADMIN may call this.
     */
    MatchResponse scheduleMatch(ScheduleMatchRequest request);

    /**
     * Get match details by ID.
     */
    MatchResponse getMatchById(Long matchId);

    /**
     * Get all matches for a specific tournament (paginated, optionally filtered by status).
     */
    Page<MatchResponse> getMatchesByTournament(Long tournamentId, MatchStatus status, Pageable pageable);

    /**
     * Get all matches for a specific team (paginated).
     */
    Page<MatchResponse> getMatchesByTeam(Long teamId, Pageable pageable);

    /**
     * Get matches filtered by status only (paginated).
     */
    Page<MatchResponse> getMatchesByStatus(MatchStatus status, Pageable pageable);

    /**
     * Record the result of a completed match (scores and winner). ADMIN-only.
     */
    MatchResponse recordResult(Long matchId, RecordMatchResultRequest request);

    /**
     * Update the status of a match (e.g., ONGOING). ADMIN-only.
     */
    MatchResponse updateMatchStatus(Long matchId, MatchStatus newStatus);

    /**
     * Soft-delete a match. ADMIN-only.
     */
    void deleteMatch(Long matchId);
}
