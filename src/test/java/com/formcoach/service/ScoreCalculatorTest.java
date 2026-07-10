package com.formcoach.service;

import com.formcoach.dto.FeedbackResponse.ErrorInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    @Test
    void perfectFrameShouldScore100() {
        int score = calculator.calculateFrameScore(List.of());
        assertEquals(100, score);
    }

    @Test
    void oneErrorShouldDeduct() {
        List<ErrorInfo> errors = List.of(
                ErrorInfo.builder().joint("左膝").angle(75.0).expected("80-100").tip("再蹲低").build()
        );
        int score = calculator.calculateFrameScore(errors);
        assertTrue(score < 100, "Score should be less than 100 with errors");
        assertTrue(score >= 80, "One error should not drop below 80, got: " + score);
    }

    @Test
    void coreErrorShouldDeductMore() {
        List<ErrorInfo> errors = List.of(
                ErrorInfo.builder().joint("核心").angle(150.0).expected("170-180").tip("收紧核心").build()
        );
        int score1 = calculator.calculateFrameScore(errors);

        // Compare with a non-core error
        List<ErrorInfo> errors2 = List.of(
                ErrorInfo.builder().joint("左肘").angle(60.0).expected("70-100").tip("手肘靠近").build()
        );
        int score2 = calculator.calculateFrameScore(errors2);

        assertTrue(score1 < score2,
                "Core error should deduct more. Core: " + score1 + ", Elbow: " + score2);
    }

    @Test
    void scoreShouldNotGoBelowZero() {
        List<ErrorInfo> errors = List.of(
                ErrorInfo.builder().joint("核心").angle(0.0).expected("170-180").tip("e1").build(),
                ErrorInfo.builder().joint("腰背").angle(0.0).expected("60-90").tip("e2").build(),
                ErrorInfo.builder().joint("左膝").angle(0.0).expected("80-100").tip("e3").build(),
                ErrorInfo.builder().joint("右膝").angle(0.0).expected("80-100").tip("e4").build(),
                ErrorInfo.builder().joint("左髋").angle(0.0).expected("70-100").tip("e5").build(),
                ErrorInfo.builder().joint("右髋").angle(0.0).expected("70-100").tip("e6").build(),
                ErrorInfo.builder().joint("左踝").angle(0.0).expected("0-30").tip("e7").build(),
                ErrorInfo.builder().joint("右踝").angle(0.0).expected("0-30").tip("e8").build(),
                ErrorInfo.builder().joint("左肘").angle(0.0).expected("70-100").tip("e9").build(),
                ErrorInfo.builder().joint("右肘").angle(0.0).expected("70-100").tip("e10").build(),
                ErrorInfo.builder().joint("左肩").angle(0.0).expected("30-60").tip("e11").build()
        );
        int score = calculator.calculateFrameScore(errors);
        assertEquals(0, score, "Score should bottom out at 0, got: " + score);
    }

    @Test
    void shouldCalculateSessionScore() {
        List<Integer> frameScores = List.of(100, 90, 80, 95, 85);
        // avg = 90, errorRate = 0/5 = 0
        int score = calculator.calculateSessionScore(frameScores, 5, 0);
        assertTrue(score >= 85 && score <= 100, "Session score should be reasonable, got: " + score);
    }

    @Test
    void highErrorRateShouldPenalize() {
        List<Integer> frameScores = List.of(90, 85, 80);
        // 15 error frames out of 18 total = high error rate
        int score = calculator.calculateSessionScore(frameScores, 18, 15);
        assertTrue(score < 80, "High error rate should penalize, got: " + score);
    }

    @Test
    void emptySessionShouldScore0() {
        int score = calculator.calculateSessionScore(List.of(), 0, 0);
        assertEquals(0, score);
    }

    @Test
    void scoreToGradeShouldWork() {
        assertEquals("S", calculator.scoreToGrade(96));
        assertEquals("A", calculator.scoreToGrade(88));
        assertEquals("B", calculator.scoreToGrade(78));
        assertEquals("C", calculator.scoreToGrade(65));
        assertEquals("D", calculator.scoreToGrade(30));
    }
}
