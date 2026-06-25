package com.gamesphere.controller;

import com.gamesphere.dto.request.CreateTournamentRequest;
import com.gamesphere.dto.request.UpdateTournamentRequest;
import com.gamesphere.dto.response.ApiResponse;
import com.gamesphere.dto.response.TournamentResponse;
import com.gamesphere.dto.response.TournamentSummaryResponse;
import com.gamesphere.enums.TournamentStatus;
import com.gamesphere.service.TournamentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tournaments  → Create tournament (ADMIN only)
    // -------------------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TournamentResponse>> createTournament(
            @Valid @RequestBody CreateTournamentRequest request) {
        TournamentResponse tournament = tournamentService.createTournament(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tournament created successfully.", tournament));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/tournaments/{id}  → Get tournament details (authenticated)
    // -------------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TournamentResponse>> getTournamentById(@PathVariable Long id) {
        TournamentResponse tournament = tournamentService.getTournamentById(id);
        return ResponseEntity.ok(ApiResponse.success("Tournament fetched successfully.", tournament));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/tournaments  → List all tournaments (paginated, filterable by status)
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TournamentSummaryResponse>>> getAllTournaments(
            @RequestParam(required = false) TournamentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TournamentSummaryResponse> tournaments = tournamentService.getAllTournaments(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tournaments fetched successfully.", tournaments));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/tournaments/search?name=...  → Search by name (paginated)
    // -------------------------------------------------------------------------
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<TournamentSummaryResponse>>> searchTournaments(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TournamentSummaryResponse> tournaments = tournamentService.searchTournamentsByName(name, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tournaments searched successfully.", tournaments));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/tournaments/{id}  → Update tournament (ADMIN only)
    // -------------------------------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TournamentResponse>> updateTournament(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTournamentRequest request) {
        TournamentResponse updated = tournamentService.updateTournament(id, request);
        return ResponseEntity.ok(ApiResponse.success("Tournament updated successfully.", updated));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/tournaments/{id}/status  → Update status only (ADMIN only)
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TournamentResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam TournamentStatus status) {
        TournamentResponse updated = tournamentService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Tournament status updated successfully.", updated));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/tournaments/{id}  → Soft-delete (ADMIN only)
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.ok(ApiResponse.success("Tournament deleted successfully."));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tournaments/{id}/register?teamId=...  → Register a team
    // -------------------------------------------------------------------------
    @PostMapping("/{id}/register")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ApiResponse<TournamentResponse>> registerTeam(
            @PathVariable Long id,
            @RequestParam Long teamId) {
        TournamentResponse tournament = tournamentService.registerTeam(id, teamId);
        return ResponseEntity.ok(ApiResponse.success("Team registered for tournament successfully.", tournament));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/tournaments/{id}/deregister?teamId=...  → Deregister a team
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}/deregister")
    @PreAuthorize("hasAnyRole('PLAYER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TournamentResponse>> deregisterTeam(
            @PathVariable Long id,
            @RequestParam Long teamId) {
        TournamentResponse tournament = tournamentService.deregisterTeam(id, teamId);
        return ResponseEntity.ok(ApiResponse.success("Team deregistered from tournament successfully.", tournament));
    }
}
