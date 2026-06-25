package com.gamesphere.service.impl;

import com.gamesphere.dto.request.RecordMatchResultRequest;
import com.gamesphere.dto.request.ScheduleMatchRequest;
import com.gamesphere.dto.response.MatchResponse;
import com.gamesphere.entity.Match;
import com.gamesphere.entity.Team;
import com.gamesphere.entity.Tournament;
import com.gamesphere.enums.MatchStatus;
import com.gamesphere.enums.TournamentStatus;
import com.gamesphere.exception.BadRequestException;
import com.gamesphere.exception.ResourceNotFoundException;
import com.gamesphere.mapper.MatchMapper;
import com.gamesphere.repository.MatchRepository;
import com.gamesphere.repository.TeamRepository;
import com.gamesphere.repository.TournamentRepository;
import com.gamesphere.service.MatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final MatchMapper matchMapper;

    public MatchServiceImpl(MatchRepository matchRepository,
                            TournamentRepository tournamentRepository,
                            TeamRepository teamRepository,
                            MatchMapper matchMapper) {
        this.matchRepository = matchRepository;
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.matchMapper = matchMapper;
    }

    // -------------------------------------------------------------------------
    // Schedule
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public MatchResponse scheduleMatch(ScheduleMatchRequest request) {
        log.info("Scheduling match: teamA={} vs teamB={} in tournament={}",
                request.getTeamAId(), request.getTeamBId(), request.getTournamentId());

        if (request.getTeamAId().equals(request.getTeamBId())) {
            throw new BadRequestException("A team cannot play against itself.");
        }

        Tournament tournament = findTournamentById(request.getTournamentId());
        Team teamA = findTeamById(request.getTeamAId());
        Team teamB = findTeamById(request.getTeamBId());

        if (tournament.getStatus() != TournamentStatus.ACTIVE &&
                tournament.getStatus() != TournamentStatus.UPCOMING) {
            throw new BadRequestException("Matches can only be scheduled for UPCOMING or ACTIVE tournaments.");
        }

        // Validate both teams are registered for this tournament
        boolean teamARegistered = tournament.getRegisteredTeams().stream()
                .anyMatch(t -> t.getId().equals(teamA.getId()));
        boolean teamBRegistered = tournament.getRegisteredTeams().stream()
                .anyMatch(t -> t.getId().equals(teamB.getId()));

        if (!teamARegistered) {
            throw new BadRequestException("Team '" + teamA.getName() + "' is not registered for this tournament.");
        }
        if (!teamBRegistered) {
            throw new BadRequestException("Team '" + teamB.getName() + "' is not registered for this tournament.");
        }

        Match match = Match.builder()
                .tournament(tournament)
                .teamA(teamA)
                .teamB(teamB)
                .scheduledAt(request.getScheduledAt())
                .notes(request.getNotes())
                .status(MatchStatus.SCHEDULED)
                .build();

        Match saved = matchRepository.save(match);
        log.info("Match id={} scheduled for tournament '{}'.", saved.getId(), tournament.getName());
        return matchMapper.toMatchResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public MatchResponse getMatchById(Long matchId) {
        log.debug("Fetching match id={}", matchId);
        return matchMapper.toMatchResponse(findMatchById(matchId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatchesByTournament(Long tournamentId, MatchStatus status, Pageable pageable) {
        log.debug("Fetching matches for tournament id={}, status={}", tournamentId, status);
        if (status != null) {
            return matchRepository.findByTournamentIdAndStatus(tournamentId, status, pageable)
                    .map(matchMapper::toMatchResponse);
        }
        return matchRepository.findByTournamentId(tournamentId, pageable)
                .map(matchMapper::toMatchResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatchesByTeam(Long teamId, Pageable pageable) {
        log.debug("Fetching matches for team id={}", teamId);
        return matchRepository.findByTeamId(teamId, pageable)
                .map(matchMapper::toMatchResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatchesByStatus(MatchStatus status, Pageable pageable) {
        log.debug("Fetching matches by status={}", status);
        if (status != null) {
            return matchRepository.findByStatus(status, pageable)
                    .map(matchMapper::toMatchResponse);
        }
        return matchRepository.findAll(pageable)
                .map(matchMapper::toMatchResponse);
    }

    // -------------------------------------------------------------------------
    // Record Result
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public MatchResponse recordResult(Long matchId, RecordMatchResultRequest request) {
        log.info("Recording result for match id={}: scoreA={}, scoreB={}",
                matchId, request.getTeamAScore(), request.getTeamBScore());

        Match match = findMatchById(matchId);

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new BadRequestException("Match result has already been recorded.");
        }

        match.setTeamAScore(request.getTeamAScore());
        match.setTeamBScore(request.getTeamBScore());
        match.setPlayedAt(LocalDateTime.now());
        match.setStatus(MatchStatus.COMPLETED);

        if (request.getNotes() != null) {
            match.setNotes(request.getNotes());
        }

        // Determine winner (draw keeps winner as null)
        if (request.getTeamAScore() > request.getTeamBScore()) {
            match.setWinner(match.getTeamA());
        } else if (request.getTeamBScore() > request.getTeamAScore()) {
            match.setWinner(match.getTeamB());
        } else {
            match.setWinner(null); // Draw
        }

        // Update team statistics
        updateTeamStats(match);

        Match saved = matchRepository.save(match);
        log.info("Result recorded for match id={}. Winner: {}",
                matchId, saved.getWinner() != null ? saved.getWinner().getName() : "DRAW");
        return matchMapper.toMatchResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Update Status
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public MatchResponse updateMatchStatus(Long matchId, MatchStatus newStatus) {
        log.info("Updating match id={} status to {}", matchId, newStatus);
        Match match = findMatchById(matchId);

        validateMatchStatusTransition(match.getStatus(), newStatus);
        match.setStatus(newStatus);

        if (newStatus == MatchStatus.LIVE) {
            match.setPlayedAt(LocalDateTime.now());
        }

        Match saved = matchRepository.save(match);
        return matchMapper.toMatchResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteMatch(Long matchId) {
        log.info("Soft-deleting match id={}", matchId);
        Match match = findMatchById(matchId);
        if (match.getStatus() == MatchStatus.LIVE) {
            throw new BadRequestException("Cannot delete a LIVE match.");
        }
        matchRepository.delete(match);
        log.info("Match id={} soft-deleted.", matchId);
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private Match findMatchById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
    }

    private Tournament findTournamentById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
    }

    private Team findTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));
    }

    /**
     * Enforces valid match lifecycle transitions:
     * SCHEDULED → LIVE → COMPLETED
     */
    private void validateMatchStatusTransition(MatchStatus current, MatchStatus requested) {
        boolean valid = switch (current) {
            case SCHEDULED -> requested == MatchStatus.LIVE;
            case LIVE -> requested == MatchStatus.COMPLETED;
            case COMPLETED -> false; // Terminal state
        };
        if (!valid) {
            throw new BadRequestException(
                    "Invalid match status transition from " + current + " to " + requested + ".");
        }
    }

    /**
     * Updates wins/losses and win rate for both teams after a match is completed.
     */
    private void updateTeamStats(Match match) {
        Team teamA = match.getTeamA();
        Team teamB = match.getTeamB();

        if (match.getWinner() == null) {
            // Draw — no wins or losses updated
            return;
        }

        if (match.getWinner().getId().equals(teamA.getId())) {
            teamA.setWins(teamA.getWins() + 1);
            teamB.setLosses(teamB.getLosses() + 1);
        } else {
            teamB.setWins(teamB.getWins() + 1);
            teamA.setLosses(teamA.getLosses() + 1);
        }

        recalculateWinRate(teamA);
        recalculateWinRate(teamB);

        teamRepository.save(teamA);
        teamRepository.save(teamB);
    }

    private void recalculateWinRate(Team team) {
        int total = team.getWins() + team.getLosses();
        if (total > 0) {
            team.setWinRate(Math.round(((double) team.getWins() / total) * 10000.0) / 100.0);
        } else {
            team.setWinRate(0.0);
        }
    }
}
