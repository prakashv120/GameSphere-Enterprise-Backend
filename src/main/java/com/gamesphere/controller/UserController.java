package com.gamesphere.controller;

import com.gamesphere.dto.request.UserProfileUpdateRequest;
import com.gamesphere.dto.response.ApiResponse;
import com.gamesphere.dto.response.UserProfileResponse;
import com.gamesphere.service.UserService;
import com.gamesphere.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.getProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.updateProfile(currentUserId, updateRequest);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.deleteAccount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getPlayers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long teamId,
            Pageable pageable) {
        Page<UserProfileResponse> result;
        if (name != null && !name.trim().isEmpty()) {
            result = userService.searchPlayersByName(name, pageable);
        } else if (teamId != null) {
            result = userService.filterPlayersByTeam(teamId, pageable);
        } else {
            result = userService.getAllPlayers(pageable);
        }
        return ResponseEntity.ok(ApiResponse.success("Players retrieved successfully", result));
    }
}
