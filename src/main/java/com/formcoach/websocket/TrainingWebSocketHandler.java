package com.formcoach.websocket;

import com.alibaba.fastjson2.JSON;
import com.formcoach.dto.FeedbackResponse;
import com.formcoach.dto.FrameRequest;
import com.formcoach.service.TrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time training feedback.
 *
 * Protocol (JSON):
 *   Client → Server:
 *     {"action": "START", "userId": 1, "movementId": 1}
 *     {"action": "FRAME", "sessionId": 1, "frameIndex": 10, "movementType": "SQUAT", "landmarks": [...]}
 *     {"action": "END", "sessionId": 1}
 *
 *   Server → Client:
 *     {"type": "SESSION_STARTED", "sessionId": 1}
 *     {"type": "FEEDBACK", "data": {...FeedbackResponse}}
 *     {"type": "SESSION_ENDED", "data": {...TrainingReport}}
 *     {"type": "ERROR", "message": "..."}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingWebSocketHandler extends TextWebSocketHandler {

    private final TrainingService trainingService;

    // sessionId (WebSocket) → userId
    private final Map<String, Long> wsUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received: {}", payload);

        try {
            Map<String, Object> msg = JSON.parseObject(payload, Map.class);
            String action = (String) msg.get("action");

            switch (action) {
                case "START" -> handleStart(session, msg);
                case "FRAME" -> handleFrame(session, msg);
                case "END" -> handleEnd(session, msg);
                default -> sendError(session, "Unknown action: " + action);
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
            sendError(session, e.getMessage());
        }
    }

    private void handleStart(WebSocketSession session, Map<String, Object> msg) throws IOException {
        Long userId = toLong(msg.get("userId"));
        Long movementId = toLong(msg.get("movementId"));

        if (userId == null || movementId == null) {
            sendError(session, "Missing userId or movementId");
            return;
        }

        wsUserMap.put(session.getId(), userId);

        Long sessionId = trainingService.startSession(userId, movementId);
        sendJson(session, Map.of("type", "SESSION_STARTED", "sessionId", sessionId));
    }

    private void handleFrame(WebSocketSession session, Map<String, Object> msg) throws IOException {
        Long sessionId = toLong(msg.get("sessionId"));
        if (sessionId == null) {
            sendError(session, "Missing sessionId");
            return;
        }

        FrameRequest frame = parseFrameRequest(msg);
        FeedbackResponse feedback = trainingService.processFrame(frame);
        sendJson(session, Map.of("type", "FEEDBACK", "data", feedback));
    }

    private void handleEnd(WebSocketSession session, Map<String, Object> msg) throws IOException {
        Long sessionId = toLong(msg.get("sessionId"));
        if (sessionId == null) {
            sendError(session, "Missing sessionId");
            return;
        }

        var report = trainingService.endSession(sessionId);
        sendJson(session, Map.of("type", "SESSION_ENDED", "data", report));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        wsUserMap.remove(session.getId());
        log.info("WebSocket disconnected: {}, status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error: {}", session.getId(), exception);
    }

    private void sendJson(WebSocketSession session, Object data) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(JSON.toJSONString(data)));
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        sendJson(session, Map.of("type", "ERROR", "message", message));
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private FrameRequest parseFrameRequest(Map<String, Object> msg) {
        FrameRequest frame = new FrameRequest();
        frame.setSessionId(toLong(msg.get("sessionId")));
        frame.setFrameIndex(((Number) msg.get("frameIndex")).intValue());
        frame.setMovementType((String) msg.get("movementType"));

        // Parse landmarks
        java.util.List<Map<String, Object>> rawLandmarks = (java.util.List<Map<String, Object>>) msg.get("landmarks");
        if (rawLandmarks != null) {
            java.util.List<FrameRequest.Landmark> landmarks = rawLandmarks.stream().map(m -> {
                FrameRequest.Landmark lm = new FrameRequest.Landmark();
                lm.setX(toDouble(m.get("x")));
                lm.setY(toDouble(m.get("y")));
                lm.setZ(toDouble(m.get("z")));
                lm.setVisibility(toDouble(m.get("visibility")));
                return lm;
            }).toList();
            frame.setLandmarks(landmarks);
        }

        return frame;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
