package dev.tolkach.yandex;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;

public class YandexMusicPage {

    private static final String SEARCH_NAVIGATION_TEST_ID = "NAVBAR_NAVIGATION_ITEM_SEARCH";

    private static final String PLAYERBAR_TEST_ID = "PLAYERBAR_DESKTOP";

    private static final String SEARCH_PAGE_TEST_ID = "SEARCH_PAGE";

    private final Page page;

    public YandexMusicPage(Page page) {
        this.page = page;
    }

    public SearchPage openSearch() {
        if (page.url().startsWith("music-application://desktop/search")) {
            return new SearchPage(page);
        }

        Locator searchNavigation = page.getByTestId(SEARCH_NAVIGATION_TEST_ID);

        try {
            searchNavigation.click(new Locator.ClickOptions().setTimeout(3_000));
        }
        catch (TimeoutError e) {
            System.out.println("Search navigation is unstable. Using DOM click fallback.");

            searchNavigation.evaluate("element => element.click()");
        }

        page.getByTestId(SEARCH_PAGE_TEST_ID).waitFor(
                new Locator
                        .WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(10_000)
        );

        return new SearchPage(page);
    }

    public PlayerBar playerBar() {
        return new PlayerBar(page);
    }

    public Page page() {
        return page;
    }

    public boolean ensureStandardPlayerBar() {
        Locator playerBar = page.getByTestId(PLAYERBAR_TEST_ID).first();

        if (playerBar.count() > 0 && playerBar.isVisible()) {
            return true;
        }

        System.out.println("Standard player bar is not visible. Switching to search page...");

        openSearch();

        playerBar = page.getByTestId(PLAYERBAR_TEST_ID).first();

        try {
            playerBar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(3000));

            System.out.println("Standard player bar restored.");

            return true;
        }
        catch (TimeoutError error) {
            return false;
        }
    }
}
