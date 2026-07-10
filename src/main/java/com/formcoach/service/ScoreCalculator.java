package com.formcoach.service;

import com.formcoach.dto.FeedbackResponse.ErrorInfo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Score engine: calculates a 0-100 score based on form quality.
 *
 * Scoring factors:
 * - Error frame ratio (fewer errors = higher score)
 * - Angle deviation from ideal range
 * - Per-joint weighting (core joints weighted higher)
 */
@Service
public class ScoreCalculator {

    /**
     * Calculate overall score for a single frame.
     *
     * @param errors detected errors for this frame
     * @return score 0-100
     */
    public int calculateFrameScore(List<ErrorInfo> errors) {
        if (errors == null || errors.isEmpty()) {
            return 100;
        }

        // Base 100, deduct per error
        int deduction = errors.size() * 10;
        // Extra deduction for core-related errors
        for (ErrorInfo e : errors) {
            if (e.getJoint().contains("核心") || e.getJoint().contains("腰背")) {
                deduction += 5;
            }
            if (e.getJoint().contains("膝")) {
                deduction += 3;
            }
        }

        return Math.max(0, 100 - deduction);
    }

    /**
     * Calculate session score based on all frames in a session.
     *
     * @param frameScores   list of per-frame scores
     * @param totalFrames   total number of frames
     * @param errorFrames   number of frames with errors
     * @return weighted session score 0-100
     */
    public int calculateSessionScore(List<Integer> frameScores, int totalFrames, int errorFrames) {
        if (totalFrames == 0) return 0;
        if (frameScores.isEmpty()) return 100;

        // Average frame score
        double avgScore = frameScores.stream().mapToInt(Integer::intValue).average().orElse(0);

        // Penalty for high error rate
        double errorRate = (double) errorFrames / totalFrames;
        double errorPenalty = errorRate * 20; // up to 20 points penalty

        // Bonus for consistency (lower variance = higher bonus)
        double variance = frameScores.stream()
                .mapToDouble(s -> Math.pow(s - avgScore, 2))
                .average().orElse(0);
        double consistencyBonus = Math.max(0, 5 - Math.sqrt(variance) * 0.5); // up to 5

        return (int) Math.max(0, Math.min(100, avgScore - errorPenalty + consistencyBonus));
    }

    /**
     * Generate a letter grade from a numeric score.
     */
    public String scoreToGrade(int score) {
        if (score >= 95) return "S";
        if (score >= 85) return "A";
        if (score >= 75) return "B";
        if (score >= 60) return "C";
        return "D";
    }
}
