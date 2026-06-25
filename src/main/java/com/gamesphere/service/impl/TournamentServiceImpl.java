package com.gamesphere.service.impl;

import com.gamesphere.dto.request.CreateTournamentRequest;
import com.gamesphere.dto.request.UpdateTournamentRequest;
import com.gamesphere.dto.response.TournamentResponse;
import com.gamesphere.dto.response.TournamentSummaryResponse;
import com.gamesphere.entity.Team;
import com.gamesphere.entity.Tournament;
import com.gamesphere.enums.TournamentStatus;
import com.gamesphere.exception.BadRequestException;
import com.gamesphere.exception.ResourceNotFoundException;
import com.gamesphere.mapper.TournamentMapper;
import com.gamesphere.repository.TeamRepository;
import com.gamesphere.repository.TournamentRepository;
import com.gamesphere.service.TournamentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final TournamentMapper tournamentMapper;

    public TournamentServiceImpl(TournamentRepository tournamentRepository,
                                 TeamRepository teamRepository,
                                 TournamentMapper tournamentMapper) {
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.tournamentMapper = tournamentMapper;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = "tournaments", allEntries = true)
    public TournamentResponse createTournament(CreateTournamentRequest request) {
        log.info("Creating tournament '{}'", request.getName());

        if (tournamentRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tournament name '" + request.getName() + "' is already taken.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date.");
        }

        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .description(request.getDescription())
                .maxTeams(request.getMaxTeams())
                .prizePool(request.getPrizePool())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(TournamentStatus.UPCOMING)
                .build();

        Tournament saved = tournamentRepository.save(tournament);
        log.info("Tournament '{}' created with id={}", saved.getName(), saved.getId());
        return tournamentMapper.toTournamentResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "tournament", key = "#tournamentId")
    public TournamentResponse getTournamentById(Long tournamentId) {
        log.debug("Fetching tournament id={}", tournamentId);
        return tournamentMapper.toTournamentResponse(findTournamentById(tournamentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TournamentSummaryResponse> getAllTournaments(TournamentStatus status, Pageable pageable) {
        log.debug("Fetching all tournaments, status={}", status);
        if (status != null) {
            return tournamentRepository.findByStatus(status, pageable)
                    .map(tournamentMapper::toTournamentSummaryResponse);
        }
        return tournamentRepository.findAll(pageable)
                .map(tournamentMapper::toTournamentSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TournamentSummaryResponse> searchTournamentsByName(String name, Pageable pageable) {
        log.debug("Searching tournaments by name='{}'", name);
        return tournamentRepository.searchByName(name, pageable)
                .map(tournamentMapper::toTournamentSummaryResponse);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = {"tournament", "tournaments"}, allEntries = true)
    public TournamentResponse updateTournament(Long tournamentId, UpdateTournamentRequest request) {
        log.info("Updating tournament id={}", tournamentId);
        Tournament tournament = findTournamentById(tournamentId);

        if (request.getName() != null && !request.getName().isBlank()) {
            if (tournamentRepository.existsByName(request.getName()) &&
                    !tournament.getName().equalsIgnoreCase(request.getName())) {
                throw new BadRequestException("Tournament name '" + request.getName() + "' is already taken.");
            }
            tournament.setName(request.getName());
        }
        if (request.getDescription() != null) {
            tournament.setDescription(request.getDescription());
        }
        if (request.getPrizePool() != null) {
            tournament.setPrizePool(request.getPrizePool());
        }
        if (request.getStartDate() != null) {
            tournament.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            tournament.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            validateStatusTransition(tournament.getStatus(), request.getStatus());
            tournament.setStatus(request.getStatus());
        }

        Tournament updated = tournamentRepository.save(tournament);
        return tournamentMapper.toTournamentResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"tournament", "tournaments"}, allEntries = true)
    public TournamentResponse updateStatus(Long tournamentId, TournamentStatus newStatus) {
        log.info("Updating status of tournament id={} to {}", tournamentId, newStatus);
        Tournament tournament = findTournamentById(tournamentId);
        validateStatusTransition(tournament.getStatus(), newStatus);
        tournament.setStatus(newStatus);
        return tournamentMapper.toTournamentResponse(tournamentRepository.save(tournament));
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = {"tournament", "tournaments"}, allEntries = true)
    public void deleteTournament(Long tournamentId) {
        log.info("Soft-deleting tournament id={}", tournamentId);
        Tournament tournament = findTournamentById(tournamentId);
        if (tournament.getStatus() == TournamentStatus.ACTIVE) {
            throw new BadRequestException("Cannot delete an active tournament. Complete or cancel it first.");
        }
        tournamentRepository.delete(tournament);
        log.info("Tournament id={} soft-deleted.", tournamentId);
    }

    // -------------------------------------------------------------------------
    // Team Registration
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = {"tournament", "tournaments"}, allEntries = true)
    public TournamentResponse registerTeam(Long tournamentId, Long teamId) {
        log.info("Registering team id={} for tournament id={}", teamId, tournamentId);
        Tournament tournament = findTournamentById(tournamentId);
        Team team = findTeamById(teamId);

        if (tournament.getStatus() != TournamentStatus.UPCOMING) {
            throw new BadRequestException("Teams can only register for UPCOMING tournaments.");
        }
        if (tournament.getRegisteredTeams().size() >= tournament.getMaxTeams()) {
            throw new BadRequestException("Tournament is full. Maximum " + tournament.getMaxTeams() + " teams allowed.");
        }
        if (tournamentRepository.isTeamRegistered(tournamentId, teamId)) {
            throw new BadRequestException("Team '" + team.getName() + "' is already registered for this tournament.");
        }

        tournament.getRegisteredTeams().add(team);
        Tournament updated = tournamentRepository.save(tournament);
        log.info("Team id={} registered for tournament id={}", teamId, tournamentId);
        return tournamentMapper.toTournamentResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"tournament", "tournaments"}, allEntries = true)
    public TournamentResponse deregisterTeam(Long tournamentId, Long teamId) {
        log.info("Deregistering team id={} from tournament id={}", teamId, tournamentId);
        Tournament tournament = findTournamentById(tournamentId);
        Team team = findTeamById(teamId);

        if (tournament.getStatus() != TournamentStatus.UPCOMING) {
            throw new BadRequestException("Teams can only be deregistered from UPCOMING tournaments.");
        }
        boolean removed = tournament.getRegisteredTeams().removeIf(t -> t.getId().equals(teamId));
        if (!removed) {
            throw new BadRequestException("Team '" + team.getName() + "' is not registered for this tournament.");
        }

        Tournament updated = tournamentRepository.save(tournament);
        log.info("Team id={} deregistered from tournament id={}", teamId, tournamentId);
        return tournamentMapper.toTournamentResponse(updated);
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private Tournament findTournamentById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
    }

    private Team findTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));
    }

    /**
     * Enforces valid tournament lifecycle transitions:
     * UPCOMING → ACTIVE → COMPLETED
     */
    private void validateStatusTransition(TournamentStatus current, TournamentStatus requested) {
        boolean valid = switch (current) {
            case UPCOMING -> requested == TournamentStatus.ACTIVE;
            case ACTIVE -> requested == TournamentStatus.COMPLETED;
            case COMPLETED -> false; // Terminal state
        };
        if (!valid) {
            throw new BadRequestException(
                    "Invalid status transition from " + current + " to " + requested + ".");
        }
    }
}
