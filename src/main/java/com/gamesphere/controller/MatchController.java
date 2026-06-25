package com.gamesphere.controller;

import com.gamesphere.dto.request.RecordMatchResultRequest;
import com.gamesphere.dto.request.ScheduleMatchRequest;
import com.gamesphere.dto.response.ApiResponse;
import com.gamesphere.dto.response.MatchResponse;
import com.gamesphere.enums.MatchStatus;
import com.gamesphere.service.MatchService;
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
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/matches  → Schedule a match (ADMIN only)
    // -------------------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MatchResponse>> scheduleMatch(
            @Valid @RequestBody ScheduleMatchRequest request) {
        MatchResponse match = matchService.scheduleMatch(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Match scheduled successfully.", match));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/matches/{id}  → Get match details (authenticated)
    // -------------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchResponse>> getMatchById(@PathVariable Long id) {
        MatchResponse match = matchService.getMatchById(id);
        return ResponseEntity.ok(ApiResponse.success("Match fetched successfully.", match));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/matches  → Get all matches filtered by status (paginated)
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MatchResponse>>> getMatchesByStatus(
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "scheduledAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MatchResponse> matches = matchService.getMatchesByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Matches fetched successfully.", matches));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/matches/tournament/{tournamentId}  → Matches by tournament
    // -------------------------------------------------------------------------
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<ApiResponse<Page<MatchResponse>>> getMatchesByTournament(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledAt").ascending());
        Page<MatchResponse> matches = matchService.getMatchesByTournament(tournamentId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tournament matches fetched successfully.", matches));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/matches/team/{teamId}  → All matches for a specific team
    // -------------------------------------------------------------------------
    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<Page<MatchResponse>>> getMatchesByTeam(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledAt").descending());
        Page<MatchResponse> matches = matchService.getMatchesByTeam(teamId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Team matches fetched successfully.", matches));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/matches/{id}/result  → Record match result (ADMIN only)
    // -------------------------------------------------------------------------
    @PostMapping("/{id}/result")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MatchResponse>> recordResult(
            @PathVariable Long id,
            @Valid @RequestBody RecordMatchResultRequest request) {
        MatchResponse match = matchService.recordResult(id, request);
        return ResponseEntity.ok(ApiResponse.success("Match result recorded successfully.", match));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/matches/{id}/status  → Update match status (ADMIN only)
    // -------------------------------------------------------------------------
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MatchResponse>> updateMatchStatus(
            @PathVariable Long id,
            @RequestParam MatchStatus status) {
        MatchResponse match = matchService.updateMatchStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Match status updated successfully.", match));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/matches/{id}  → Soft-delete match (ADMIN only)
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.ok(ApiResponse.success("Match deleted successfully."));
    }
}
