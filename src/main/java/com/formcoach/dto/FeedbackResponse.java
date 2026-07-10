package com.formcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private int frameIndex;
    private boolean isCorrect;
    private int score;

    @Builder.Default
    private List<ErrorInfo> errors = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private String joint;
        private double angle;
        private String expected;
        private String tip;
    }
}
