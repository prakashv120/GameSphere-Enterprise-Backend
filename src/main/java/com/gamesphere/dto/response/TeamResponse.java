package com.gamesphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String tag;
    private String description;
    private CaptainInfo captain;
    private List<MemberInfo> members;
    private int memberCount;
    private int wins;
    private int losses;
    private double winRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaptainInfo implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String username;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String username;
        private String email;
    }
}
