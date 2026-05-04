package com.kinlik.aicodehelper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class NotificationHandler extends TextWebSocketHandler {

    // username -> sessions
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri() != null ? session.getUri().getQuery() : "";
        String username = extractUsername(query);
        if (username != null) {
            userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("WebSocket连接建立: username={}, sessionId={}", username, session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到WebSocket消息: sessionId={}, payload={}", session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        userSessions.values().forEach(sessions -> sessions.remove(session));
        log.info("WebSocket连接关闭: sessionId={}", session.getId());
    }

    public void sendToUser(String username, String title, String body, String type) {
        Set<WebSocketSession> sessions = userSessions.get(username);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("用户无活跃WebSocket连接: username={}", username);
            return;
        }
        String payload = String.format(
                "{\"type\":\"%s\",\"title\":\"%s\",\"body\":\"%s\"}",
                type, title.replace("\"", "\\\""), body.replace("\"", "\\\"")
        );
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    log.error("WebSocket发送失败: username={}", username, e);
                }
            }
        }
    }

    private String extractUsername(String query) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("username=")) {
                return param.substring("username=".length());
            }
        }
        return null;
    }
}
