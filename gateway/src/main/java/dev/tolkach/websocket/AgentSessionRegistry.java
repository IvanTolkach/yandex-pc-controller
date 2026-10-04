package dev.tolkach.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentSessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(String deviceId, WebSocketSession session) {
        sessions.put(deviceId, session);
    }

    public Optional<WebSocketSession> find(String deviceId) {
        WebSocketSession session = sessions.get(deviceId);

        if (session == null || !session.isOpen()) {
            return Optional.empty();
        }

        return Optional.of(session);
    }

    public void unregister(String deviceId, WebSocketSession session) {
        sessions.remove(deviceId, session);
    }

    public boolean isConnected(String deviceId) {
        return find(deviceId).isPresent();
    }

}
