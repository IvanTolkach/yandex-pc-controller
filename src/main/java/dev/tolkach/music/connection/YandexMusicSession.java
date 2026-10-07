package dev.tolkach.music.connection;

import com.microsoft.playwright.Page;
import dev.tolkach.browser.CdpBrowser;
import dev.tolkach.music.MusicController;

public class YandexMusicSession implements AutoCloseable {

    private final CdpBrowser cdpBrowser;
    private final Page page;
    private final MusicController controller;

    public YandexMusicSession(CdpBrowser cdpBrowser, Page page, MusicController controller) {
        this.cdpBrowser = cdpBrowser;
        this.page = page;
        this.controller = controller;
    }

    public MusicController controller() {
        return controller;
    }

    public boolean ping() {
        try {
            if (!cdpBrowser.browser().isConnected()) {
                return false;
            }

            if (page.isClosed()) {
                return false;
            }

            page.evaluate("() => true");

            return true;
        }
        catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isAlive() {
        return cdpBrowser.browser().isConnected() && !page.isClosed();
    }

    @Override
    public void close() {
        cdpBrowser.close();
    }
}
