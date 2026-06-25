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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private LeaderboardServiceImpl leaderboardService;

    private Team team;

    @BeforeEach
    void setUp() {
        team = Team.builder()
                .id(1L)
                .name("Alpha Squad")
                .tag("ALPHA")
                .wins(5)
                .losses(1)
                .winRate(83.33)
                .build();
    }

    @Test
    void getTeamLeaderboard_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Team> teamPage = new PageImpl<>(Collections.singletonList(team));
        when(teamRepository.findAllOrderByWinRateDesc(pageable)).thenReturn(teamPage);

        Page<LeaderboardEntryResponse> leaderboard = leaderboardService.getTeamLeaderboard(pageable);

        assertNotNull(leaderboard);
        assertEquals(1, leaderboard.getTotalElements());
        assertEquals("Alpha Squad", leaderboard.getContent().get(0).getTeamName());
        assertEquals(1, leaderboard.getContent().get(0).getRank());
    }

    @Test
    void getDashboardStats_Success() {
        when(userRepository.count()).thenReturn(15L);
        when(teamRepository.count()).thenReturn(4L);
        when(tournamentRepository.count()).thenReturn(2L);
        when(matchRepository.count()).thenReturn(10L);

        // Active tournaments check
        Page<com.gamesphere.entity.Tournament> emptyTournamentPage = new PageImpl<>(Collections.emptyList());
        when(tournamentRepository.findByStatus(eq(TournamentStatus.ACTIVE), any(Pageable.class))).thenReturn(emptyTournamentPage);

        // Match status checks
        Page<com.gamesphere.entity.Match> emptyMatchPage = new PageImpl<>(Collections.emptyList());
        when(matchRepository.findByStatus(eq(MatchStatus.COMPLETED), any(Pageable.class))).thenReturn(emptyMatchPage);
        when(matchRepository.findByStatus(eq(MatchStatus.SCHEDULED), any(Pageable.class))).thenReturn(emptyMatchPage);
        when(matchRepository.findByStatus(eq(MatchStatus.LIVE), any(Pageable.class))).thenReturn(emptyMatchPage);

        DashboardStatsResponse stats = leaderboardService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(15L, stats.getTotalUsers());
        assertEquals(4L, stats.getTotalTeams());
        assertEquals(2L, stats.getTotalTournaments());
        assertEquals(10L, stats.getTotalMatches());
    }
}
