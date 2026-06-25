package com.gamesphere.service.impl;

import com.gamesphere.dto.request.LoginRequest;
import com.gamesphere.dto.request.RegisterRequest;
import com.gamesphere.dto.request.UserProfileUpdateRequest;
import com.gamesphere.dto.response.AuthResponse;
import com.gamesphere.dto.response.UserProfileResponse;
import com.gamesphere.entity.User;
import com.gamesphere.entity.UserProfile;
import com.gamesphere.enums.Role;
import com.gamesphere.exception.BadRequestException;
import com.gamesphere.exception.ResourceNotFoundException;
import com.gamesphere.repository.UserProfileRepository;
import com.gamesphere.repository.UserRepository;
import com.gamesphere.security.JwtTokenProvider;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

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

        user = User.builder()
                .id(1L)
                .username("testplayer")
                .email("test@gamesphere.com")
                .password("encoded_password")
                .role(Role.PLAYER)
                .build();
        user.setUserProfile(UserProfile.builder()
                .user(user)
                .fullName("Test Player")
                .bio("Esports Enthusiast")
                .avatarUrl("")
                .phone("")
                .wins(0)
                .losses(0)
                .winRate(0.0)
                .build());
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenProvider.generateToken(any())).thenReturn("jwt_token");

        AuthResponse response = userService.registerUser(registerRequest);

        assertNotNull(response);
        assertEquals("testplayer", response.getUsername());
        assertEquals("jwt_token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_UsernameTaken_ThrowsException() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.registerUser(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_EmailTaken_ThrowsException() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.registerUser(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfile(1L);

        assertNotNull(response);
        assertEquals("testplayer", response.getUsername());
        assertEquals("Test Player", response.getFullName());
    }

    @Test
    void getProfile_NotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(1L));
    }

    @Test
    void updateProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileUpdateRequest updateReq = new UserProfileUpdateRequest();
        updateReq.setFullName("New Name");
        updateReq.setBio("New Bio");

        UserProfileResponse response = userService.updateProfile(1L, updateReq);

        assertNotNull(response);
        assertEquals("New Name", response.getFullName());
        assertEquals("New Bio", response.getBio());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void deleteAccount_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteAccount(1L);

        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void getAllPlayers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> usersPage = new PageImpl<>(Collections.singletonList(user));
        when(userRepository.findByRole(Role.PLAYER, pageable)).thenReturn(usersPage);

        Page<UserProfileResponse> result = userService.getAllPlayers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testplayer", result.getContent().get(0).getUsername());
    }
}
