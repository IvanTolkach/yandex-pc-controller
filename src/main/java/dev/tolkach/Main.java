package dev.tolkach;

import com.microsoft.playwright.Page;
import dev.tolkach.browser.CdpBrowser;
import dev.tolkach.music.MusicCommandHandler;
import dev.tolkach.music.MusicController;
import dev.tolkach.music.YandexMusicController;
import dev.tolkach.music.commands.*;
import dev.tolkach.yandex.YandexMusicClient;
import dev.tolkach.yandex.YandexMusicPage;
import dev.tolkach.yandex.matching.TrackMatcher;
import dev.tolkach.yandex.model.PlaybackState;

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

            TrackMatcher trackMatcher = new TrackMatcher();

            YandexMusicClient client = new YandexMusicClient(musicPage, trackMatcher);

            MusicController musicController = new YandexMusicController(client);

            MusicCommandHandler commandHandler = new MusicCommandHandler(musicController);

            PlaybackState state = commandHandler.handle(new PlayTrackCommand("damage", "Kai Angel"));

            System.out.println(state);
        }
        catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
