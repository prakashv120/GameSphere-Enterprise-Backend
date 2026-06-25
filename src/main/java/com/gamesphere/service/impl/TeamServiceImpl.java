package com.gamesphere.service.impl;

import com.gamesphere.dto.request.CreateTeamRequest;
import com.gamesphere.dto.request.UpdateTeamRequest;
import com.gamesphere.dto.response.TeamResponse;
import com.gamesphere.dto.response.TeamSummaryResponse;
import com.gamesphere.entity.Team;
import com.gamesphere.entity.User;
import com.gamesphere.exception.BadRequestException;
import com.gamesphere.exception.ResourceNotFoundException;
import com.gamesphere.exception.UnauthorizedException;
import com.gamesphere.mapper.TeamMapper;
import com.gamesphere.repository.TeamRepository;
import com.gamesphere.repository.UserRepository;
import com.gamesphere.service.TeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;

    public TeamServiceImpl(TeamRepository teamRepository,
                           UserRepository userRepository,
                           TeamMapper teamMapper) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamMapper = teamMapper;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = "teams", allEntries = true)
    public TeamResponse createTeam(Long captainUserId, CreateTeamRequest request) {
        log.info("Creating team '{}' by user id={}", request.getName(), captainUserId);

        User captain = findUserById(captainUserId);

        if (captain.getTeam() != null) {
            throw new BadRequestException("You are already a member of a team. Leave your current team before creating a new one.");
        }
        if (teamRepository.existsByName(request.getName())) {
            throw new BadRequestException("Team name '" + request.getName() + "' is already taken.");
        }
        if (teamRepository.existsByTag(request.getTag())) {
            throw new BadRequestException("Team tag '" + request.getTag() + "' is already taken.");
        }

        Team team = Team.builder()
                .name(request.getName())
                .tag(request.getTag())
                .description(request.getDescription())
                .captain(captain)
                .build();

        Team savedTeam = teamRepository.save(team);

        // Assign the captain as the first member
        captain.setTeam(savedTeam);
        userRepository.save(captain);

        // Reload with members populated
        Team reloaded = findTeamById(savedTeam.getId());
        log.info("Team '{}' created with id={}", savedTeam.getName(), savedTeam.getId());
        return teamMapper.toTeamResponse(reloaded);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "team", key = "#teamId")
    public TeamResponse getTeamById(Long teamId) {
        log.debug("Fetching team id={}", teamId);
        Team team = findTeamById(teamId);
        return teamMapper.toTeamResponse(team);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "teams", key = "#pageable")
    public Page<TeamSummaryResponse> getAllTeams(Pageable pageable) {
        log.debug("Fetching all teams page={}", pageable.getPageNumber());
        return teamRepository.findAll(pageable)
                .map(teamMapper::toTeamSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamSummaryResponse> searchTeamsByName(String name, Pageable pageable) {
        log.debug("Searching teams by name='{}'", name);
        return teamRepository.searchByName(name, pageable)
                .map(teamMapper::toTeamSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamSummaryResponse> getTeamLeaderboard(Pageable pageable) {
        log.debug("Fetching team leaderboard page={}", pageable.getPageNumber());
        return teamRepository.findAllOrderByWinRateDesc(pageable)
                .map(teamMapper::toTeamSummaryResponse);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = {"team", "teams"}, allEntries = true)
    public TeamResponse updateTeam(Long teamId, Long requestingUserId, UpdateTeamRequest request) {
        log.info("Updating team id={} by user id={}", teamId, requestingUserId);
        Team team = findTeamById(teamId);
        assertIsCaptain(team, requestingUserId, "update");

        if (request.getName() != null && !request.getName().isBlank()) {
            if (teamRepository.existsByName(request.getName()) &&
                    !team.getName().equalsIgnoreCase(request.getName())) {
                throw new BadRequestException("Team name '" + request.getName() + "' is already taken.");
            }
            team.setName(request.getName());
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }

        Team updated = teamRepository.save(team);
        return teamMapper.toTeamResponse(updated);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = {"team", "teams"}, allEntries = true)
    public void deleteTeam(Long teamId, Long requestingUserId) {
        log.info("Deleting (soft) team id={} by user id={}", teamId, requestingUserId);
        Team team = findTeamById(teamId);
        assertIsCaptain(team, requestingUserId, "delete");

        // Remove team membership from all members
        for (User member : team.getMembers()) {
            member.setTeam(null);
            userRepository.save(member);
        }

        teamRepository.delete(team); // Triggers @SQLDelete → sets deleted = true
        log.info("Team id={} soft-deleted.", teamId);
    }

    // -------------------------------------------------------------------------
    // Join / Leave
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = {"team", "teams"}, allEntries = true)
    public TeamResponse joinTeam(Long teamId, Long userId) {
        log.info("User id={} joining team id={}", userId, teamId);
        Team team = findTeamById(teamId);
        User user = findUserById(userId);

        if (user.getTeam() != null) {
            throw new BadRequestException("You are already a member of a team. Leave your current team first.");
        }

        user.setTeam(team);
        userRepository.save(user);

        Team reloaded = findTeamById(teamId);
        log.info("User id={} joined team '{}'.", userId, team.getName());
        return teamMapper.toTeamResponse(reloaded);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"team", "teams"}, allEntries = true)
    public void leaveTeam(Long teamId, Long userId) {
        log.info("User id={} leaving team id={}", userId, teamId);
        Team team = findTeamById(teamId);
        User user = findUserById(userId);

        assertIsTeamMember(team, user);

        if (isCaptain(team, userId)) {
            throw new BadRequestException(
                    "You are the captain. Transfer captaincy to another member before leaving the team.");
        }

        user.setTeam(null);
        userRepository.save(user);
        log.info("User id={} left team '{}'.", userId, team.getName());
    }

    // -------------------------------------------------------------------------
    // Captain Actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = {"team", "teams"}, allEntries = true)
    public TeamResponse transferCaptaincy(Long teamId, Long currentCaptainId, Long newCaptainId) {
        log.info("Transferring captaincy of team id={} from user id={} to user id={}",
                teamId, currentCaptainId, newCaptainId);
        Team team = findTeamById(teamId);
        assertIsCaptain(team, currentCaptainId, "transfer captaincy");

        User newCaptain = findUserById(newCaptainId);
        assertIsTeamMember(team, newCaptain);

        team.setCaptain(newCaptain);
        Team updated = teamRepository.save(team);
        log.info("Captaincy transferred to user id={} for team '{}'.", newCaptainId, team.getName());
        return teamMapper.toTeamResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"team", "teams"}, allEntries = true)
    public TeamResponse removeMember(Long teamId, Long captainId, Long memberIdToRemove) {
        log.info("Captain id={} removing member id={} from team id={}", captainId, memberIdToRemove, teamId);
        Team team = findTeamById(teamId);
        assertIsCaptain(team, captainId, "remove members from");

        if (captainId.equals(memberIdToRemove)) {
            throw new BadRequestException("Captain cannot remove themselves. Transfer captaincy first.");
        }

        User memberToRemove = findUserById(memberIdToRemove);
        assertIsTeamMember(team, memberToRemove);

        memberToRemove.setTeam(null);
        userRepository.save(memberToRemove);

        Team reloaded = findTeamById(teamId);
        log.info("Member id={} removed from team '{}'.", memberIdToRemove, team.getName());
        return teamMapper.toTeamResponse(reloaded);
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private Team findTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private boolean isCaptain(Team team, Long userId) {
        return team.getCaptain() != null && team.getCaptain().getId().equals(userId);
    }

    private void assertIsCaptain(Team team, Long userId, String action) {
        if (!isCaptain(team, userId)) {
            throw new UnauthorizedException(
                    "Only the team captain can " + action + " this team.");
        }
    }

    private void assertIsTeamMember(Team team, User user) {
        boolean isMember = team.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isMember) {
            throw new BadRequestException(
                    "User '" + user.getUsername() + "' is not a member of team '" + team.getName() + "'.");
        }
    }
}
