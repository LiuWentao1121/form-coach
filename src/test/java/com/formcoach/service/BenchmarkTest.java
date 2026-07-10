package com.formcoach.service;

import com.formcoach.dto.FeedbackResponse.ErrorInfo;
import com.formcoach.dto.FrameRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmark: validates < 5ms per-frame engine latency.
 * Run with: mvn test -Dtest=BenchmarkTest
 */
class BenchmarkTest {

    private AngleCalculator angleCalc;
    private ErrorDetector errorDetector;
    private ScoreCalculator scoreCalc;

    private static final String SQUAT_CFG =
        "{\"leftKnee\":{\"min\":80,\"max\":100,\"jointChain\":[\"leftHip\",\"leftKnee\",\"leftAnkle\"]}," +
        "\"rightKnee\":{\"min\":80,\"max\":100,\"jointChain\":[\"rightHip\",\"rightKnee\",\"rightAnkle\"]}," +
        "\"leftHip\":{\"min\":70,\"max\":100,\"jointChain\":[\"leftShoulder\",\"leftHip\",\"leftKnee\"]}," +
        "\"rightHip\":{\"min\":70,\"max\":100,\"jointChain\":[\"rightShoulder\",\"rightHip\",\"rightKnee\"]}," +
        "\"backAngle\":{\"min\":60,\"max\":90,\"jointChain\":[\"leftShoulder\",\"leftHip\",\"leftAnkle\"]}," +
        "\"leftKneeOverToe\":{\"min\":0,\"max\":3,\"jointChain\":[\"leftKnee\",\"leftAnkle\"]}," +
        "\"rightKneeOverToe\":{\"min\":0,\"max\":3,\"jointChain\":[\"rightKnee\",\"rightAnkle\"]}}";

    private static final String SQUAT_TIPS =
        "{\"tooDeep\":\"x\",\"tooShallow\":\"x\",\"backRound\":\"x\",\"kneeOverToe\":\"x\",\"leanForward\":\"x\",\"kneeValgus\":\"x\"}";

    private static final int WARMUP = 100;
    private static final int ITERATIONS = 1000;

    @BeforeEach
    void setUp() {
        angleCalc = new AngleCalculator();
        ReflectionTestUtils.setField(angleCalc, "smoothAlpha", 1.0);
        errorDetector = new ErrorDetector();
        scoreCalc = new ScoreCalculator();
    }

    @Test
    void benchmarkFullPipeline() {
        List<Double> latencies = new ArrayList<>(ITERATIONS);

        for (int i = 0; i < WARMUP + ITERATIONS; i++) {
            FrameRequest frame = randomSquatFrame(i);
            String sid = "bench-" + (i / 10);

            long start = System.nanoTime();

            // Full pipeline: angle → detect → score (what happens per frame)
            Map<String, Double> angles = angleCalc.calculate(frame, SQUAT_CFG, sid);
            List<ErrorInfo> errors = errorDetector.detect("SQUAT", angles, SQUAT_CFG, SQUAT_TIPS);
            int score = scoreCalc.calculateFrameScore(errors);

            long elapsed = System.nanoTime() - start;

            if (i >= WARMUP) {
                latencies.add(elapsed / 1_000_000.0); // ms
            }
        }

        latencies.sort(Double::compare);
        double avg = latencies.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double p50 = latencies.get(latencies.size() / 2);
        double p95 = latencies.get((int)(latencies.size() * 0.95));
        double p99 = latencies.get((int)(latencies.size() * 0.99));
        double max = latencies.get(latencies.size() - 1);

        System.out.println("\n========== FormCoach Engine Benchmark ==========");
        System.out.printf("  Iterations : %d frames%n", ITERATIONS);
        System.out.printf("  Avg latency: %.3f ms%n", avg);
        System.out.printf("  P50 latency: %.3f ms%n", p50);
        System.out.printf("  P95 latency: %.3f ms%n", p95);
        System.out.printf("  P99 latency: %.3f ms%n", p99);
        System.out.printf("  Max latency: %.3f ms%n", max);
        System.out.printf("  Throughput : %.0f frames/sec%n", 1000.0 / avg);
        System.out.println("=================================================");

        // Assert: average must be < 5ms
        assertTrue(avg < 5.0,
            String.format("Average latency %.2fms exceeds 5ms target!", avg));

        // Assert: P99 must be < 10ms
        assertTrue(p99 < 10.0,
            String.format("P99 latency %.2fms exceeds 10ms target!", p99));
    }

    @Test
    void benchmarkAngleCalculationOnly() {
        List<Double> lats = new ArrayList<>(ITERATIONS);
        FrameRequest frame = randomSquatFrame(0);

        for (int i = 0; i < WARMUP + ITERATIONS; i++) {
            long s = System.nanoTime();
            angleCalc.calculate(frame, SQUAT_CFG, "bench");
            if (i >= WARMUP) lats.add((System.nanoTime() - s) / 1_000_000.0);
        }
        double avg = lats.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        System.out.printf("%n  AngleCalculator only: %.3f ms avg (target < 3ms)%n", avg);
        assertTrue(avg < 3.0, "Angle calc too slow: " + avg + "ms");
    }

    @Test
    void benchmarkErrorDetectionOnly() {
        Map<String, Double> angles = Map.of(
            "leftKnee", 85.0, "rightKnee", 92.0,
            "leftHip", 80.0, "rightHip", 88.0,
            "backAngle", 75.0,
            "leftKneeOverToe", 2.0, "rightKneeOverToe", 1.5
        );

        List<Double> lats = new ArrayList<>(ITERATIONS);
        for (int i = 0; i < WARMUP + ITERATIONS; i++) {
            long s = System.nanoTime();
            errorDetector.detect("SQUAT", angles, SQUAT_CFG, SQUAT_TIPS);
            if (i >= WARMUP) lats.add((System.nanoTime() - s) / 1_000_000.0);
        }
        double avg = lats.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        System.out.printf("  ErrorDetector only:     %.3f ms avg (target < 1ms)%n", avg);
        assertTrue(avg < 1.0, "Error detection too slow: " + avg + "ms");
    }

    // ── helpers ──

    private FrameRequest randomSquatFrame(int seed) {
        Random r = new Random(seed);
        FrameRequest frame = new FrameRequest();
        frame.setFrameIndex(seed);
        frame.setMovementType("SQUAT");

        // Generate 33 landmarks with slight noise around realistic squat positions
        List<FrameRequest.Landmark> lm = new ArrayList<>();
        for (int i = 0; i < 33; i++) {
            FrameRequest.Landmark p = new FrameRequest.Landmark();
            p.setVisibility(0.9 + r.nextDouble() * 0.1);

            if (i == 23) { p.setX(0.45+r.nextGaussian()*0.01); p.setY(0.4+r.nextGaussian()*0.01); }   // leftHip
            else if (i == 25) { p.setX(0.45+r.nextGaussian()*0.01); p.setY(0.7+r.nextGaussian()*0.01); } // leftKnee
            else if (i == 27) { p.setX(0.4+r.nextGaussian()*0.01); p.setY(0.95+r.nextGaussian()*0.01); } // leftAnkle
            else if (i == 24) { p.setX(0.55+r.nextGaussian()*0.01); p.setY(0.4+r.nextGaussian()*0.01); }
            else if (i == 26) { p.setX(0.55+r.nextGaussian()*0.01); p.setY(0.7+r.nextGaussian()*0.01); }
            else if (i == 28) { p.setX(0.6+r.nextGaussian()*0.01); p.setY(0.95+r.nextGaussian()*0.01); }
            else if (i == 11) { p.setX(0.45+r.nextGaussian()*0.01); p.setY(0.15); }
            else if (i == 12) { p.setX(0.55+r.nextGaussian()*0.01); p.setY(0.15); }
            else { p.setX(0.5+r.nextGaussian()*0.02); p.setY(0.5+r.nextGaussian()*0.02); }

            lm.add(p);
        }
        frame.setLandmarks(lm);
        return frame;
    }
}
