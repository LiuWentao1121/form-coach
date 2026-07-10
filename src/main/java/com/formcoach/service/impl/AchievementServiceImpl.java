package com.formcoach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.formcoach.entity.Achievement;
import com.formcoach.entity.TrainingSession;
import com.formcoach.mapper.AchievementMapper;
import com.formcoach.mapper.TrainingSessionMapper;
import com.formcoach.service.AchievementService;
import com.formcoach.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementMapper achievementMapper;
    private final TrainingSessionMapper sessionMapper;
    private final UserService userService;

    @Override
    public void checkAndUnlock(Long userId, TrainingSession session) {
        Set<String> unlocked = achievementMapper.selectList(
                        new LambdaQueryWrapper<Achievement>().eq(Achievement::getUserId, userId))
                .stream()
                .map(Achievement::getAchievementType)
                .collect(Collectors.toSet());

        // FIRST_TRAINING
        checkAndAward(userId, "FIRST_TRAINING", unlocked);

        // STREAK_7
        if (userService.getStreakDays(userId) >= 7) {
            checkAndAward(userId, "STREAK_7", unlocked);
        }

        // STREAK_30
        if (userService.getStreakDays(userId) >= 30) {
            checkAndAward(userId, "STREAK_30", unlocked);
        }

        // PERFECT_SCORE
        if (session.getScore() != null && session.getScore() == 100) {
            checkAndAward(userId, "PERFECT_SCORE", unlocked);
        }

        // SCORE_1000 — total sessions score > 1000
        long totalScore = sessionMapper.selectList(
                        new LambdaQueryWrapper<TrainingSession>().eq(TrainingSession::getUserId, userId))
                .stream().mapToLong(s -> s.getScore() != null ? s.getScore() : 0).sum();
        if (totalScore >= 1000) {
            checkAndAward(userId, "SCORE_1000", unlocked);
        }

        // SESSIONS_100
        long sessionCount = sessionMapper.selectCount(
                new LambdaQueryWrapper<TrainingSession>().eq(TrainingSession::getUserId, userId));
        if (sessionCount >= 100) {
            checkAndAward(userId, "SESSIONS_100", unlocked);
        }
    }

    private void checkAndAward(Long userId, String type, Set<String> alreadyUnlocked) {
        if (!alreadyUnlocked.contains(type)) {
            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setAchievementType(type);
            achievement.setUnlockedAt(LocalDateTime.now());
            achievementMapper.insert(achievement);
            log.info("Achievement unlocked: userId={}, type={}", userId, type);
        }
    }
}
