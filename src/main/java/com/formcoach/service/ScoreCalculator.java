package com.formcoach.service;

import com.formcoach.dto.FeedbackResponse.ErrorInfo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Balanced scoring engine: strict on errors, fair on overall.
 * - Per error: 12 points base + extra for critical joints
 * - Session: error-rate penalty up to 30 points
 */
@Service
public class ScoreCalculator {

    public int calculateFrameScore(List<ErrorInfo> errors) {
        if (errors == null || errors.isEmpty()) return 100;

        double deduction = 0;
        for (ErrorInfo e : errors) {
            deduction += 12;
            if (e.getJoint().contains("核心") || e.getJoint().contains("腰背")) deduction += 8;
            if (e.getJoint().contains("膝")) deduction += 4;
            if (e.getJoint().contains("髋")) deduction += 3;
        }
        return Math.max(0, (int) (100 - deduction));
    }

    public int calculateSessionScore(List<Integer> frameScores, int totalFrames, int errorFrames) {
        if (totalFrames == 0) return 0;
        if (frameScores.isEmpty()) return 100;

        double avgScore = frameScores.stream().mapToInt(Integer::intValue).average().orElse(0);
        double errorRate = (double) errorFrames / totalFrames;
        double errorPenalty = errorRate * 30;

        return (int) Math.max(0, Math.min(100, avgScore - errorPenalty));
    }

    public String scoreToGrade(int score) {
        if (score >= 95) return "S";
        if (score >= 85) return "A";
        if (score >= 70) return "B";
        if (score >= 50) return "C";
        return "D";
    }
}
