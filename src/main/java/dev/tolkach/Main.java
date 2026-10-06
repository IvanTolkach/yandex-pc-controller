package dev.tolkach;

import com.microsoft.playwright.Page;
import dev.tolkach.browser.CdpBrowser;
import dev.tolkach.gateway.client.GatewayClient;
import dev.tolkach.music.MusicController;
import dev.tolkach.music.YandexMusicController;
import dev.tolkach.music.commands.*;
import dev.tolkach.music.matching.TextNormalizer;
import dev.tolkach.music.protocol.*;
import dev.tolkach.yandex.YandexMusicClient;
import dev.tolkach.yandex.YandexMusicPage;
import dev.tolkach.yandex.matching.AlbumMatcher;
import dev.tolkach.yandex.matching.TrackMatcher;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final String CDP_URL = "http://127.0.0.1:9222";

    private static final String GATEWAY_URL = "ws://127.0.0.1:8080/ws/agent";

    private static final String DEVICE_ID = "my-pc";

    static void main() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        try (CdpBrowser cdpBrowser = CdpBrowser.connect(CDP_URL)) {
            Page page = cdpBrowser.pages().stream().findFirst().orElseThrow(
                    () -> new IllegalStateException("Yandex Music not found")
            );

            YandexMusicPage musicPage = new YandexMusicPage(page);

            YandexMusicClient client = new YandexMusicClient(musicPage, new TrackMatcher(), new AlbumMatcher(new TextNormalizer()));

            MusicController musicController = new YandexMusicController(client);

            MusicCommandHandler commandHandler = new MusicCommandHandler(musicController);

            MusicCommandDispatcher dispatcher = new MusicCommandDispatcher(new MusicCommandMapper(), commandHandler);

            try (GatewayClient gatewayClient = new GatewayClient(GATEWAY_URL, DEVICE_ID, dispatcher)) {
                gatewayClient.connect();

                System.out.println("Desktop agent is running.");
                System.out.println("Connected to gateway: " + GATEWAY_URL);
                System.out.println("Device id: " + DEVICE_ID);
                System.out.println("Press ENTER to shutdown.");
                System.in.read();
            }
        }
        catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
