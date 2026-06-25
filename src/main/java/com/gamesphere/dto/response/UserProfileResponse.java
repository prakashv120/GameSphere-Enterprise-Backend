package com.gamesphere.dto.response;

import com.gamesphere.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private String fullName;
    private String bio;
    private String avatarUrl;
    private String phone;
    private int wins;
    private int losses;
    private double winRate;
    private Long teamId;
    private String teamName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
