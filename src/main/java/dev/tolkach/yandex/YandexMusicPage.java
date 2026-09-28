package dev.tolkach.yandex;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class YandexMusicPage {

    private static final String SEARCH_NAVIGATION_TEST_ID = "NAVBAR_NAVIGATION_ITEM_SEARCH";

    private final Page page;

    public YandexMusicPage(Page page) {
        this.page = page;
    }

    public SearchPage openSearch() {
        if (page.url().endsWith("/search")) {
            return new SearchPage(page);
        }

        Locator searchLink = page.getByTestId(SEARCH_NAVIGATION_TEST_ID);

        if (!searchLink.isVisible()) {
            throw new IllegalStateException("Search button not found or invisible.");
        }

        searchLink.click();

        return new SearchPage(page);
    }

    public Page page() {
        return page;
    }
}
