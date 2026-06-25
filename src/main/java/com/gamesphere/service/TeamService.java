package com.gamesphere.service;

import com.gamesphere.dto.request.CreateTeamRequest;
import com.gamesphere.dto.request.UpdateTeamRequest;
import com.gamesphere.dto.response.TeamResponse;
import com.gamesphere.dto.response.TeamSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamService {

    /**
     * Create a new team. The authenticated user becomes the captain and first member.
     */
    TeamResponse createTeam(Long captainUserId, CreateTeamRequest request);

    /**
     * Get full team details by ID.
     */
    TeamResponse getTeamById(Long teamId);

    /**
     * Get all teams (paginated, lightweight summary).
     */
    Page<TeamSummaryResponse> getAllTeams(Pageable pageable);

    /**
     * Search teams by name (case-insensitive, paginated).
     */
    Page<TeamSummaryResponse> searchTeamsByName(String name, Pageable pageable);

    /**
     * Update team name/description. Only the captain can perform this.
     */
    TeamResponse updateTeam(Long teamId, Long requestingUserId, UpdateTeamRequest request);

    /**
     * Soft-delete a team. Only the captain can perform this.
     */
    void deleteTeam(Long teamId, Long requestingUserId);

    /**
     * A player joins an existing team. Player must not already be in a team.
     */
    TeamResponse joinTeam(Long teamId, Long userId);

    /**
     * A player leaves their current team. Captain cannot leave without transferring.
     */
    void leaveTeam(Long teamId, Long userId);

    /**
     * Captain transfers captaincy to another team member.
     */
    TeamResponse transferCaptaincy(Long teamId, Long currentCaptainId, Long newCaptainId);

    /**
     * Captain removes a member from the team.
     */
    TeamResponse removeMember(Long teamId, Long captainId, Long memberIdToRemove);

    /**
     * Get teams ordered by win rate (leaderboard).
     */
    Page<TeamSummaryResponse> getTeamLeaderboard(Pageable pageable);
}
