package com.formcoach.controller;

import com.formcoach.common.PageResult;
import com.formcoach.common.Result;
import com.formcoach.dto.TrainingReport;
import com.formcoach.entity.TrainingSession;
import com.formcoach.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "训练报告", description = "训练数据分析与对比")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "获取单次训练报告（评分、错误统计、角度曲线）")
    @GetMapping("/{sessionId}")
    public Result<TrainingReport> getReport(@PathVariable Long sessionId) {
        return Result.ok(reportService.getReport(sessionId));
    }

    @Operation(summary = "训练历史列表")
    @GetMapping
    public Result<PageResult<TrainingSession>> listSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        return Result.ok(reportService.listSessions(userId, page, size));
    }

    @Operation(summary = "训练统计数据（总次数、平均分、各动作统计）")
    @GetMapping("/stats")
    public Result<?> getStats() {
        Long userId = getCurrentUserId();
        return Result.ok(reportService.getUserStats(userId));
    }

    @Operation(summary = "对比两次训练")
    @GetMapping("/compare")
    public Result<?> compare(@RequestParam Long sessionId1, @RequestParam Long sessionId2) {
        return Result.ok(reportService.compareSessions(sessionId1, sessionId2));
    }

    @Operation(summary = "本周训练报告（每日明细 + 趋势分析）")
    @GetMapping("/weekly")
    public Result<?> weeklyReport() {
        Long userId = getCurrentUserId();
        return Result.ok(reportService.getWeeklyReport(userId));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
