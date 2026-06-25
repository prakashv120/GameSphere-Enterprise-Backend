package com.gamesphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;


    private int rank;
    private Long teamId;
    private String teamName;
    private String teamTag;
    private int wins;
    private int losses;
    private double winRate;
    private int memberCount;
}
