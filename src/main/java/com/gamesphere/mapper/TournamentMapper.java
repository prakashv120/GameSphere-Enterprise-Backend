package com.gamesphere.mapper;

import com.gamesphere.dto.response.TournamentResponse;
import com.gamesphere.dto.response.TournamentSummaryResponse;
import com.gamesphere.entity.Tournament;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TournamentMapper {

    private final TeamMapper teamMapper;

    public TournamentMapper(TeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    /**
     * Maps a Tournament entity to the full TournamentResponse DTO.
     */
    public TournamentResponse toTournamentResponse(Tournament tournament) {
        List<com.gamesphere.dto.response.TeamSummaryResponse> teamSummaries =
                tournament.getRegisteredTeams().stream()
                        .map(teamMapper::toTeamSummaryResponse)
                        .toList();

        return TournamentResponse.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .description(tournament.getDescription())
                .status(tournament.getStatus())
                .maxTeams(tournament.getMaxTeams())
                .registeredTeamsCount(teamSummaries.size())
                .prizePool(tournament.getPrizePool())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .registeredTeams(teamSummaries)
                .createdAt(tournament.getCreatedAt())
                .updatedAt(tournament.getUpdatedAt())
                .build();
    }

    /**
     * Maps a Tournament entity to the lightweight TournamentSummaryResponse DTO.
     */
    public TournamentSummaryResponse toTournamentSummaryResponse(Tournament tournament) {
        return TournamentSummaryResponse.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .status(tournament.getStatus())
                .maxTeams(tournament.getMaxTeams())
                .registeredTeamsCount(
                        tournament.getRegisteredTeams() != null
                                ? tournament.getRegisteredTeams().size() : 0)
                .prizePool(tournament.getPrizePool())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .build();
    }
}
