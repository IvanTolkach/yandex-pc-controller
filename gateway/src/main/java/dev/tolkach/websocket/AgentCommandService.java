package dev.tolkach.websocket;

import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.protocol.music.MusicCommandResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
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

    private final Map<String, PendingCommand> pending = new ConcurrentHashMap<>();

    public AgentCommandService(AgentSessionRegistry registry, JsonMapper jsonMapper) {
        this.registry = registry;
        this.jsonMapper = jsonMapper;
    }

    public MusicCommandResponse sendCommand(String deviceId, MusicCommandRequest request) {
        return sendCommandAsync(deviceId, request).join();
    }

    public CompletableFuture<MusicCommandResponse> sendCommandAsync(String deviceId, MusicCommandRequest request) {
        WebSocketSession session = registry.find(deviceId).orElse(null);

        if (session == null) {
            return CompletableFuture.completedFuture(agentUnavailable(request.requestId(), deviceId));
        }

        CompletableFuture<MusicCommandResponse> future = new CompletableFuture<>();

        PendingCommand pendingCommand = new PendingCommand(deviceId, future);

        PendingCommand previous = pending.putIfAbsent(request.requestId(), pendingCommand);

        if (previous != null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Request already pending: " + request.requestId()));
        }

        future.orTimeout(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        future.whenComplete((response, error) -> pending.remove(request.requestId(), pendingCommand));

        try {
            String json = jsonMapper.writeValueAsString(request);

            synchronized (session) {
                if (!session.isOpen()) {
                    future.complete(agentUnavailable(request.requestId(), deviceId));

                    return future;
                }

                session.sendMessage(new TextMessage(json));
            }
        }
        catch (IOException | RuntimeException exception) {
            future.complete(agentUnavailable(request.requestId(), deviceId));
        }

        return future;
    }

    public void completeResponse(MusicCommandResponse response) {
        PendingCommand pendingCommand = pending.get(response.requestId());

        if (pendingCommand == null) {
            return;
        }

        pendingCommand.future().complete(response);
    }

    public void failPending(String deviceId) {
        pending.forEach((requestId, pendingCommand) -> {
            if (!pendingCommand.deviceId().equals(deviceId)) {
                return;
            }

            pendingCommand.future().complete(agentUnavailable(requestId, deviceId));
        });
    }

    private MusicCommandResponse agentUnavailable(String requestId, String deviceId) {
        return new MusicCommandResponse(
                requestId,
                false,
                null,
                "AGENT_UNAVAILABLE",
                "Desktop agent is unavailable: " + deviceId
        );
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

    private record PendingCommand(String deviceId, CompletableFuture<MusicCommandResponse> future) {
    }
}
