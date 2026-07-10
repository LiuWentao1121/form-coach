package com.formcoach.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.formcoach.common.PageResult;
import com.formcoach.dto.TrainingReport;
import com.formcoach.entity.AngleResult;
import com.formcoach.entity.Movement;
import com.formcoach.entity.TrainingSession;
import com.formcoach.mapper.AngleResultMapper;
import com.formcoach.mapper.MovementMapper;
import com.formcoach.mapper.TrainingSessionMapper;
import com.formcoach.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TrainingSessionMapper sessionMapper;
    private final AngleResultMapper angleResultMapper;
    private final MovementMapper movementMapper;

    @Override
    public TrainingReport getReport(Long sessionId) {
        TrainingSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("训练记录不存在");
        }

        Movement movement = movementMapper.selectById(session.getMovementId());

        // Parse error summary
        Map<String, Integer> errorStats = parseJsonMap(session.getErrorSummary());

        // Build angle curves from angle results
        List<AngleResult> angleResults = angleResultMapper.findBySessionId(sessionId);
        Map<String, List<Double>> angleCurves = new LinkedHashMap<>();
        for (AngleResult ar : angleResults) {
            angleCurves.computeIfAbsent(ar.getJointName(), k -> new ArrayList<>())
                    .add(ar.getAngleValue().doubleValue());
        }

        // Find previous session for comparison
        TrainingSession prevSession = findPreviousSession(
                session.getUserId(), session.getMovementId(), sessionId);

        List<String> improvements = new ArrayList<>();
        if (prevSession != null && prevSession.getScore() != null && session.getScore() != null) {
            int delta = session.getScore() - prevSession.getScore();
            if (delta > 0) {
                improvements.add("得分提升 " + delta + " 分");
            }
            Map<String, Integer> prevErrors = parseJsonMap(prevSession.getErrorSummary());
            for (var entry : errorStats.entrySet()) {
                int prevCount = prevErrors.getOrDefault(entry.getKey(), 0);
                if (entry.getValue() < prevCount) {
                    improvements.add("\"" + entry.getKey() + "\" 减少 " + (prevCount - entry.getValue()) + " 次");
                }
            }
        }

        return TrainingReport.builder()
                .sessionId(sessionId)
                .movementId(session.getMovementId())
                .movementName(movement != null ? movement.getName() : "未知")
                .score(session.getScore() != null ? session.getScore() : 0)
                .durationSeconds(session.getDurationSeconds() != null ? session.getDurationSeconds() : 0)
                .repCount(session.getRepCount() != null ? session.getRepCount() : 0)
                .errorStats(errorStats)
                .angleCurves(angleCurves)
                .previousScore(prevSession != null ? prevSession.getScore() : null)
                .scoreChange(prevSession != null && prevSession.getScore() != null && session.getScore() != null
                        ? session.getScore() - prevSession.getScore() : null)
                .improvements(improvements)
                .build();
    }

    @Override
    public PageResult<TrainingSession> listSessions(Long userId, int page, int size) {
        Page<TrainingSession> p = new Page<>(page, size);
        LambdaQueryWrapper<TrainingSession> wrapper = new LambdaQueryWrapper<TrainingSession>()
                .eq(TrainingSession::getUserId, userId)
                .orderByDesc(TrainingSession::getCreatedAt);
        Page<TrainingSession> result = sessionMapper.selectPage(p, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Object getUserStats(Long userId) {
        List<TrainingSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<TrainingSession>().eq(TrainingSession::getUserId, userId));

        int totalSessions = sessions.size();
        int totalDuration = sessions.stream().mapToInt(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0).sum();
        double avgScore = sessions.stream()
                .filter(s -> s.getScore() != null && s.getScore() > 0)
                .mapToInt(TrainingSession::getScore)
                .average().orElse(0);
        int bestScore = sessions.stream().mapToInt(s -> s.getScore() != null ? s.getScore() : 0).max().orElse(0);

        // Per-movement stats
        Map<String, Object> movementStats = sessions.stream()
                .collect(Collectors.groupingBy(
                        s -> {
                            Movement m = movementMapper.selectById(s.getMovementId());
                            return m != null ? m.getName() : "未知";
                        },
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            double avg = list.stream().filter(s -> s.getScore() != null && s.getScore() > 0)
                                    .mapToInt(TrainingSession::getScore).average().orElse(0);
                            return Map.of("count", list.size(), "avgScore", Math.round(avg * 10.0) / 10.0);
                        })
                ));

        return Map.of(
                "totalSessions", totalSessions,
                "totalDurationMinutes", totalDuration / 60,
                "avgScore", Math.round(avgScore * 10.0) / 10.0,
                "bestScore", bestScore,
                "movementStats", movementStats
        );
    }

    @Override
    public Object compareSessions(Long sessionId1, Long sessionId2) {
        TrainingReport report1 = getReport(sessionId1);
        TrainingReport report2 = getReport(sessionId2);

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("session1", Map.of("id", sessionId1, "score", report1.getScore(), "errors", report1.getErrorStats()));
        comparison.put("session2", Map.of("id", sessionId2, "score", report2.getScore(), "errors", report2.getErrorStats()));

        // Diff
        Map<String, Integer> diff = new HashMap<>();
        Set<String> allErrors = new HashSet<>(report1.getErrorStats().keySet());
        allErrors.addAll(report2.getErrorStats().keySet());
        for (String err : allErrors) {
            int c1 = report1.getErrorStats().getOrDefault(err, 0);
            int c2 = report2.getErrorStats().getOrDefault(err, 0);
            diff.put(err, c2 - c1);
        }
        comparison.put("errorDiff", diff);
        comparison.put("scoreDiff", report2.getScore() - report1.getScore());

        return comparison;
    }

    private TrainingSession findPreviousSession(Long userId, Long movementId, Long currentSessionId) {
        List<TrainingSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<TrainingSession>()
                        .eq(TrainingSession::getUserId, userId)
                        .eq(TrainingSession::getMovementId, movementId)
                        .lt(TrainingSession::getId, currentSessionId)
                        .orderByDesc(TrainingSession::getCreatedAt)
                        .last("LIMIT 1"));
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    @Override
    public Object getWeeklyReport(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<TrainingSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<TrainingSession>()
                        .eq(TrainingSession::getUserId, userId)
                        .between(TrainingSession::getCreatedAt, weekStart.atStartOfDay(), weekEnd.plusDays(1).atStartOfDay())
                        .orderByDesc(TrainingSession::getCreatedAt));

        int totalSessions = sessions.size();
        int totalMinutes = sessions.stream()
                .mapToInt(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() / 60 : 0).sum();
        double avgScore = sessions.stream()
                .filter(s -> s.getScore() != null && s.getScore() > 0)
                .mapToInt(TrainingSession::getScore).average().orElse(0);

        // Daily breakdown
        Map<String, Object> dailyBreakdown = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            String key = day.toString();
            List<TrainingSession> daySessions = sessions.stream()
                    .filter(s -> s.getCreatedAt().toLocalDate().equals(day))
                    .toList();
            dailyBreakdown.put(key, Map.of(
                    "count", daySessions.size(),
                    "avgScore", daySessions.stream()
                            .filter(s -> s.getScore() != null && s.getScore() > 0)
                            .mapToInt(TrainingSession::getScore).average().orElse(0),
                    "totalMinutes", daySessions.stream()
                            .mapToInt(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() / 60 : 0).sum()
            ));
        }

        // Trend analysis
        String trend = "stable";
        if (sessions.size() >= 2) {
            double firstHalf = sessions.subList(0, sessions.size() / 2).stream()
                    .filter(s -> s.getScore() != null && s.getScore() > 0)
                    .mapToInt(TrainingSession::getScore).average().orElse(0);
            double secondHalf = sessions.subList(sessions.size() / 2, sessions.size()).stream()
                    .filter(s -> s.getScore() != null && s.getScore() > 0)
                    .mapToInt(TrainingSession::getScore).average().orElse(0);
            if (secondHalf - firstHalf > 5) trend = "improving";
            else if (firstHalf - secondHalf > 5) trend = "declining";
        }

        return Map.of(
                "weekStart", weekStart.toString(),
                "weekEnd", weekEnd.toString(),
                "totalSessions", totalSessions,
                "totalMinutes", totalMinutes,
                "avgScore", Math.round(avgScore * 10.0) / 10.0,
                "trend", trend,
                "dailyBreakdown", dailyBreakdown
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return JSON.parseObject(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
