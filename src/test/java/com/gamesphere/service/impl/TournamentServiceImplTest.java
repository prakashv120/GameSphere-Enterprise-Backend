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
import com.gamesphere.mapper.TeamMapper;
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
class TournamentServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Spy
    private TournamentMapper tournamentMapper = new TournamentMapper(new TeamMapper());

    @InjectMocks
    private TournamentServiceImpl tournamentService;

    private Tournament tournament;
    private Team team;
    private CreateTournamentRequest createRequest;

    @BeforeEach
    void setUp() {
        tournament = Tournament.builder()
                .id(1L)
                .name("Grand Championship")
                .description("Big tournament")
                .maxTeams(8)
                .prizePool(10000.0)
                .startDate(LocalDateTime.now().plusDays(2))
                .endDate(LocalDateTime.now().plusDays(10))
                .status(TournamentStatus.UPCOMING)
                .registeredTeams(new ArrayList<>())
                .build();

        team = Team.builder()
                .id(10L)
                .name("Alpha Squad")
                .tag("ALPHA")
                .build();

        createRequest = new CreateTournamentRequest();
        createRequest.setName("Grand Championship");
        createRequest.setDescription("Big tournament");
        createRequest.setMaxTeams(8);
        createRequest.setPrizePool(10000.0);
        createRequest.setStartDate(LocalDateTime.now().plusDays(2));
        createRequest.setEndDate(LocalDateTime.now().plusDays(10));
    }

    @Test
    void createTournament_Success() {
        when(tournamentRepository.existsByName("Grand Championship")).thenReturn(false);
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

        TournamentResponse response = tournamentService.createTournament(createRequest);

        assertNotNull(response);
        assertEquals("Grand Championship", response.getName());
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }

    @Test
    void createTournament_NameTaken_ThrowsException() {
        when(tournamentRepository.existsByName("Grand Championship")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> tournamentService.createTournament(createRequest));
        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void registerTeam_Success() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(tournamentRepository.isTeamRegistered(1L, 10L)).thenReturn(false);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentResponse response = tournamentService.registerTeam(1L, 10L);

        assertNotNull(response);
        assertEquals(1, response.getRegisteredTeamsCount());
        verify(tournamentRepository, times(1)).save(tournament);
    }

    @Test
    void registerTeam_ActiveTournament_ThrowsException() {
        tournament.setStatus(TournamentStatus.ACTIVE);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        assertThrows(BadRequestException.class, () -> tournamentService.registerTeam(1L, 10L));
    }

    @Test
    void updateStatus_Success() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentResponse response = tournamentService.updateStatus(1L, TournamentStatus.ACTIVE);

        assertNotNull(response);
        assertEquals(TournamentStatus.ACTIVE, response.getStatus());
    }

    @Test
    void deleteTournament_Success() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        tournamentService.deleteTournament(1L);

        verify(tournamentRepository, times(1)).delete(tournament);
    }

    @Test
    void deleteTournament_Active_ThrowsException() {
        tournament.setStatus(TournamentStatus.ACTIVE);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThrows(BadRequestException.class, () -> tournamentService.deleteTournament(1L));
        verify(tournamentRepository, never()).delete(any());
    }
}
