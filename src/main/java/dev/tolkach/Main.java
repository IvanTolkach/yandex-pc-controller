package dev.tolkach;

import com.microsoft.playwright.Page;
import dev.tolkach.browser.CdpBrowser;
import dev.tolkach.music.MusicCommandHandler;
import dev.tolkach.music.MusicController;
import dev.tolkach.music.YandexMusicController;
import dev.tolkach.music.commands.*;
import dev.tolkach.music.protocol.*;
import dev.tolkach.yandex.YandexMusicClient;
import dev.tolkach.yandex.YandexMusicPage;
import dev.tolkach.yandex.matching.TrackMatcher;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final String CDP_URL = "http://127.0.0.1:9222";

    static void main() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        try (CdpBrowser cdpBrowser = CdpBrowser.connect(CDP_URL)) {
            Page page = cdpBrowser.pages().stream().findFirst().orElseThrow(
                    () -> new IllegalStateException("Yandex Music not found")
            );

            YandexMusicPage musicPage = new YandexMusicPage(page);

            YandexMusicClient client = new YandexMusicClient(musicPage, new TrackMatcher());

            MusicController musicController = new YandexMusicController(client);

            MusicCommandHandler commandHandler = new MusicCommandHandler(musicController);

            MusicCommandDispatcher dispatcher = new MusicCommandDispatcher(new MusicCommandMapper(), commandHandler);

            MusicCommandRequest request = new MusicCommandRequest(
                    "test-001",
                    MusicAction.PLAY_TRACK,
                    "Damage",
                    "Kai Angel"
            );

            MusicCommandResponse response = dispatcher.dispatch(request);

            System.out.println(response);
        }
        catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
