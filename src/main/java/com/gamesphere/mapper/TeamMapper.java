package com.gamesphere.mapper;

import com.gamesphere.dto.response.TeamResponse;
import com.gamesphere.dto.response.TeamSummaryResponse;
import com.gamesphere.entity.Team;
import com.gamesphere.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeamMapper {

    /**
     * Maps a Team entity to the full TeamResponse DTO (includes captain and member list).
     */
    public TeamResponse toTeamResponse(Team team) {
        TeamResponse.CaptainInfo captainInfo = null;
        if (team.getCaptain() != null) {
            User captain = team.getCaptain();
            captainInfo = TeamResponse.CaptainInfo.builder()
                    .id(captain.getId())
                    .username(captain.getUsername())
                    .email(captain.getEmail())
                    .build();
        }

        List<TeamResponse.MemberInfo> memberInfos = team.getMembers().stream()
                .map(member -> TeamResponse.MemberInfo.builder()
                        .id(member.getId())
                        .username(member.getUsername())
                        .email(member.getEmail())
                        .build())
                .toList();

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .tag(team.getTag())
                .description(team.getDescription())
                .captain(captainInfo)
                .members(memberInfos)
                .memberCount(memberInfos.size())
                .wins(team.getWins())
                .losses(team.getLosses())
                .winRate(team.getWinRate())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    /**
     * Maps a Team entity to the lightweight TeamSummaryResponse DTO (no member details).
     */
    public TeamSummaryResponse toTeamSummaryResponse(Team team) {
        return TeamSummaryResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .tag(team.getTag())
                .memberCount(team.getMembers() != null ? team.getMembers().size() : 0)
                .wins(team.getWins())
                .losses(team.getLosses())
                .winRate(team.getWinRate())
                .build();
    }
}
