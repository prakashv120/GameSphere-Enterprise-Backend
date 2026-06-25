package com.gamesphere.mapper;

import com.gamesphere.dto.response.MatchResponse;
import com.gamesphere.entity.Match;
import com.gamesphere.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    /**
     * Maps a Match entity to MatchResponse DTO.
     */
    public MatchResponse toMatchResponse(Match match) {
        return MatchResponse.builder()
                .id(match.getId())
                .tournamentId(match.getTournament() != null ? match.getTournament().getId() : null)
                .tournamentName(match.getTournament() != null ? match.getTournament().getName() : null)
                .teamA(toTeamInfo(match.getTeamA()))
                .teamB(toTeamInfo(match.getTeamB()))
                .winner(toTeamInfo(match.getWinner()))
                .teamAScore(match.getTeamAScore())
                .teamBScore(match.getTeamBScore())
                .status(match.getStatus())
                .scheduledAt(match.getScheduledAt())
                .playedAt(match.getPlayedAt())
                .notes(match.getNotes())
                .createdAt(match.getCreatedAt())
                .updatedAt(match.getUpdatedAt())
                .build();
    }

    private MatchResponse.TeamInfo toTeamInfo(Team team) {
        if (team == null) return null;
        return MatchResponse.TeamInfo.builder()
                .id(team.getId())
                .name(team.getName())
                .tag(team.getTag())
                .build();
    }
}
