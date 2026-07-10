package com.formcoach.controller;

import com.formcoach.common.Result;
import com.formcoach.dto.FeedbackResponse;
import com.formcoach.dto.FrameRequest;
import com.formcoach.dto.TrainingReport;
import com.formcoach.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "训练", description = "训练会话管理（WebSocket 为主，HTTP 为备用通道）")
@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @Operation(summary = "开始训练会话，返回 sessionId")
    @PostMapping("/start")
    public Result<?> start(@RequestParam Long movementId) {
        Long userId = getCurrentUserId();
        Long sessionId = trainingService.startSession(userId, movementId);
        return Result.ok(java.util.Map.of("sessionId", sessionId));
    }

    @Operation(summary = "提交一帧关节点数据，返回纠错结果")
    @PostMapping("/frame")
    public Result<FeedbackResponse> processFrame(@RequestBody FrameRequest frame) {
        return Result.ok(trainingService.processFrame(frame));
    }

    @Operation(summary = "结束训练并生成报告")
    @PostMapping("/end")
    public Result<TrainingReport> end(@RequestParam Long sessionId) {
        return Result.ok(trainingService.endSession(sessionId));
    }

    @Operation(summary = "查询训练会话")
    @GetMapping("/session/{sessionId}")
    public Result<?> getSession(@PathVariable Long sessionId) {
        return Result.ok(trainingService.getSession(sessionId));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
