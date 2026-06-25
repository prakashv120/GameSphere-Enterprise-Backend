package com.gamesphere.service;

import com.gamesphere.dto.request.LoginRequest;
import com.gamesphere.dto.request.RegisterRequest;
import com.gamesphere.dto.request.UserProfileUpdateRequest;
import com.gamesphere.dto.response.AuthResponse;
import com.gamesphere.dto.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    AuthResponse registerUser(RegisterRequest registerRequest);
    AuthResponse loginUser(LoginRequest loginRequest);
    UserProfileResponse getProfile(Long id);
    UserProfileResponse updateProfile(Long id, UserProfileUpdateRequest updateRequest);
    void deleteAccount(Long id);
    Page<UserProfileResponse> getAllPlayers(Pageable pageable);
    Page<UserProfileResponse> searchPlayersByName(String name, Pageable pageable);
    Page<UserProfileResponse> filterPlayersByTeam(Long teamId, Pageable pageable);
}
