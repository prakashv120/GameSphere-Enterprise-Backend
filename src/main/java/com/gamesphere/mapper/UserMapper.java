package com.gamesphere.mapper;

import com.gamesphere.dto.response.UserProfileResponse;
import com.gamesphere.entity.User;
import com.gamesphere.entity.UserProfile;

public class UserMapper {

    public static UserProfileResponse toProfileResponse(User user) {
        if (user == null) {
            return null;
        }

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole());

        if (user.getTeam() != null) {
            builder.teamId(user.getTeam().getId())
                    .teamName(user.getTeam().getName());
        }

        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            builder.fullName(profile.getFullName())
                    .bio(profile.getBio())
                    .avatarUrl(profile.getAvatarUrl())
                    .phone(profile.getPhone())
                    .wins(profile.getWins())
                    .losses(profile.getLosses())
                    .winRate(profile.getWinRate())
                    .createdAt(profile.getCreatedAt())
                    .updatedAt(profile.getUpdatedAt());
        } else {
            builder.createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt());
        }

        return builder.build();
    }
}
