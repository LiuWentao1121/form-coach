package com.formcoach.service.impl;

import com.alibaba.fastjson2.JSON;
import com.formcoach.common.BusinessException;
import com.formcoach.common.ErrorCode;
import com.formcoach.dto.FeedbackResponse;
import com.formcoach.dto.FeedbackResponse.ErrorInfo;
import com.formcoach.dto.FrameRequest;
import com.formcoach.dto.TrainingReport;
import com.formcoach.entity.Movement;
import com.formcoach.entity.TrainingSession;
import com.formcoach.mapper.AngleResultMapper;
import com.formcoach.mapper.FrameDataMapper;
import com.formcoach.mapper.MovementMapper;
import com.formcoach.mapper.TrainingSessionMapper;
import com.formcoach.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {

    private final TrainingSessionMapper sessionMapper;
    private final FrameDataMapper frameDataMapper;
    private final AngleResultMapper angleResultMapper;
    private final MovementMapper movementMapper;
    private final AngleCalculator angleCalculator;
    private final ErrorDetector errorDetector;
    private final ScoreCalculator scoreCalculator;
    private final AchievementService achievementService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_KEY_PREFIX = "training:session:";
    private static final long SESSION_TTL_HOURS = 24;

    @Override
    public Long startSession(Long userId, Long movementId) {
        Movement movement = movementMapper.selectById(movementId);
        if (movement == null) {
            throw new BusinessException(ErrorCode.MOVEMENT_NOT_FOUND);
        }

        TrainingSession session = new TrainingSession();
        session.setUserId(userId);
        session.setMovementId(movementId);
        session.setScore(0);
        session.setDurationSeconds(0);
        session.setRepCount(0);
        session.setCreatedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        SessionContext ctx = new SessionContext();
        ctx.sessionId = session.getId();
        ctx.movementId = movementId;
        ctx.movementName = movement.getName();
        ctx.standardAngles = movement.getStandardAngles();
        ctx.tips = movement.getTips();
        ctx.startTime = System.currentTimeMillis();
        ctx.frameScores = new ArrayList<>();
        ctx.errorCounts = new HashMap<>();
        ctx.angleCurves = new HashMap<>();
        saveSessionContext(session.getId(), ctx);

        log.info("Training session started: id={}, userId={}, movement={}", session.getId(), userId, movement.getName());
        return session.getId();
    }

    @Override
    public FeedbackResponse processFrame(FrameRequest frame) {
        SessionContext ctx = getSessionContext(frame.getSessionId());
        if (ctx == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }

        String sessionKey = String.valueOf(frame.getSessionId());

        // 1. Calculate joint angles (data-driven: reads standardAngles JSON)
        Map<String, Double> angles = angleCalculator.calculate(frame, ctx.standardAngles, sessionKey);

        // 2. Detect errors
        List<ErrorInfo> errors = errorDetector.detect(
                frame.getMovementType(), angles, ctx.standardAngles, ctx.tips);

        // 3. Calculate frame score
        int frameScore = scoreCalculator.calculateFrameScore(errors);
        ctx.frameScores.add(frameScore);
        ctx.totalFrames++;

        // 4. Track errors
        if (!errors.isEmpty()) {
            ctx.errorFrames++;
            for (ErrorInfo e : errors) {
                ctx.errorCounts.merge(e.getTip(), 1, Integer::sum);
            }

            // Save error frame data
            com.formcoach.entity.FrameData frameData = new com.formcoach.entity.FrameData();
            frameData.setSessionId(frame.getSessionId());
            frameData.setFrameIndex(frame.getFrameIndex());
            frameData.setJointData(JSON.toJSONString(frame.getLandmarks()));
            frameData.setIsErrorFrame(1);
            frameData.setErrorType(errors.get(0).getJoint());
            frameData.setCreatedAt(LocalDateTime.now());
            frameDataMapper.insert(frameData);
        }

        // 5. Track angle curves (sample every 5 frames to save space)
        if (frame.getFrameIndex() % 5 == 0) {
            for (var entry : angles.entrySet()) {
                ctx.angleCurves.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }

        // Persist updated context to Redis periodically (every 10 frames)
        if (frame.getFrameIndex() % 10 == 0) {
            saveSessionContext(frame.getSessionId(), ctx);
        }

        // 6. Save angle results
        for (var entry : angles.entrySet()) {
            com.formcoach.entity.AngleResult ar = new com.formcoach.entity.AngleResult();
            ar.setSessionId(frame.getSessionId());
            ar.setFrameIndex(frame.getFrameIndex());
            ar.setJointName(entry.getKey());
            ar.setAngleValue(BigDecimal.valueOf(entry.getValue()));
            ar.setAngleStatus(errors.isEmpty() ? "NORMAL" :
                    errors.stream().anyMatch(e -> e.getJoint().contains(entry.getKey())) ? "ERROR" : "NORMAL");
            ar.setCreatedAt(LocalDateTime.now());
            angleResultMapper.insert(ar);
        }

        // 7. Build feedback
        return FeedbackResponse.builder()
                .frameIndex(frame.getFrameIndex())
                .isCorrect(errors.isEmpty())
                .score(frameScore)
                .errors(errors)
                .build();
    }

    @Override
    public TrainingReport endSession(Long sessionId) {
        SessionContext ctx = getSessionContext(sessionId);
        if (ctx == null) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }
        deleteSessionContext(sessionId);

        // Clear angle smoothing state
        angleCalculator.clearSession(String.valueOf(sessionId));

        // Calculate final session score
        int sessionScore = scoreCalculator.calculateSessionScore(
                ctx.frameScores, ctx.totalFrames, ctx.errorFrames);
        int durationSeconds = (int) ((System.currentTimeMillis() - ctx.startTime) / 1000);

        // Update session record
        TrainingSession session = sessionMapper.selectById(sessionId);
        session.setScore(sessionScore);
        session.setDurationSeconds(durationSeconds);
        session.setRepCount(ctx.repCount);
        session.setErrorSummary(JSON.toJSONString(ctx.errorCounts));
        session.setAvgAngles(JSON.toJSONString(computeAvgAngles(ctx.angleCurves)));
        sessionMapper.updateById(session);

        // Check achievements
        achievementService.checkAndUnlock(session.getUserId(), session);

        // Get previous session for comparison
        TrainingSession prevSession = findPreviousSession(session.getUserId(), session.getMovementId(), sessionId);

        // Build report
        Map<String, List<Double>> curves = ctx.angleCurves.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<String> improvements = new ArrayList<>();
        if (prevSession != null && prevSession.getScore() != null) {
            int delta = sessionScore - prevSession.getScore();
            if (delta > 0) {
                improvements.add("得分提升 " + delta + " 分，继续保持！");
            }
            // Compare error types
            Map<String, Integer> prevErrors = parseErrorSummary(prevSession.getErrorSummary());
            for (var entry : ctx.errorCounts.entrySet()) {
                int prevCount = prevErrors.getOrDefault(entry.getKey(), 0);
                if (entry.getValue() < prevCount) {
                    improvements.add("\"" + entry.getKey() + "\" 次数减少 " + (prevCount - entry.getValue()) + " 次");
                }
            }
        }

        return TrainingReport.builder()
                .sessionId(sessionId)
                .movementId(ctx.movementId)
                .movementName(ctx.movementName)
                .score(sessionScore)
                .durationSeconds(durationSeconds)
                .repCount(ctx.repCount)
                .errorStats(ctx.errorCounts)
                .angleCurves(curves)
                .previousScore(prevSession != null ? prevSession.getScore() : null)
                .scoreChange(prevSession != null ? sessionScore - prevSession.getScore() : null)
                .improvements(improvements)
                .build();
    }

    @Override
    public TrainingSession getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    private TrainingSession findPreviousSession(Long userId, Long movementId, Long currentSessionId) {
        // Simple: get the most recent session before this one
        List<TrainingSession> sessions = sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TrainingSession>()
                        .eq(TrainingSession::getUserId, userId)
                        .eq(TrainingSession::getMovementId, movementId)
                        .lt(TrainingSession::getId, currentSessionId)
                        .orderByDesc(TrainingSession::getCreatedAt)
                        .last("LIMIT 1"));
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    private Map<String, Double> computeAvgAngles(Map<String, List<Double>> curves) {
        Map<String, Double> avgs = new HashMap<>();
        for (var entry : curves.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            avgs.put(entry.getKey(), Math.round(avg * 10.0) / 10.0);
        }
        return avgs;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> parseErrorSummary(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return JSON.parseObject(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    // Redis-based session context with ConcurrentHashMap fallback
    private final Map<Long, SessionContext> localFallback = new java.util.concurrent.ConcurrentHashMap<>();

    private void saveSessionContext(Long sessionId, SessionContext ctx) {
        try {
            String key = SESSION_KEY_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, ctx, SESSION_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("Redis unavailable, using local cache for session {}", sessionId);
            localFallback.put(sessionId, ctx);
        }
    }

    private SessionContext getSessionContext(Long sessionId) {
        try {
            String key = SESSION_KEY_PREFIX + sessionId;
            SessionContext ctx = (SessionContext) redisTemplate.opsForValue().get(key);
            if (ctx != null) return ctx;
        } catch (Exception e) {
            log.debug("Redis unavailable, falling back to local cache for session {}", sessionId);
        }
        return localFallback.get(sessionId);
    }

    private void deleteSessionContext(Long sessionId) {
        try {
            String key = SESSION_KEY_PREFIX + sessionId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("Redis unavailable during delete for session {}", sessionId);
        }
        localFallback.remove(sessionId);
    }

    private static class SessionContext implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        Long sessionId;
        Long movementId;
        String movementName;
        String standardAngles;
        String tips;
        long startTime;
        int totalFrames;
        int errorFrames;
        int repCount;
        List<Integer> frameScores;
        Map<String, Integer> errorCounts;
        Map<String, List<Double>> angleCurves;
    }
}
