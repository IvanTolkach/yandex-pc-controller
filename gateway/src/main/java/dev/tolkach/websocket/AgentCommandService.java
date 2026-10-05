package dev.tolkach.websocket;

import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.protocol.music.MusicCommandResponse;
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

    private final Map<String, CompletableFuture<MusicCommandResponse>> pendingResponses = new ConcurrentHashMap<>();

    public AgentCommandService(AgentSessionRegistry registry, JsonMapper jsonMapper) {
        this.registry = registry;
        this.jsonMapper = jsonMapper;
    }

    public MusicCommandResponse sendCommand(String deviceId, MusicCommandRequest request) throws Exception {
        return sendCommandAsync(deviceId, request).get(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<MusicCommandResponse> sendCommandAsync(String deviceId, MusicCommandRequest request) {
        WebSocketSession session = registry.find(deviceId).orElseThrow(() -> new IllegalStateException("Agent is not connected: " + deviceId));

        String requestId = request.requestId();

        CompletableFuture<MusicCommandResponse> future = new CompletableFuture<>();

        CompletableFuture<MusicCommandResponse> existing = pendingResponses.putIfAbsent(requestId, future);

        if (existing != null) {
            throw new IllegalArgumentException("Duplicate requestId: " + requestId);
        }

        try {
            String json = jsonMapper.writeValueAsString(request);

            session.sendMessage(new TextMessage(json));

            future.orTimeout(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            future.whenComplete((response, error) -> pendingResponses.remove(requestId, future));

            return future;
        }
        catch (Exception exception) {
            pendingResponses.remove(requestId, future);

            future.completeExceptionally(exception);

            return future;
        }
    }

    public void completeResponse(MusicCommandResponse response) {
        CompletableFuture<MusicCommandResponse> future = pendingResponses.get(response.requestId());

        if (future != null) {
            future.complete(response);
        }
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
