package com.gamesphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesphere.config.SecurityConfig;
import com.gamesphere.dto.request.RecordMatchResultRequest;
import com.gamesphere.dto.request.ScheduleMatchRequest;
import com.gamesphere.dto.response.MatchResponse;
import com.gamesphere.enums.MatchStatus;
import com.gamesphere.security.CustomAuthenticationEntryPoint;
import com.gamesphere.security.CustomUserDetailsService;
import com.gamesphere.security.JwtTokenProvider;
import com.gamesphere.security.UserPrincipal;
import com.gamesphere.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class})
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchService matchService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private MatchResponse matchResponse;
    private ScheduleMatchRequest scheduleRequest;
    private RecordMatchResultRequest resultRequest;
    private UserPrincipal adminPrincipal;
    private UserPrincipal playerPrincipal;

    @BeforeEach
    void setUp() {
        scheduleRequest = ScheduleMatchRequest.builder()
                .tournamentId(1L)
                .teamAId(10L)
                .teamBId(11L)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .notes("Grand finals")
                .build();

        resultRequest = RecordMatchResultRequest.builder()
                .teamAScore(3)
                .teamBScore(1)
                .notes("Team A wins 3-1")
                .build();

        MatchResponse.TeamInfo teamA = MatchResponse.TeamInfo.builder().id(10L).name("Alpha").tag("ALP").build();
        MatchResponse.TeamInfo teamB = MatchResponse.TeamInfo.builder().id(11L).name("Beta").tag("BET").build();

        matchResponse = MatchResponse.builder()
                .id(100L)
                .tournamentId(1L)
                .tournamentName("Winter Clash")
                .teamA(teamA)
                .teamB(teamB)
                .status(MatchStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .build();

        adminPrincipal = new UserPrincipal(
                1L,
                "admin",
                "admin@gamesphere.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        playerPrincipal = new UserPrincipal(
                2L,
                "player",
                "player@gamesphere.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER"))
        );
    }

    @Test
    void scheduleMatch_Success() throws Exception {
        when(matchService.scheduleMatch(any(ScheduleMatchRequest.class))).thenReturn(matchResponse);

        mockMvc.perform(post("/api/v1/matches")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    void scheduleMatch_ForbiddenForPlayer() throws Exception {
        mockMvc.perform(post("/api/v1/matches")
                        .with(csrf())
                        .with(user(playerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMatchById_Success() throws Exception {
        when(matchService.getMatchById(100L)).thenReturn(matchResponse);

        mockMvc.perform(get("/api/v1/matches/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    void getMatchesByStatus_Success() throws Exception {
        when(matchService.getMatchesByStatus(eq(MatchStatus.SCHEDULED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(matchResponse)));

        mockMvc.perform(get("/api/v1/matches")
                        .param("status", "SCHEDULED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(100));
    }

    @Test
    void getMatchesByTournament_Success() throws Exception {
        when(matchService.getMatchesByTournament(eq(1L), eq(MatchStatus.SCHEDULED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(matchResponse)));

        mockMvc.perform(get("/api/v1/matches/tournament/1")
                        .param("status", "SCHEDULED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(100));
    }

    @Test
    void getMatchesByTeam_Success() throws Exception {
        when(matchService.getMatchesByTeam(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(matchResponse)));

        mockMvc.perform(get("/api/v1/matches/team/10")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(100));
    }

    @Test
    void recordResult_Success() throws Exception {
        matchResponse.setStatus(MatchStatus.COMPLETED);
        matchResponse.setTeamAScore(3);
        matchResponse.setTeamBScore(1);
        matchResponse.setWinner(matchResponse.getTeamA());

        when(matchService.recordResult(eq(100L), any(RecordMatchResultRequest.class))).thenReturn(matchResponse);

        mockMvc.perform(post("/api/v1/matches/100/result")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resultRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.teamAScore").value(3));
    }

    @Test
    void updateMatchStatus_Success() throws Exception {
        matchResponse.setStatus(MatchStatus.LIVE);
        when(matchService.updateMatchStatus(eq(100L), eq(MatchStatus.LIVE))).thenReturn(matchResponse);

        mockMvc.perform(patch("/api/v1/matches/100/status")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .param("status", "LIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("LIVE"));
    }

    @Test
    void deleteMatch_Success() throws Exception {
        doNothing().when(matchService).deleteMatch(100L);

        mockMvc.perform(delete("/api/v1/matches/100")
                        .with(csrf())
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Match deleted successfully."));
    }
}
