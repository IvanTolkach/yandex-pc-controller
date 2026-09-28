package dev.tolkach;

import com.microsoft.playwright.Page;
import dev.tolkach.browser.CdpBrowser;
import dev.tolkach.diagnostics.PageInspector;
import dev.tolkach.yandex.SearchPage;
import dev.tolkach.yandex.YandexMusicPage;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final String CDP_URL = "http://127.0.0.1:9222";

    static void main() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        try (CdpBrowser cdpBrowser = CdpBrowser.connect(CDP_URL)) {

            System.out.println("Connected!");

            Page page = cdpBrowser.pages().stream().findFirst().orElseThrow(
                    () -> new IllegalStateException("Yandex Music not found")
            );

            YandexMusicPage musicPage = new YandexMusicPage(page);

            System.out.println("Opening search...");

            SearchPage searchPage = musicPage.openSearch();

            page.waitForTimeout(500);

            System.out.println("Search page opened: " + page.url());
            System.out.println("Searching for: Damage Kai Angel");

            searchPage.search("Damage Kai Angel");

            page.waitForTimeout(1500);

            searchPage.printSearchElements();

            System.out.println();
            System.out.println("===== SEARCH RESULT PAGE =====");
            System.out.println(page.locator("body").innerText());
        }
        catch (Exception e) {
            System.out.println("Connection lost. " + e.getMessage());
        }
    }
}
