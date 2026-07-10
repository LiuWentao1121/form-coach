package com.formcoach.service;

import com.formcoach.dto.UserProfileResponse;
import com.formcoach.entity.User;

public interface UserService {

    User getById(Long id);

    void updateProfile(Long userId, User updates);

    /**
     * Get full user profile with stats, heatmap, achievements
     */
    UserProfileResponse getProfile(Long userId);

    /**
     * Get consecutive training days up to today
     */
    int getStreakDays(Long userId);
}
