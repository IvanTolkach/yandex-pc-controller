package dev.tolkach.gateway.client;

import dev.tolkach.music.protocol.MusicCommandDispatcher;
import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.protocol.music.MusicCommandResponse;
import tools.jackson.databind.json.JsonMapper;

import javax.management.relation.RoleList;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GatewayClient implements AutoCloseable {

    private static final long INITIAL_RECONNECT_DELAY_SECONDS = 1;
    private static final long MAX_RECONNECT_DELAY_SECONDS = 30;
    private static final long PING_INTERVAL_SECONDS = 15;

    private final HttpClient httpClient;
    private final URI uri;
    private final JsonMapper jsonMapper;
    private final MusicCommandDispatcher dispatcher;

    private final ExecutorService commandExecutor;
    private final ScheduledExecutorService scheduler;

    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempt = new AtomicInteger(0);

    private volatile WebSocket webSocket;
    private volatile boolean connected;
    private volatile ScheduledFuture<?> pingTask;

    public GatewayClient(String gatewayUri, String  deviceId, MusicCommandDispatcher dispatcher) {
        if (gatewayUri == null || gatewayUri.isBlank()) {
            throw new IllegalArgumentException("gatewayUri must not be empty");
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be empty");
        }

        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }

        this.httpClient = HttpClient.newHttpClient();

        this.uri = URI.create(gatewayUri + "/" + deviceId);

        this.jsonMapper = JsonMapper.builder().build();

        this.dispatcher = dispatcher;

        this.commandExecutor = Executors.newSingleThreadExecutor(Thread
                .ofVirtual()
                .name("music-command-", 0)
                .factory()
        );

        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual()
                .name("gateway-scheduler-", 0)
                .factory()
        );
    }

    public void connect() {
        if (closing.get()) {
            throw new IllegalStateException("GatewayClient is closed");
        }

        connectInternal().join();
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() throws Exception {
        if (!closing.compareAndSet(false, true)) {
            return;
        }

        connected = false;

        ScheduledFuture<?> currentPingTask = pingTask;

        if (currentPingTask != null) {
            currentPingTask.cancel(false);
        }

        WebSocket socket = webSocket;

        webSocket = null;

        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }

        commandExecutor.shutdown();

        scheduler.shutdown();

        System.out.println("Gateway client stopped.");
    }

    private CompletableFuture<WebSocket> connectInternal() {
        if (closing.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("GatewayClient is closed"));
        }

        if (!connecting.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Connection attempt already in progress"));
        }

        return httpClient
                .newWebSocketBuilder()
                .buildAsync(uri, new Listener())
                .whenComplete(
                        (socket, error) -> {
                            connecting.set(false);
                            if (error != null) {
                                scheduleReconnect(error);
                            }
                        }
                );
    }

    private void scheduleReconnect(Throwable cause) {
        if (closing.get()) {
            return;
        }

        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        int attempt = reconnectAttempt.incrementAndGet();

        long delay = calculateReconnectDelay(attempt);

        System.out.println("Gateway connection lost. Reconnect attempt " + attempt + " in " + delay + " seconds.");

        scheduler.schedule(() -> {
            reconnectScheduled.set(false);

            if (closing.get()) {
                return;
            }

            connectInternal();
        }, delay, TimeUnit.SECONDS);
    }

    private long calculateReconnectDelay(int attempt) {
        long delay = INITIAL_RECONNECT_DELAY_SECONDS;

        for (int i = 1; i < attempt; i++) {
            delay = Math.min(delay * 2, MAX_RECONNECT_DELAY_SECONDS);
        }

        return Math.min(delay, MAX_RECONNECT_DELAY_SECONDS);
    }

    private void startPingTask(WebSocket socket) {
        ScheduledFuture<?> oldTask = pingTask;

        if (oldTask != null) {
            oldTask.cancel(false);
        }

        pingTask = scheduler.scheduleAtFixedRate(() -> {
            if (!connected || closing.get()) {
                return;
            }
            try {
                socket.sendPing(ByteBuffer.allocate(0));
            } catch (Exception exception) {
                System.err.println("Failed to send gateway ping: " + exception.getMessage());
            }
        }, PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void handleCommand(String payload) {
        try {
            MusicCommandRequest request = jsonMapper.readValue(payload, MusicCommandRequest.class);

            System.out.println("Parsed command: " + request);

            MusicCommandResponse response = dispatcher.dispatch(request);

            System.out.println("Command result: " + response);

            String responseJson = jsonMapper.writeValueAsString(response);

            WebSocket socket = webSocket;

            if (socket == null || !connected) {
                throw new IllegalStateException("Gateway WebSocket is not connected");
            }

            socket.sendText(responseJson, true);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private class Listener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket socket) {
            if (closing.get()) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");

                return;
            }

            webSocket = socket;
            connected = true;

            reconnectAttempt.set(0);

            System.out.println("Gateway WebSocket connected.");

            startPingTask(socket);

            socket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            if (closing.get()) {
                socket.request(1);
                return null;
            }

            commandExecutor.submit(() -> handleCommand(data.toString()));

            socket.request(1);

            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connected = false;

            System.err.println("Gateway WebSocket error: " + error.getMessage());

            scheduleReconnect(error);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
            connected = false;

            if (webSocket == socket) {
                webSocket = null;
            }

            ScheduledFuture<?> currentPingTask = pingTask;

            if (currentPingTask != null) {
                currentPingTask.cancel(false);
            }

            System.out.println("Gateway WebSocket closed: " + statusCode + " " + reason);

            if (!closing.get()) {
                scheduleReconnect(new IllegalStateException("WebSocket closed"));
            }

            socket.request(1);

            return null;
        }
    }
}
