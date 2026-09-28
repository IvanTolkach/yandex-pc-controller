package dev.tolkach;

import com.microsoft.playwright.Page;
import dev.tolkach.browser.CdpBrowser;
import dev.tolkach.diagnostics.PageInspector;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {

    private static final String CDP_URL = "http://127.0.0.1:9222";

    static void main() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        try (CdpBrowser cdpBrowser = CdpBrowser.connect(CDP_URL)) {

            System.out.println("Connected!");

            List<Page> pages = cdpBrowser.pages();

            if (pages.isEmpty()) {
                throw new IllegalStateException("No pages found in Yandex Music");
            }

            Page page = pages.getFirst();

            PageInspector inspector = new PageInspector();
            inspector.inspect(page);
            inspector.saveHtml(page, "yandex-music-page.html");
            inspector.saveScreenshot(page, "yandex-music-page.png");
        }
        catch (Exception e) {
            System.out.println("Connection refused.");
        }
    }
}
