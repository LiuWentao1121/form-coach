package com.formcoach.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.formcoach.dto.FeedbackResponse.ErrorInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Data-driven error detector — reads standardAngles and tips from movement config.
 * No per-exercise code. Adding a new movement = just a DB row.
 */
@Slf4j
@Service
public class ErrorDetector {

    /** Angle key → human-readable joint label */
    private static final Map<String, String> LABELS = Map.ofEntries(
        Map.entry("leftKnee", "左膝"), Map.entry("rightKnee", "右膝"),
        Map.entry("leftHip", "左髋"), Map.entry("rightHip", "右髋"),
        Map.entry("leftElbow", "左肘"), Map.entry("rightElbow", "右肘"),
        Map.entry("leftShoulderAbduction", "左肩"), Map.entry("rightShoulderAbduction", "右肩"),
        Map.entry("shoulderAngle", "肩部"), Map.entry("backAngle", "腰背"),
        Map.entry("bodyLine", "身体线"), Map.entry("hipHeight", "臀部高度"),
        Map.entry("frontKnee", "前膝"), Map.entry("backKnee", "后膝"),
        Map.entry("frontHip", "前髋"), Map.entry("hipAngle", "髋部"),
        Map.entry("trunkAngle", "躯干"), Map.entry("kneeAngle", "膝部"),
        Map.entry("ankleAngle", "脚踝"), Map.entry("straightKnee", "直腿膝"),
        Map.entry("bentKnee", "屈膝"), Map.entry("leftKneeOverToe", "左膝"),
        Map.entry("rightKneeOverToe", "右膝")
    );

    /**
     * Pure data-driven detection: iterates calculated angles, checks against standardAngles thresholds.
     */
    public List<ErrorInfo> detect(String movementType,
                                   Map<String, Double> angles,
                                   String standardAngles,
                                   String tips) {
        if (angles.isEmpty()) return List.of();

        Map<String, AngleRange> standards = parseStandardAngles(standardAngles);
        Map<String, String> tipsMap = parseTips(tips);
        if (standards.isEmpty()) return List.of();

        List<ErrorInfo> errors = new ArrayList<>();

        for (var entry : angles.entrySet()) {
            String key = entry.getKey();
            double value = entry.getValue();
            AngleRange range = standards.get(key);
            if (range == null) continue;

            String label = LABELS.getOrDefault(key, key);
            String expected = range.min + "-" + range.max;

            if (value < range.min) {
                String tip = findTip(tipsMap, key, "low");
                errors.add(buildError(label, value, expected, tip));
            } else if (value > range.max) {
                String tip = findTip(tipsMap, key, "high");
                errors.add(buildError(label, value, expected, tip));
            }
        }

        return errors;
    }

    /** Pick a relevant tip: matches angle key to tip key, with fallback. */
    private String findTip(Map<String, String> tips, String angleKey, String direction) {
        if (tips.isEmpty()) return "注意调整姿势";
        // Map angle keys to expected tip keys
        String simple = angleKey.replace("left","").replace("right","").replace("OverToe","").toLowerCase();
        // Direct match first
        for (var e : tips.entrySet()) {
            if (simple.contains(e.getKey().toLowerCase()) || e.getKey().toLowerCase().contains(simple)) {
                return e.getValue();
            }
        }
        // Thematically: "low" = too small = too deep/bent, "high" = not enough
        String fallback = "high".equals(direction) ? "动作幅度不够" : "动作幅度过大，注意控制";
        for (var e : tips.entrySet()) {
            if ("high".equals(direction) && (e.getKey().contains("Shallow")||e.getKey().contains("High")||e.getKey().contains("Lock")))
                return e.getValue();
            if ("low".equals(direction) && (e.getKey().contains("Deep")||e.getKey().contains("Low")||e.getKey().contains("Round")||e.getKey().contains("Sag")))
                return e.getValue();
        }
        return tips.values().iterator().next();
    }

    private ErrorInfo buildError(String joint, double angle, String expected, String tip) {
        return ErrorInfo.builder()
                .joint(joint)
                .angle(Math.round(angle * 10.0) / 10.0)
                .expected(expected)
                .tip(tip != null ? tip : "注意姿势调整")
                .build();
    }

    private Map<String, AngleRange> parseStandardAngles(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return JSON.parseObject(json, new TypeReference<Map<String, AngleRange>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse standard angles: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, String> parseTips(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return JSON.parseObject(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse tips: {}", e.getMessage());
            return Map.of();
        }
    }

    public static class AngleRange {
        public double min;
        public double max;
        public List<String> jointChain;
        public AngleRange() {}
    }
}
