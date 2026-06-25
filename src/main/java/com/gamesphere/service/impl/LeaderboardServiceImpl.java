package com.gamesphere.service.impl;

import com.gamesphere.dto.response.DashboardStatsResponse;
import com.gamesphere.dto.response.LeaderboardEntryResponse;
import com.gamesphere.entity.Team;
import com.gamesphere.enums.MatchStatus;
import com.gamesphere.enums.TournamentStatus;
import com.gamesphere.repository.MatchRepository;
import com.gamesphere.repository.TeamRepository;
import com.gamesphere.repository.TournamentRepository;
import com.gamesphere.repository.UserRepository;
import com.gamesphere.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;

    public LeaderboardServiceImpl(TeamRepository teamRepository,
                                  UserRepository userRepository,
                                  TournamentRepository tournamentRepository,
                                  MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
    }

    // -------------------------------------------------------------------------
    // Leaderboard
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "leaderboard", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<LeaderboardEntryResponse> getTeamLeaderboard(Pageable pageable) {
        log.debug("Fetching team leaderboard, page={}", pageable.getPageNumber());

        Page<Team> teamsPage = teamRepository.findAllOrderByWinRateDesc(pageable);

        // Calculate rank offset for current page
        int rankOffset = pageable.getPageNumber() * pageable.getPageSize() + 1;
        AtomicInteger rankCounter = new AtomicInteger(rankOffset);

        List<LeaderboardEntryResponse> entries = teamsPage.getContent().stream()
                .map(team -> LeaderboardEntryResponse.builder()
                        .rank(rankCounter.getAndIncrement())
                        .teamId(team.getId())
                        .teamName(team.getName())
                        .teamTag(team.getTag())
                        .wins(team.getWins())
                        .losses(team.getLosses())
                        .winRate(team.getWinRate())
                        .memberCount(team.getMembers() != null ? team.getMembers().size() : 0)
                        .build())
                .toList();

        return new PageImpl<>(entries, pageable, teamsPage.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // Dashboard Stats
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard-stats")
    public DashboardStatsResponse getDashboardStats() {
        log.debug("Fetching dashboard stats");

        long totalUsers = userRepository.count();
        long totalTeams = teamRepository.count();
        long totalTournaments = tournamentRepository.count();
        long totalMatches = matchRepository.count();
        long activeTournaments = tournamentRepository.findByStatus(TournamentStatus.ACTIVE,
                Pageable.unpaged()).getTotalElements();
        long completedMatches = matchRepository.findByStatus(MatchStatus.COMPLETED,
                Pageable.unpaged()).getTotalElements();
        long scheduledMatches = matchRepository.findByStatus(MatchStatus.SCHEDULED,
                Pageable.unpaged()).getTotalElements();
        long liveMatches = matchRepository.findByStatus(MatchStatus.LIVE,
                Pageable.unpaged()).getTotalElements();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalTeams(totalTeams)
                .totalTournaments(totalTournaments)
                .totalMatches(totalMatches)
                .activeTournaments(activeTournaments)
                .completedMatches(completedMatches)
                .scheduledMatches(scheduledMatches)
                .liveMatches(liveMatches)
                .build();
    }
}
