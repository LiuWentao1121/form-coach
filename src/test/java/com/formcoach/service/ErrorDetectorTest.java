package com.formcoach.service;

import com.alibaba.fastjson2.JSON;
import com.formcoach.dto.FeedbackResponse.ErrorInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorDetectorTest {

    private final ErrorDetector detector = new ErrorDetector();

    private final String squatStandardAngles = JSON.toJSONString(Map.of(
            "leftKnee", Map.of("min", 80, "max", 100),
            "rightKnee", Map.of("min", 80, "max", 100),
            "leftHip", Map.of("min", 70, "max", 100),
            "rightHip", Map.of("min", 70, "max", 100),
            "backAngle", Map.of("min", 60, "max", 90),
            "leftKneeOverToe", Map.of("min", (Object)0.0, "max", (Object)3.0),
            "rightKneeOverToe", Map.of("min", (Object)0.0, "max", (Object)3.0)
    ));

    private final String squatTips = JSON.toJSONString(Map.of(
            "tooDeep", "下蹲过深，膝盖受力过大",
            "tooShallow", "再蹲低一点，大腿与地面平行",
            "backRound", "挺直腰背，不要弓背",
            "kneeOverToe", "重心后移，膝盖不要超过脚尖",
            "leanForward", "上身不要太前倾",
            "kneeValgus", "膝盖不要内扣"
    ));

    @Test
    void shouldDetectPerfectSquat() {
        Map<String, Double> angles = Map.of(
                "leftKnee", 90.0,
                "rightKnee", 90.0,
                "leftHip", 85.0,
                "rightHip", 85.0,
                "backAngle", 75.0,
                "leftKneeOverToe", 1.0,
                "rightKneeOverToe", 1.0
        );

        List<ErrorInfo> errors = detector.detect("SQUAT", angles, squatStandardAngles, squatTips);
        assertTrue(errors.isEmpty(), "Perfect squat should have zero errors, got: " + errors.size());
    }

    @Test
    void shouldDetectKneeTooDeep() {
        Map<String, Double> angles = Map.of(
                "leftKnee", 60.0,   // too deep (< 80)
                "rightKnee", 90.0,
                "leftHip", 85.0,
                "rightHip", 85.0,
                "backAngle", 75.0,
                "leftKneeOverToe", 1.0,
                "rightKneeOverToe", 1.0
        );

        List<ErrorInfo> errors = detector.detect("SQUAT", angles, squatStandardAngles, squatTips);
        assertTrue(errors.size() > 0, "Should detect knee too deep");
        assertTrue(errors.stream().anyMatch(e -> e.getJoint().contains("左膝")),
                "Should flag left knee");
    }

    @Test
    void shouldDetectKneeTooShallow() {
        Map<String, Double> angles = Map.of(
                "leftKnee", 120.0,  // too shallow (> 100)
                "rightKnee", 90.0,
                "leftHip", 85.0,
                "rightHip", 85.0,
                "backAngle", 75.0,
                "leftKneeOverToe", 1.0,
                "rightKneeOverToe", 1.0
        );

        List<ErrorInfo> errors = detector.detect("SQUAT", angles, squatStandardAngles, squatTips);
        assertTrue(errors.size() > 0, "Should detect knee too shallow");
    }

    @Test
    void shouldDetectBackRound() {
        Map<String, Double> angles = Map.of(
                "leftKnee", 90.0,
                "rightKnee", 90.0,
                "leftHip", 85.0,
                "rightHip", 85.0,
                "backAngle", 30.0,  // rounded back (< 60)
                "leftKneeOverToe", 1.0,
                "rightKneeOverToe", 1.0
        );

        List<ErrorInfo> errors = detector.detect("SQUAT", angles, squatStandardAngles, squatTips);
        assertTrue(errors.stream().anyMatch(e -> e.getJoint().contains("腰背")),
                "Should detect back rounding");
    }

    @Test
    void shouldDetectKneeOverToe() {
        Map<String, Double> angles = Map.of(
                "leftKnee", 90.0,
                "rightKnee", 90.0,
                "leftHip", 85.0,
                "rightHip", 85.0,
                "backAngle", 75.0,
                "leftKneeOverToe", 5.0,  // knee over toe (> 3)
                "rightKneeOverToe", 1.0
        );

        List<ErrorInfo> errors = detector.detect("SQUAT", angles, squatStandardAngles, squatTips);
        assertFalse(errors.isEmpty(), "Should detect at least one error for knee over toe");
        assertTrue(errors.stream().anyMatch(e -> e.getJoint().contains("左膝")),
                "Should flag left knee for over-toe projection");
    }

    @Test
    void shouldReturnEmptyForEmptyAngles() {
        List<ErrorInfo> errors = detector.detect("SQUAT", Map.of(), squatStandardAngles, squatTips);
        assertTrue(errors.isEmpty());
    }
}
