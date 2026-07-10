package com.formcoach.service;

import com.formcoach.entity.TrainingSession;

public interface AchievementService {

    /**
     * Check and unlock any achievements after a training session.
     */
    void checkAndUnlock(Long userId, TrainingSession session);
}
