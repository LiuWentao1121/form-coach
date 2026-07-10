package com.formcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingReport {

    private Long sessionId;
    private Long movementId;
    private String movementName;
    private int score;
    private int durationSeconds;
    private int repCount;

    // Error stats: errorType -> count
    private Map<String, Integer> errorStats;

    // Angle curve data for charts: jointName -> list of angle values
    private Map<String, List<Double>> angleCurves;

    // Comparison with previous session
    private Integer previousScore;
    private Integer scoreChange;
    private List<String> improvements;
}
