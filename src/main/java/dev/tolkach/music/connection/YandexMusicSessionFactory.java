package dev.tolkach.music.connection;

import com.microsoft.playwright.Page;
import dev.tolkach.browser.CdpBrowser;
import dev.tolkach.music.MusicController;
import dev.tolkach.music.YandexMusicController;
import dev.tolkach.music.matching.TextNormalizer;
import dev.tolkach.yandex.YandexMusicClient;
import dev.tolkach.yandex.YandexMusicPage;
import dev.tolkach.yandex.matching.AlbumMatcher;
import dev.tolkach.yandex.matching.TrackMatcher;

public class YandexMusicSessionFactory {

    private final String cdpUrl;

    public YandexMusicSessionFactory(String cdpUrl) {
        this.cdpUrl = cdpUrl;
    }

    public YandexMusicSession connect() {
        CdpBrowser cdpBrowser = CdpBrowser.connect(cdpUrl);

        try {
            Page page = cdpBrowser.pages()
                    .stream()
                    .filter(this::isYandexMusicPage)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Yandex Music page not found"));

            YandexMusicPage musicPage = new YandexMusicPage(page);

            YandexMusicClient client = new YandexMusicClient(musicPage, new TrackMatcher(), new AlbumMatcher(new TextNormalizer()));

            MusicController controller = new YandexMusicController(client);

            return new YandexMusicSession(cdpBrowser, page, controller);
        }
        catch (RuntimeException exception) {
            cdpBrowser.close();
            throw exception;
        }
    }

    private boolean isYandexMusicPage(Page page) {
        return page.url().startsWith("music-application://desktop");
    }
}
