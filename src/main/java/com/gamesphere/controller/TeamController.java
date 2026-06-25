package com.gamesphere.controller;

import com.gamesphere.dto.request.CreateTeamRequest;
import com.gamesphere.dto.request.UpdateTeamRequest;
import com.gamesphere.dto.response.ApiResponse;
import com.gamesphere.dto.response.TeamResponse;
import com.gamesphere.dto.response.TeamSummaryResponse;
import com.gamesphere.security.UserPrincipal;
import com.gamesphere.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/teams  → Create a new team (PLAYER only)
    // -------------------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        TeamResponse team = teamService.createTeam(currentUser.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Team created successfully.", team));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/teams/{id}  → Get team details (authenticated)
    // -------------------------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable Long id) {
        TeamResponse team = teamService.getTeamById(id);
        return ResponseEntity.ok(ApiResponse.success("Team fetched successfully.", team));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/teams  → List all teams (paginated)
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TeamSummaryResponse>>> getAllTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TeamSummaryResponse> teams = teamService.getAllTeams(pageable);
        return ResponseEntity.ok(ApiResponse.success("Teams fetched successfully.", teams));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/teams/search?name=...  → Search teams by name (paginated)
    // -------------------------------------------------------------------------
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<TeamSummaryResponse>>> searchTeams(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TeamSummaryResponse> teams = teamService.searchTeamsByName(name, pageable);
        return ResponseEntity.ok(ApiResponse.success("Teams searched successfully.", teams));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/teams/leaderboard  → Teams ranked by win rate (paginated)
    // -------------------------------------------------------------------------
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<Page<TeamSummaryResponse>>> getLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TeamSummaryResponse> leaderboard = teamService.getTeamLeaderboard(pageable);
        return ResponseEntity.ok(ApiResponse.success("Team leaderboard fetched successfully.", leaderboard));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/teams/{id}  → Update team (captain only)
    // -------------------------------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeamRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        TeamResponse updated = teamService.updateTeam(id, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Team updated successfully.", updated));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/teams/{id}  → Soft-delete team (captain only)
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        teamService.deleteTeam(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Team deleted successfully."));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/teams/{id}/join  → Player joins a team
    // -------------------------------------------------------------------------
    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ApiResponse<TeamResponse>> joinTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        TeamResponse team = teamService.joinTeam(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Joined team successfully.", team));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/teams/{id}/leave  → Player leaves a team
    // -------------------------------------------------------------------------
    @PostMapping("/{id}/leave")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        teamService.leaveTeam(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Left team successfully."));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/teams/{id}/transfer-captaincy  → Transfer captain role
    // -------------------------------------------------------------------------
    @PostMapping("/{id}/transfer-captaincy")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ApiResponse<TeamResponse>> transferCaptaincy(
            @PathVariable Long id,
            @RequestParam Long newCaptainId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        TeamResponse team = teamService.transferCaptaincy(id, currentUser.getId(), newCaptainId);
        return ResponseEntity.ok(ApiResponse.success("Captaincy transferred successfully.", team));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/teams/{id}/members/{memberId}  → Captain removes a member
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize("hasRole('PLAYER')")
    public ResponseEntity<ApiResponse<TeamResponse>> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        TeamResponse team = teamService.removeMember(id, currentUser.getId(), memberId);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully.", team));
    }

    // -------------------------------------------------------------------------
    // ADMIN: DELETE /api/v1/teams/{id}/admin  → Admin force-deletes any team
    // -------------------------------------------------------------------------
    @DeleteMapping("/{id}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminDeleteTeam(@PathVariable Long id) {
        // Admin bypasses captain check — we need a dedicated admin override.
        // For now, re-use deleteTeam with a sentinel admin user id approach is avoided.
        // Instead, we retrieve the captain's id from the team and pass it.
        TeamResponse teamDetails = teamService.getTeamById(id);
        Long captainId = teamDetails.getCaptain() != null ? teamDetails.getCaptain().getId() : null;
        if (captainId != null) {
            teamService.deleteTeam(id, captainId);
        }
        return ResponseEntity.ok(ApiResponse.success("Team force-deleted by admin."));
    }
}
