package dev.tolkach.gateway.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class GatewayClient implements AutoCloseable {

    private final HttpClient httpClient;
    private final URI uri;

    private WebSocket webSocket;

    public GatewayClient(String gatewayUri, String  deviceId) {
        if (gatewayUri == null || gatewayUri.isBlank()) {
            throw new IllegalArgumentException("gatewayUri must not be empty");
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be empty");
        }

        this.httpClient = HttpClient.newHttpClient();

        this.uri = URI.create(gatewayUri + "/" + deviceId);
    }

    public void connect() {
        webSocket = httpClient
                .newWebSocketBuilder()
                .buildAsync(uri, new Listener())
                .orTimeout(10, TimeUnit.SECONDS)
                .join();
    }

    public boolean isConnected() {
        return webSocket != null;
    }

    @Override
    public void close() throws Exception {
        if (webSocket == null) {
            return;
        }

        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");

        webSocket = null;
    }

    private static class Listener implements WebSocket.Listener {

        @Override public void onOpen(WebSocket webSocket) {
            System.out.println("Gateway WebSocket connected.");

            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            System.out.println("Gateway message: " + data);

            webSocket.request(1);

            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            error.printStackTrace();
        }
    }
}
