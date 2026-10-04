package dev.tolkach.websocket;

import dev.tolkach.protocol.music.MusicCommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    private final AgentSessionRegistry registry;
    private final AgentCommandService commandService;
    private final JsonMapper jsonMapper;

    public AgentWebSocketHandler(AgentSessionRegistry registry, AgentCommandService commandService, JsonMapper jsonMapper) {
        this.registry = registry;
        this.commandService = commandService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String deviceId = extractDeviceId(session);

        registry.register(deviceId, session);

        log.info("Agent connected: deviceId={}, sessionId={}", deviceId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            MusicCommandResponse response = jsonMapper.readValue(message.getPayload(), MusicCommandResponse.class);

            commandService.completeResponse(response);
        }
        catch (Exception exception) {
            log.error("Failed to parse response from agent. sessionId={}", session.getId(), exception);
        }
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

        int separator = path.lastIndexOf('/');

        if (separator < 0 || separator == path.length() - 1) {
            throw new IllegalArgumentException("Device id is missing: " + path);
        }

        return path.substring(separator + 1).trim();
    }
}
