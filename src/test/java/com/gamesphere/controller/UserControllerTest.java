package com.gamesphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesphere.config.SecurityConfig;
import com.gamesphere.dto.request.UserProfileUpdateRequest;
import com.gamesphere.dto.response.UserProfileResponse;
import com.gamesphere.security.CustomAuthenticationEntryPoint;
import com.gamesphere.security.CustomUserDetailsService;
import com.gamesphere.security.JwtTokenProvider;
import com.gamesphere.security.UserPrincipal;
import com.gamesphere.service.UserService;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private UserProfileResponse profileResponse;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        profileResponse = UserProfileResponse.builder()
                .id(1L)
                .username("testplayer")
                .email("test@gamesphere.com")
                .fullName("Test Player")
                .bio("Gamer")
                .build();

        userPrincipal = new UserPrincipal(
                1L,
                "testplayer",
                "test@gamesphere.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER"))
        );
    }

    @Test
    void getProfile_Success() throws Exception {
        when(userService.getProfile(any())).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/users/profile")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
                .andExpect(jsonPath("$.data.username").value("testplayer"));
    }

    @Test
    void updateProfile_Success() throws Exception {
        UserProfileUpdateRequest updateReq = new UserProfileUpdateRequest();
        updateReq.setFullName("New Name");
        updateReq.setBio("New Bio");

        profileResponse.setFullName("New Name");
        profileResponse.setBio("New Bio");

        when(userService.updateProfile(any(), any(UserProfileUpdateRequest.class))).thenReturn(profileResponse);

        mockMvc.perform(put("/api/v1/users/profile")
                        .with(csrf())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("New Name"));
    }

    @Test
    void deleteAccount_Success() throws Exception {
        doNothing().when(userService).deleteAccount(any());

        mockMvc.perform(delete("/api/v1/users/account")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));
    }

    @Test
    void getPlayers_Success() throws Exception {
        when(userService.getAllPlayers(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(profileResponse)));

        mockMvc.perform(get("/api/v1/users")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("testplayer"));
    }
}
