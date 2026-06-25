package com.gamesphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesphere.config.SecurityConfig;
import com.gamesphere.dto.request.CreateTournamentRequest;
import com.gamesphere.dto.request.UpdateTournamentRequest;
import com.gamesphere.dto.response.TournamentResponse;
import com.gamesphere.dto.response.TournamentSummaryResponse;
import com.gamesphere.enums.TournamentStatus;
import com.gamesphere.security.CustomAuthenticationEntryPoint;
import com.gamesphere.security.CustomUserDetailsService;
import com.gamesphere.security.JwtTokenProvider;
import com.gamesphere.security.UserPrincipal;
import com.gamesphere.service.TournamentService;
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

@WebMvcTest(TournamentController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class})
class TournamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TournamentService tournamentService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private TournamentResponse tournamentResponse;
    private TournamentSummaryResponse summaryResponse;
    private CreateTournamentRequest createRequest;
    private UpdateTournamentRequest updateRequest;
    private UserPrincipal adminPrincipal;
    private UserPrincipal playerPrincipal;

    @BeforeEach
    void setUp() {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(2);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(5);

        createRequest = CreateTournamentRequest.builder()
                .name("Winter Clash")
                .description("Winter tournament")
                .maxTeams(16)
                .prizePool(5000.0)
                .startDate(futureStart)
                .endDate(futureEnd)
                .build();

        updateRequest = UpdateTournamentRequest.builder()
                .name("Winter Clash v2")
                .description("Updated description")
                .prizePool(6000.0)
                .startDate(futureStart)
                .endDate(futureEnd)
                .status(TournamentStatus.ACTIVE)
                .build();

        tournamentResponse = TournamentResponse.builder()
                .id(1L)
                .name("Winter Clash")
                .description("Winter tournament")
                .status(TournamentStatus.UPCOMING)
                .maxTeams(16)
                .registeredTeamsCount(0)
                .prizePool(5000.0)
                .startDate(futureStart)
                .endDate(futureEnd)
                .build();

        summaryResponse = TournamentSummaryResponse.builder()
                .id(1L)
                .name("Winter Clash")
                .status(TournamentStatus.UPCOMING)
                .maxTeams(16)
                .registeredTeamsCount(0)
                .prizePool(5000.0)
                .startDate(futureStart)
                .endDate(futureEnd)
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
    void createTournament_Success() throws Exception {
        when(tournamentService.createTournament(any(CreateTournamentRequest.class))).thenReturn(tournamentResponse);

        mockMvc.perform(post("/api/v1/tournaments")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Winter Clash"));
    }

    @Test
    void createTournament_ForbiddenForPlayer() throws Exception {
        mockMvc.perform(post("/api/v1/tournaments")
                        .with(csrf())
                        .with(user(playerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTournamentById_Success() throws Exception {
        when(tournamentService.getTournamentById(1L)).thenReturn(tournamentResponse);

        mockMvc.perform(get("/api/v1/tournaments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Winter Clash"));
    }

    @Test
    void getAllTournaments_Success() throws Exception {
        when(tournamentService.getAllTournaments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(summaryResponse)));

        mockMvc.perform(get("/api/v1/tournaments")
                        .param("status", "UPCOMING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Winter Clash"));
    }

    @Test
    void searchTournaments_Success() throws Exception {
        when(tournamentService.searchTournamentsByName(eq("Winter"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(summaryResponse)));

        mockMvc.perform(get("/api/v1/tournaments/search")
                        .param("name", "Winter")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Winter Clash"));
    }

    @Test
    void updateTournament_Success() throws Exception {
        tournamentResponse.setName("Winter Clash v2");
        when(tournamentService.updateTournament(eq(1L), any(UpdateTournamentRequest.class)))
                .thenReturn(tournamentResponse);

        mockMvc.perform(put("/api/v1/tournaments/1")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Winter Clash v2"));
    }

    @Test
    void updateStatus_Success() throws Exception {
        tournamentResponse.setStatus(TournamentStatus.ACTIVE);
        when(tournamentService.updateStatus(eq(1L), eq(TournamentStatus.ACTIVE)))
                .thenReturn(tournamentResponse);

        mockMvc.perform(patch("/api/v1/tournaments/1/status")
                        .with(csrf())
                        .with(user(adminPrincipal))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void deleteTournament_Success() throws Exception {
        doNothing().when(tournamentService).deleteTournament(1L);

        mockMvc.perform(delete("/api/v1/tournaments/1")
                        .with(csrf())
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tournament deleted successfully."));
    }

    @Test
    void registerTeam_Success() throws Exception {
        tournamentResponse.setRegisteredTeamsCount(1);
        when(tournamentService.registerTeam(eq(1L), eq(10L))).thenReturn(tournamentResponse);

        mockMvc.perform(post("/api/v1/tournaments/1/register")
                        .with(csrf())
                        .with(user(playerPrincipal))
                        .param("teamId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registeredTeamsCount").value(1));
    }

    @Test
    void deregisterTeam_Success() throws Exception {
        when(tournamentService.deregisterTeam(eq(1L), eq(10L))).thenReturn(tournamentResponse);

        mockMvc.perform(delete("/api/v1/tournaments/1/deregister")
                        .with(csrf())
                        .with(user(playerPrincipal))
                        .param("teamId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
