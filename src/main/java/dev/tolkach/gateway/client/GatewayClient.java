package dev.tolkach.gateway.client;

import dev.tolkach.music.protocol.MusicCommandDispatcher;
import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.protocol.music.MusicCommandResponse;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GatewayClient implements AutoCloseable {

    private final HttpClient httpClient;
    private final URI uri;
    private final JsonMapper jsonMapper;
    private final MusicCommandDispatcher dispatcher;
    private final ExecutorService commandExecutor;

    private volatile WebSocket webSocket;
    private volatile boolean connected;

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
    }

    public void connect() {
        webSocket = httpClient
                .newWebSocketBuilder()
                .buildAsync(uri, new Listener())
                .orTimeout(10, TimeUnit.SECONDS)
                .join();
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() throws Exception {
        connected = false;

        WebSocket socket = webSocket;

        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }

        commandExecutor.shutdown();

        webSocket = null;
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
        public void onOpen(WebSocket webSocket) {
            connected = true;

            System.out.println("Gateway WebSocket connected.");

            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String payload = data.toString();

            commandExecutor.submit(() -> handleCommand(payload));

            webSocket.request(1);

            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connected = false;

            error.printStackTrace();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            connected = false;

            System.out.println("Gateway disconnected: " + statusCode + " " + reason);

            webSocket.request(1);

            return null;
        }
    }
}
