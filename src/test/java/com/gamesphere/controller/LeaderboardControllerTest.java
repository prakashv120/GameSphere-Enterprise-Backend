package com.gamesphere.controller;

import com.gamesphere.config.SecurityConfig;
import com.gamesphere.dto.response.DashboardStatsResponse;
import com.gamesphere.dto.response.LeaderboardEntryResponse;
import com.gamesphere.security.CustomAuthenticationEntryPoint;
import com.gamesphere.security.CustomUserDetailsService;
import com.gamesphere.security.JwtTokenProvider;
import com.gamesphere.security.UserPrincipal;
import com.gamesphere.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaderboardController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class})
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaderboardService leaderboardService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private LeaderboardEntryResponse leaderboardEntry;
    private DashboardStatsResponse dashboardStats;
    private UserPrincipal adminPrincipal;
    private UserPrincipal playerPrincipal;

    @BeforeEach
    void setUp() {
        leaderboardEntry = LeaderboardEntryResponse.builder()
                .rank(1)
                .teamId(1L)
                .teamName("Alpha Squad")
                .teamTag("ALPHA")
                .wins(10)
                .losses(2)
                .winRate(83.33)
                .memberCount(5)
                .build();

        dashboardStats = DashboardStatsResponse.builder()
                .totalUsers(100)
                .totalTeams(10)
                .totalTournaments(3)
                .totalMatches(20)
                .activeTournaments(1)
                .completedMatches(15)
                .scheduledMatches(4)
                .liveMatches(1)
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
    void getTeamLeaderboard_Success() throws Exception {
        when(leaderboardService.getTeamLeaderboard(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(leaderboardEntry)));

        mockMvc.perform(get("/api/v1/leaderboard")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].teamName").value("Alpha Squad"));
    }

    @Test
    void getDashboardStats_Success() throws Exception {
        when(leaderboardService.getDashboardStats()).thenReturn(dashboardStats);

        mockMvc.perform(get("/api/v1/dashboard")
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(100));
    }

    @Test
    void getDashboardStats_ForbiddenForPlayer() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(user(playerPrincipal)))
                .andExpect(status().isForbidden());
    }
}
