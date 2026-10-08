package dev.tolkach.music.launcher;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CdpEndpointProbe {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(1);

    private final HttpClient httpClient;
    private final URI versionUri;

    public CdpEndpointProbe(String cdpUrl) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
        this.versionUri = URI.create(stripTrailingShash(cdpUrl) + "/json/version");
    }

    public boolean isAvailable() {
        HttpRequest request = HttpRequest.newBuilder(versionUri).timeout(REQUEST_TIMEOUT).GET().build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            return response.statusCode() >= 200 && response.statusCode() < 300;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private static String stripTrailingShash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }
}
