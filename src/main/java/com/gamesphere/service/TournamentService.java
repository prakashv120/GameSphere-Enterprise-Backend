package com.gamesphere.service;

import com.gamesphere.dto.request.CreateTournamentRequest;
import com.gamesphere.dto.request.UpdateTournamentRequest;
import com.gamesphere.dto.response.TournamentResponse;
import com.gamesphere.dto.response.TournamentSummaryResponse;
import com.gamesphere.enums.TournamentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TournamentService {

    /**
     * Create a new tournament. Only ADMIN may call this.
     */
    TournamentResponse createTournament(CreateTournamentRequest request);

    /**
     * Get full tournament details by ID.
     */
    TournamentResponse getTournamentById(Long tournamentId);

    /**
     * Get all tournaments with optional status filter (paginated).
     */
    Page<TournamentSummaryResponse> getAllTournaments(TournamentStatus status, Pageable pageable);

    /**
     * Search tournaments by name (paginated).
     */
    Page<TournamentSummaryResponse> searchTournamentsByName(String name, Pageable pageable);

    /**
     * Update tournament details and/or status. Only ADMIN may call this.
     */
    TournamentResponse updateTournament(Long tournamentId, UpdateTournamentRequest request);

    /**
     * Soft-delete a tournament. Only ADMIN may call this.
     */
    void deleteTournament(Long tournamentId);

    /**
     * Register a team for a tournament (PLAYER / captain).
     */
    TournamentResponse registerTeam(Long tournamentId, Long teamId);

    /**
     * Deregister a team from a tournament (PLAYER / captain or ADMIN).
     */
    TournamentResponse deregisterTeam(Long tournamentId, Long teamId);

    /**
     * Update the status of a tournament (ADMIN-only shortcut).
     */
    TournamentResponse updateStatus(Long tournamentId, TournamentStatus newStatus);
}
