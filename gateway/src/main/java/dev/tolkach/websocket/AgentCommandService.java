package dev.tolkach.websocket;

import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.protocol.music.MusicCommandResponse;
import dev.tolkach.protocol.music.MusicErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AgentCommandService {

    private static final Logger log = LoggerFactory.getLogger(AgentCommandService.class);

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

        future.whenComplete((response, error) -> pending.remove(request.requestId(), pendingCommand));

        scheduleTimeout(deviceId, request.requestId(), future);

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
            log.warn("Failed to send command to agent: deviceId={}, requestId={}", deviceId, request.requestId(), exception);

            future.complete(agentUnavailable(request.requestId(), deviceId));
        }

        return future;
    }

    public void completeResponse(MusicCommandResponse response) {
        PendingCommand pendingCommand = pending.get(response.requestId());

        if (pendingCommand == null) {
            log.debug("Ignoring response for non-pending request: {}", response.requestId());

            return;
        }

        boolean completed = pendingCommand.future().complete(response);

        if (!completed) {
            log.debug("Request was already completed: {}", response.requestId());
        }
    }

    public void failPending(String deviceId) {
        pending.forEach((requestId, pendingCommand) -> {
            if (!pendingCommand.deviceId().equals(deviceId)) {
                return;
            }

            pendingCommand.future().complete(agentUnavailable(requestId, deviceId));
        });
    }

    private void scheduleTimeout(String deviceId, String requestId, CompletableFuture<MusicCommandResponse> future) {
        CompletableFuture
                .delayedExecutor(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> {
                    boolean completed = future.complete(agentTimeout(requestId, deviceId));

                    if (completed) {
                        log.warn("Agent command timed out: deviceId={}, requestId={}, timeout={}s", deviceId, requestId, RESPONSE_TIMEOUT.toSeconds());
                    }
                });
    }

    private MusicCommandResponse agentUnavailable(String requestId, String deviceId) {
        return new MusicCommandResponse(
                requestId,
                false,
                null,
                MusicErrorCode.AGENT_UNAVAILABLE,
                "Desktop agent is unavailable: " + deviceId
        );
    }

    private MusicCommandResponse agentTimeout(String requestId, String deviceId) {
        return new MusicCommandResponse(
                requestId,
                false,
                null,
                MusicErrorCode.AGENT_TIMEOUT,
                "Desktop agent did not respond within " + RESPONSE_TIMEOUT.toSeconds() + " seconds: " + deviceId
        );
    }

    private record PendingCommand(String deviceId, CompletableFuture<MusicCommandResponse> future) {
    }
}
