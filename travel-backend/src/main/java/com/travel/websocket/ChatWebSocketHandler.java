package com.travel.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getParam(session, "userId");
        if (userId != null && !userId.isBlank()) {
            SESSIONS.put(userId, session);
            log.info("WebSocket connected: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> msg = OBJECT_MAPPER.readValue(message.getPayload(), Map.class);
        String receiverId = String.valueOf(msg.get("receiverId"));
        WebSocketSession receiverSession = SESSIONS.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(new TextMessage(message.getPayload()));
        }
    }

    public static void sendToUser(String userId, Object payload) {
        try {
            WebSocketSession session = SESSIONS.get(userId);
            if (session == null || !session.isOpen()) {
                return;
            }
            session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(payload)));
        } catch (Exception e) {
            log.warn("WebSocket push failed: userId={}", userId, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getParam(session, "userId");
        if (userId != null && !userId.isBlank()) {
            SESSIONS.remove(userId);
            log.info("WebSocket closed: userId={}", userId);
        }
    }

    private String getParam(WebSocketSession session, String key) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && key.equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }
}

