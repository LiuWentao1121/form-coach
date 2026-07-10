package com.formcoach.service;

import com.alibaba.fastjson2.JSON;
import com.formcoach.dto.FrameRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data-driven angle calculation engine.
 *
 * Reads jointChain from movement standardAngles JSON — no per-exercise code.
 * Adding a new movement = inserting a DB row, zero Java changes.
 *
 * JointChain format in standardAngles JSON:
 *   3 elements → angle via dot-product arccos
 *   2 elements → horizontal projection (e.g. knee-over-toe)
 */
@Slf4j
@Service
public class AngleCalculator {

    @Value("${app.mediapipe.smooth-alpha:0.3}")
    private double smoothAlpha;

    private final Map<String, Map<String, Double>> smoothedAngles = new ConcurrentHashMap<>();

    /** MediaPipe Pose landmark name → index (33 total, we map 14 key joints). */
    static final Map<String, Integer> JOINT = Map.ofEntries(
        Map.entry("leftShoulder", 11),  Map.entry("rightShoulder", 12),
        Map.entry("leftHip", 23),      Map.entry("rightHip", 24),
        Map.entry("leftKnee", 25),     Map.entry("rightKnee", 26),
        Map.entry("leftAnkle", 27),    Map.entry("rightAnkle", 28),
        Map.entry("leftElbow", 13),    Map.entry("rightElbow", 14),
        Map.entry("leftWrist", 15),    Map.entry("rightWrist", 16)
    );

    /**
     * Calculate all angles defined in the movement's standardAngles JSON.
     *
     * @param frame          incoming landmarks
     * @param standardAngles JSON like {"leftKnee":{"min":80,"max":100,"jointChain":["leftHip","leftKnee","leftAnkle"]},...}
     * @param sessionId      for EMA smoothing state
     */
    @SuppressWarnings("unchecked")
    public Map<String, Double> calculate(FrameRequest frame, String standardAngles, String sessionId) {
        List<FrameRequest.Landmark> lm = frame.getLandmarks();
        if (lm == null || lm.size() < 33) {
            log.warn("Invalid landmarks size: {}", lm != null ? lm.size() : 0);
            return Map.of();
        }

        Map<String, Double> angles = new LinkedHashMap<>();

        try {
            Map<String, Object> config = JSON.parseObject(standardAngles, Map.class);
            if (config == null) return angles;

            for (var entry : config.entrySet()) {
                String angleName = entry.getKey();
                Map<String, Object> def = (Map<String, Object>) entry.getValue();
                Object chainObj = def.get("jointChain");
                if (chainObj == null) continue;
                List<String> chain;
                if (chainObj instanceof List) {
                    chain = (List<String>) chainObj;
                } else if (chainObj instanceof com.alibaba.fastjson2.JSONArray) {
                    chain = ((com.alibaba.fastjson2.JSONArray) chainObj).toList(String.class);
                } else {
                    continue;
                }
                if (chain.size() < 2) continue;

                if (chain.size() == 3) {
                    // Standard angle: 3 points → dot-product arccos
                    Integer a = JOINT.get(chain.get(0));
                    Integer b = JOINT.get(chain.get(1));
                    Integer c = JOINT.get(chain.get(2));
                    if (a != null && b != null && c != null) {
                        angles.put(angleName, calcAngle(lm, a, b, c));
                    } else {
                        log.debug("Unknown joint in chain: {}", chain);
                    }
                } else {
                    // 2 points → horizontal projection (knee-over-toe, etc.)
                    Integer a = JOINT.get(chain.get(0));
                    Integer b = JOINT.get(chain.get(1));
                    if (a != null && b != null) {
                        angles.put(angleName, projection(lm, a, b));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse standardAngles: {}", e.getMessage());
        }

        return applyEmaSmoothing(sessionId, angles);
    }

    /** Vector dot-product angle: arccos(v1·v2 / |v1||v2|) */
    public double calcAngle(List<FrameRequest.Landmark> lm, int idx1, int idx2, int idx3) {
        var p1 = lm.get(idx1); var p2 = lm.get(idx2); var p3 = lm.get(idx3);
        double v1x = p1.getX() - p2.getX(), v1y = p1.getY() - p2.getY(), v1z = p1.getZ() - p2.getZ();
        double v2x = p3.getX() - p2.getX(), v2y = p3.getY() - p2.getY(), v2z = p3.getZ() - p2.getZ();
        double dot = v1x * v2x + v1y * v2y + v1z * v2z;
        double m1 = Math.sqrt(v1x * v1x + v1y * v1y + v1z * v1z);
        double m2 = Math.sqrt(v2x * v2x + v2y * v2y + v2z * v2z);
        if (m1 < 0.0001 || m2 < 0.0001) return 0;
        double cos = Math.max(-1.0, Math.min(1.0, dot / (m1 * m2)));
        return Math.toDegrees(Math.acos(cos));
    }

    /** Horizontal offset between two points (scaled ×100 for threshold comparison). */
    private double projection(List<FrameRequest.Landmark> lm, int idx1, int idx2) {
        return Math.abs(lm.get(idx1).getX() - lm.get(idx2).getX()) * 100;
    }

    /** EMA smoothing: EMA(t) = α·value(t) + (1−α)·EMA(t−1) */
    private Map<String, Double> applyEmaSmoothing(String sessionId, Map<String, Double> raw) {
        Map<String, Double> data = smoothedAngles.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
        Map<String, Double> smoothed = new LinkedHashMap<>();
        for (var e : raw.entrySet()) {
            double prev = data.getOrDefault(e.getKey(), e.getValue());
            double val = smoothAlpha * e.getValue() + (1 - smoothAlpha) * prev;
            data.put(e.getKey(), val);
            smoothed.put(e.getKey(), val);
        }
        return smoothed;
    }

    public void clearSession(String sessionId) {
        smoothedAngles.remove(sessionId);
    }
}
