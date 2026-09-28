package dev.tolkach.yandex;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class YandexMusicPage {

    private static final String SEARCH_NAVIGATION_TEST_ID = "NAVBAR_NAVIGATION_ITEM_SEARCH";

    private final Page page;

    public YandexMusicPage(Page page) {
        this.page = page;
    }

    public void openSearch() {
        Locator searchLink = page.getByTestId(SEARCH_NAVIGATION_TEST_ID);

        if (!searchLink.isVisible()) {
            throw new IllegalStateException("Search button not found or invisible.");
        }

        searchLink.click();
    }

    public void search(String query) {
        //
    }

    public void playTrack(String title, String artist) {
        //
    }

    public Page page() {
        return page;
    }
}
