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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Spy
    private MatchMapper matchMapper = new MatchMapper();

    @InjectMocks
    private MatchServiceImpl matchService;

    private Tournament tournament;
    private Team teamA;
    private Team teamB;
    private Match match;
    private ScheduleMatchRequest scheduleRequest;

    @BeforeEach
    void setUp() {
        teamA = Team.builder().id(10L).name("Alpha Squad").tag("ALPHA").wins(0).losses(0).winRate(0.0).build();
        teamB = Team.builder().id(20L).name("Beta Squad").tag("BETA").wins(0).losses(0).winRate(0.0).build();

        tournament = Tournament.builder()
                .id(1L)
                .name("Grand Championship")
                .status(TournamentStatus.ACTIVE)
                .registeredTeams(new ArrayList<>())
                .build();
        tournament.getRegisteredTeams().add(teamA);
        tournament.getRegisteredTeams().add(teamB);

        match = Match.builder()
                .id(100L)
                .tournament(tournament)
                .teamA(teamA)
                .teamB(teamB)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .status(MatchStatus.SCHEDULED)
                .build();

        scheduleRequest = new ScheduleMatchRequest();
        scheduleRequest.setTournamentId(1L);
        scheduleRequest.setTeamAId(10L);
        scheduleRequest.setTeamBId(20L);
        scheduleRequest.setScheduledAt(LocalDateTime.now().plusDays(1));
    }

    @Test
    void scheduleMatch_Success() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(teamA));
        when(teamRepository.findById(20L)).thenReturn(Optional.of(teamB));
        when(matchRepository.save(any(Match.class))).thenReturn(match);

        MatchResponse response = matchService.scheduleMatch(scheduleRequest);

        assertNotNull(response);
        assertEquals("Alpha Squad", response.getTeamA().getName());
        assertEquals("Beta Squad", response.getTeamB().getName());
        verify(matchRepository, times(1)).save(any(Match.class));
    }

    @Test
    void scheduleMatch_SelfPlay_ThrowsException() {
        scheduleRequest.setTeamBId(10L); // Team A plays itself

        assertThrows(BadRequestException.class, () -> matchService.scheduleMatch(scheduleRequest));
    }

    @Test
    void recordResult_Success() {
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecordMatchResultRequest req = new RecordMatchResultRequest();
        req.setTeamAScore(3);
        req.setTeamBScore(1);
        req.setNotes("Clean win");

        MatchResponse response = matchService.recordResult(100L, req);

        assertNotNull(response);
        assertEquals(MatchStatus.COMPLETED, response.getStatus());
        assertEquals(10L, response.getWinner().getId()); // Team A wins
        assertEquals(1, teamA.getWins());
        assertEquals(1, teamB.getLosses());
        assertEquals(100.0, teamA.getWinRate());
        assertEquals(0.0, teamB.getWinRate());
    }

    @Test
    void updateMatchStatus_Success() {
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.updateMatchStatus(100L, MatchStatus.LIVE);

        assertNotNull(response);
        assertEquals(MatchStatus.LIVE, response.getStatus());
    }

    @Test
    void deleteMatch_Success() {
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));

        matchService.deleteMatch(100L);

        verify(matchRepository, times(1)).delete(match);
    }
}
