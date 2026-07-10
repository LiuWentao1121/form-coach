package com.formcoach.service;

import com.formcoach.dto.FrameRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AngleCalculatorTest {

    private AngleCalculator calc;

    private static final String KNEE_CFG =
        "{\"leftKnee\":{\"min\":80,\"max\":100,\"jointChain\":[\"leftHip\",\"leftKnee\",\"leftAnkle\"]}}";

    @BeforeEach
    void setUp() {
        calc = new AngleCalculator();
        ReflectionTestUtils.setField(calc, "smoothAlpha", 1.0);
    }

    @Test
    void shouldCalc90DegreeAngle() {
        // hip(0.5,0) knee(0,0) ankle(0,0.5) → perfect 90°
        FrameRequest f = frame(23,0.5,0,  25,0,0,  27,0,0.5);
        Map<String, Double> r = calc.calculate(f, KNEE_CFG, "s1");
        assertEquals(90.0, r.get("leftKnee"), 1.0);
    }

    @Test
    void shouldCalc180DegreeAngle() {
        // Straight line → 180°
        FrameRequest f = frame(23,0.4,0,  25,0.4,0.5,  27,0.4,1.0);
        Map<String, Double> r = calc.calculate(f, KNEE_CFG, "s2");
        assertEquals(180.0, r.get("leftKnee"), 0.5);
    }

    @Test
    void shouldHandleEmptyLandmarks() {
        FrameRequest f = new FrameRequest(); f.setLandmarks(List.of());
        assertTrue(calc.calculate(f, KNEE_CFG, "s3").isEmpty());
    }

    @Test
    void shouldHandleNullLandmarks() {
        assertTrue(calc.calculate(new FrameRequest(), KNEE_CFG, "s4").isEmpty());
    }

    @Test
    void shouldSkipUnknownJointNames() {
        String cfg = "{\"x\":{\"min\":0,\"max\":1,\"jointChain\":[\"a\",\"b\",\"c\"]}}";
        assertTrue(calc.calculate(frame(23,0.5,0.5, 25,0.5,0.5, 27,0.5,0.5), cfg, "s5").isEmpty());
    }

    // ── helper ──
    private FrameRequest frame(int i1, double x1, double y1,
                                int i2, double x2, double y2,
                                int i3, double x3, double y3) {
        FrameRequest f = new FrameRequest();
        f.setFrameIndex(0);
        List<FrameRequest.Landmark> lm = new ArrayList<>();
        for (int i = 0; i < 33; i++) {
            FrameRequest.Landmark p = new FrameRequest.Landmark();
            p.setVisibility(1.0);
            if (i == i1) { p.setX(x1); p.setY(y1); }
            else if (i == i2) { p.setX(x2); p.setY(y2); }
            else if (i == i3) { p.setX(x3); p.setY(y3); }
            lm.add(p);
        }
        f.setLandmarks(lm);
        return f;
    }
}
