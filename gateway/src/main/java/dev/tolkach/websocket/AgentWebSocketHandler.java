package dev.tolkach.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;


public class AgentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    private final AgentSessionRegistry registry;

    public AgentWebSocketHandler(AgentSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String deviceId = extractDeviceId(session);

        registry.register(deviceId, session);

        log.info("Agent connected: deviceId={}, sessionId={}", deviceId, session.getId());

        session.sendMessage(new TextMessage("WELCOME"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Message from agent: sessionId={}, payload={}", session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String deviceId = extractDeviceId(session);

        registry.unregister(deviceId, session);

        log.info("Agent disconnected: deviceId={}, sessionId={}, status={}", deviceId, session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error: sessionId={}", session.getId(), exception);
    }

    private String extractDeviceId(WebSocketSession session) {
        URI uri = session.getUri();

        if (uri == null) {
            throw new IllegalStateException("WebSocket URI is missing");
        }

        String path = uri.getPath();

        if (path == null || path.isBlank()) {
            throw new IllegalStateException("WebSocket path is missing");
        }

        int separator = path.lastIndexOf('/');

        if (separator < 0 || separator == path.length() - 1) {
            throw new IllegalArgumentException("Device id is missing in WebSocket path: " + path);
        }

        return path.substring(separator + 1).trim();
    }
}
