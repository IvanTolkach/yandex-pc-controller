package dev.tolkach.yandex;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class SearchPage {

    private static final String SEARCH_INPUT_TEST_ID = "SEARCH_PAGE_SEARCH_INPUT";

    private final Page page;

    public SearchPage(Page page) {
        this.page = page;
    }

    public void search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalStateException("Search query must not be empty");
        }

        Locator searchInput = page.getByTestId(SEARCH_INPUT_TEST_ID);

        searchInput.fill(query);
    }

    public void printSearchElements() {

        Locator elements = page.locator("[data-test-id*='SEARCH']");

        System.out.println("Search-related elements: " + elements.count());

        for (int i = 0; i < elements.count(); i++) {
            Locator element = elements.nth(i);

            System.out.println("[" + i + "] " + element.getAttribute("data-test-id"));
        }
    }

    public Page page() {
        return page;
    }
}
