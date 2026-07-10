package com.formcoach.service;

import com.formcoach.dto.FeedbackResponse;
import com.formcoach.dto.FrameRequest;
import com.formcoach.dto.TrainingReport;

public interface TrainingService {

    /**
     * Start a new training session.
     * @return sessionId
     */
    Long startSession(Long userId, Long movementId);

    /**
     * Process a single frame: calculate angles → detect errors → score.
     */
    FeedbackResponse processFrame(FrameRequest frame);

    /**
     * End training session and generate results.
     */
    TrainingReport endSession(Long sessionId);

    /**
     * Get session by ID
     */
    com.formcoach.entity.TrainingSession getSession(Long sessionId);
}
