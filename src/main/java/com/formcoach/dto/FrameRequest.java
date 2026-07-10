package com.formcoach.dto;

import lombok.Data;

import java.util.List;

@Data
public class FrameRequest {

    private Long sessionId;
    private Integer frameIndex;
    private String movementType;  // SQUAT / PUSH_UP / PLANK
    private List<Landmark> landmarks;

    @Data
    public static class Landmark {
        private double x;
        private double y;
        private double z;
        private double visibility;
    }
}
