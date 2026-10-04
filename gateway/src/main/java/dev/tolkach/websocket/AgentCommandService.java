package dev.tolkach.websocket;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AgentCommandService {

    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(15);

    private final AgentSessionRegistry registry;
    private final JsonMapper jsonMapper;

    private final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();

    public AgentCommandService(AgentSessionRegistry registry, JsonMapper jsonMapper) {
        this.registry = registry;
        this.jsonMapper = jsonMapper;
    }

    public String sendCommand(String deviceId, String commandJson) throws Exception {
        String requestId = extractRequestId(commandJson);

        WebSocketSession session = registry
                .find(deviceId)
                .orElseThrow(() -> new IllegalStateException("Agent is not connected: " + deviceId));

        CompletableFuture<String> future = new CompletableFuture<>();

        CompletableFuture<String> existing = pendingResponses.putIfAbsent(requestId, future);

        if (existing != null) {
            throw  new IllegalArgumentException("Duplicate requestedId: " + requestId);
        }

        try {
            System.out.println("Sending command to agent " + deviceId + ": " + commandJson);

            session.sendMessage(new TextMessage(commandJson));

            String response = future.get(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            System.out.println("Response from agent: " + response);

            return response;
        }
        finally {
            pendingResponses.remove(requestId, future);
        }
    }

    public void completeResponse(String responseJson) {
        System.out.println("Completing response: " + responseJson);

        String requestId = extractRequestId(responseJson);

        CompletableFuture<String> future = pendingResponses.get(requestId);

        if (future == null) {
            System.out.println("No pending request for requestId=" + requestId);

            return;
        }

        future.complete(responseJson);
    }

    private String extractRequestId(String json) {
        try {
            JsonNode root = jsonMapper.readTree(json);

            JsonNode requestId = root.get("requestId");

            if (requestId == null || requestId.isNull() || requestId.asString().isBlank()) {
                throw new IllegalArgumentException("requestId is missing");
            }

            return requestId.asString();
        }
        catch (IllegalArgumentException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new IllegalArgumentException("Invalid command JSON", exception);
        }

    }
}
