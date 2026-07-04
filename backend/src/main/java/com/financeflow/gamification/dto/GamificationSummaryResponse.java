package com.financeflow.gamification.dto;

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
public class GamificationSummaryResponse {

    private int healthScore;
    private int currentStreak;
    private List<AchievementDto> achievements;
    private List<AchievementDto> recentAchievements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementDto {
        private String code;
        private String title;
        private String description;
        private LocalDateTime unlockedAt;
    }
}
