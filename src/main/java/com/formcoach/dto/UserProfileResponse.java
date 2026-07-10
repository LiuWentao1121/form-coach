package com.formcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private BigDecimal height;
    private BigDecimal weight;
    private Integer gender;

    // Stats
    private int totalSessions;
    private int totalDurationMinutes;
    private int streakDays;
    private int totalScore;

    // Calendar heatmap: date string -> minutes
    private Map<String, Integer> trainingHeatmap;

    // Achievements
    private List<AchievementInfo> achievements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementInfo {
        private String type;
        private String name;
        private String description;
        private String unlockedAt;
        private boolean unlocked;
    }
}
