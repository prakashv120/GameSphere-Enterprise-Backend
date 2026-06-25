package com.gamesphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesphere.config.SecurityConfig;
import com.gamesphere.dto.request.LoginRequest;
import com.gamesphere.dto.request.RegisterRequest;
import com.gamesphere.dto.response.AuthResponse;
import com.gamesphere.enums.Role;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class})
class AuthControllerTest {

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

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testplayer");
        registerRequest.setEmail("test@gamesphere.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.PLAYER);

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testplayer");
        loginRequest.setPassword("password123");

        authResponse = AuthResponse.builder()
                .token("test_token")
                .id(1L)
                .username("testplayer")
                .email("test@gamesphere.com")
                .role(Role.PLAYER)
                .build();

        userPrincipal = new UserPrincipal(
                1L,
                "testplayer",
                "test@gamesphere.com",
                "password123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER"))
        );
    }

    @Test
    void register_Success() throws Exception {
        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.token").value("test_token"));
    }

    @Test
    void login_Success() throws Exception {
        when(userService.loginUser(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("test_token"));
    }

    @Test
    void logout_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully logged out"));
    }
}
