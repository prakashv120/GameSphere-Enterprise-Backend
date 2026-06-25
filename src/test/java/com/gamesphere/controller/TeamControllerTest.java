package com.gamesphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesphere.config.SecurityConfig;
import com.gamesphere.dto.request.CreateTeamRequest;
import com.gamesphere.dto.response.TeamResponse;
import com.gamesphere.dto.response.TeamSummaryResponse;
import com.gamesphere.security.CustomAuthenticationEntryPoint;
import com.gamesphere.security.CustomUserDetailsService;
import com.gamesphere.security.JwtTokenProvider;
import com.gamesphere.security.UserPrincipal;
import com.gamesphere.service.TeamService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class})
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamService teamService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private TeamResponse teamResponse;
    private CreateTeamRequest createRequest;
    private UserPrincipal captainPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        createRequest = new CreateTeamRequest();
        createRequest.setName("Alpha Squad");
        createRequest.setTag("ALPHA");
        createRequest.setDescription("Pro team");

        teamResponse = TeamResponse.builder()
                .id(1L)
                .name("Alpha Squad")
                .tag("ALPHA")
                .description("Pro team")
                .memberCount(1)
                .wins(0)
                .losses(0)
                .winRate(0.0)
                .build();

        captainPrincipal = new UserPrincipal(
                10L,
                "captain",
                "captain@gamesphere.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER"))
        );

        adminPrincipal = new UserPrincipal(
                20L,
                "admin",
                "admin@gamesphere.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void createTeam_Success() throws Exception {
        when(teamService.createTeam(eq(10L), any(CreateTeamRequest.class))).thenReturn(teamResponse);

        mockMvc.perform(post("/api/v1/teams")
                        .with(csrf())
                        .with(user(captainPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Alpha Squad"));
    }

    @Test
    void createTeam_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTeamById_Success() throws Exception {
        when(teamService.getTeamById(1L)).thenReturn(teamResponse);

        mockMvc.perform(get("/api/v1/teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Alpha Squad"));
    }

    @Test
    void getAllTeams_Success() throws Exception {
        TeamSummaryResponse summary = TeamSummaryResponse.builder()
                .id(1L)
                .name("Alpha Squad")
                .tag("ALPHA")
                .memberCount(1)
                .wins(0)
                .losses(0)
                .winRate(0.0)
                .build();

        when(teamService.getAllTeams(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(summary)));

        mockMvc.perform(get("/api/v1/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Alpha Squad"));
    }

    @Test
    void joinTeam_Success() throws Exception {
        teamResponse.setMemberCount(2);
        when(teamService.joinTeam(eq(1L), eq(10L))).thenReturn(teamResponse);

        mockMvc.perform(post("/api/v1/teams/1/join")
                        .with(csrf())
                        .with(user(captainPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberCount").value(2));
    }

    @Test
    void deleteTeam_Admin_Success() throws Exception {
        when(teamService.getTeamById(1L)).thenReturn(teamResponse);

        // Try deleting as PLAYER role (captainPrincipal) -> should get 403 Forbidden
        mockMvc.perform(delete("/api/v1/teams/1/admin")
                        .with(csrf())
                        .with(user(captainPrincipal)))
                .andExpect(status().isForbidden());
    }
}
