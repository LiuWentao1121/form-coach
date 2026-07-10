package com.formcoach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.formcoach.dto.UserProfileResponse;
import com.formcoach.dto.UserProfileResponse.AchievementInfo;
import com.formcoach.common.BusinessException;
import com.formcoach.common.ErrorCode;
import com.formcoach.entity.Achievement;
import com.formcoach.entity.User;
import com.formcoach.mapper.AchievementMapper;
import com.formcoach.mapper.TrainingSessionMapper;
import com.formcoach.mapper.UserMapper;
import com.formcoach.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final TrainingSessionMapper trainingSessionMapper;
    private final AchievementMapper achievementMapper;

    // Achievement definitions
    private static final Map<String, String[]> ACHIEVEMENT_DEFS = Map.of(
            "FIRST_TRAINING", new String[]{"初次训练", "完成第一次训练"},
            "STREAK_7", new String[]{"连续7天", "连续7天完成训练"},
            "STREAK_30", new String[]{"连续30天", "连续30天完成训练"},
            "SCORE_1000", new String[]{"千分达人", "累计训练得分超过1000分"},
            "SESSIONS_100", new String[]{"百练成金", "累计完成100次训练"},
            "PERFECT_SCORE", new String[]{"满分训练", "单次训练获得100分"}
    );

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public void updateProfile(Long userId, User updates) {
        updates.setId(userId);
        // Only allow updating non-sensitive fields
        User target = new User();
        target.setId(userId);
        target.setNickname(updates.getNickname());
        target.setAvatar(updates.getAvatar());
        target.setHeight(updates.getHeight());
        target.setWeight(updates.getWeight());
        target.setGender(updates.getGender());
        userMapper.updateById(target);
    }

    @Override
    public UserProfileResponse getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        long totalSessionsLong = trainingSessionMapper.selectCount(
                new LambdaQueryWrapper<com.formcoach.entity.TrainingSession>()
                        .eq(com.formcoach.entity.TrainingSession::getUserId, userId));
        int totalSessions = (int) totalSessionsLong;
        int totalSeconds = trainingSessionMapper.sumTrainingSeconds(userId);
        int streakDays = getStreakDays(userId);

        // Heatmap data (last 180 days)
        String startDate = LocalDate.now().minusDays(180).format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<Map<String, Object>> heatmapRows = trainingSessionMapper.getHeatmapData(userId, startDate);
        Map<String, Integer> heatmap = new HashMap<>();
        for (Map<String, Object> row : heatmapRows) {
            String date = row.get("date").toString();
            Object secondsObj = row.get("seconds");
            int seconds = secondsObj instanceof Number ? ((Number) secondsObj).intValue() : 0;
            heatmap.put(date, seconds / 60); // convert to minutes
        }

        // Achievements
        List<Achievement> unlocked = achievementMapper.selectList(
                new LambdaQueryWrapper<Achievement>().eq(Achievement::getUserId, userId));
        Set<String> unlockedTypes = unlocked.stream()
                .map(Achievement::getAchievementType)
                .collect(Collectors.toSet());

        List<AchievementInfo> achievements = new ArrayList<>();
        for (var entry : ACHIEVEMENT_DEFS.entrySet()) {
            achievements.add(AchievementInfo.builder()
                    .type(entry.getKey())
                    .name(entry.getValue()[0])
                    .description(entry.getValue()[1])
                    .unlocked(unlockedTypes.contains(entry.getKey()))
                    .unlockedAt(unlocked.stream()
                            .filter(a -> a.getAchievementType().equals(entry.getKey()))
                            .findFirst()
                            .map(a -> a.getUnlockedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .orElse(null))
                    .build());
        }

        // Total score
        List<com.formcoach.entity.TrainingSession> sessions = trainingSessionMapper.selectList(
                new LambdaQueryWrapper<com.formcoach.entity.TrainingSession>()
                        .eq(com.formcoach.entity.TrainingSession::getUserId, userId));
        int totalScore = sessions.stream().mapToInt(s -> s.getScore() != null ? s.getScore() : 0).sum();

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .height(user.getHeight())
                .weight(user.getWeight())
                .gender(user.getGender())
                .totalSessions(totalSessions)
                .totalDurationMinutes(totalSeconds / 60)
                .streakDays(streakDays)
                .totalScore(totalScore)
                .trainingHeatmap(heatmap)
                .achievements(achievements)
                .build();
    }

    @Override
    public int getStreakDays(Long userId) {
        // Query all training dates ordered DESC
        List<com.formcoach.entity.TrainingSession> sessions = trainingSessionMapper.selectList(
                new LambdaQueryWrapper<com.formcoach.entity.TrainingSession>()
                        .eq(com.formcoach.entity.TrainingSession::getUserId, userId)
                        .orderByDesc(com.formcoach.entity.TrainingSession::getCreatedAt));

        if (sessions.isEmpty()) return 0;

        Set<LocalDate> trainingDates = sessions.stream()
                .map(s -> s.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate checkDate = LocalDate.now();

        // If no training today, check if yesterday counts
        while (trainingDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }
}
