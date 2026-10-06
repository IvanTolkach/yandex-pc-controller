package dev.tolkach.diagnostics;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class AlbumSearchInspector {

    private final Page page;

    public AlbumSearchInspector(Page page) {
        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }

        this.page = page;
    }

    public void inspect() {
        System.out.println();
        System.out.println("===== ALBUM SEARCH INSPECTOR =====");
        System.out.println("URL: " + page.url());

        inspectTestIds();
        inspectAlbumLinks();
        inspectAlbumCandidates();
    }

    private void inspectTestIds() {
        System.out.println();
        System.out.println("===== ELEMENTS WITH data-test-id =====");

        Locator elements = page.locator("[data-test-id]");

        int count = elements.count();

        System.out.println("Found: " + count);

        for (int i = 0; i < count; i++) {
            Locator element = elements.nth(i);

            String testId = element.getAttribute("data-test-id");

            String tag = element.evaluate("element => element.tagName").toString();

            String text = element.textContent();

            if (text == null) {
                text = "";
            }

            text = text.replaceAll("\\s+", " ").trim();

            if (text.length() > 150) {
                text = text.substring(0, 150) + "...";
            }

            String href = element.getAttribute("href");

            if ((testId != null && (testId.toLowerCase().contains("album") || testId.toLowerCase().contains("search"))) ||
                    (href != null && href.contains("/album"))) {
                System.out.println("--- element " + i + " ---");
                System.out.println("test-id = " + testId);
                System.out.println("tag = " + tag);
                System.out.println("text = " + text);
                System.out.println("href = " + href);
            }
        }
    }

    private void inspectAlbumLinks() {
        System.out.println();
        System.out.println("===== ALBUM LINKS =====");

        Locator links = page.locator("a[href*='/album']");

        int count = links.count();

        System.out.println("Found: " + count);

        for (int i = 0; i < count; i++) {
            Locator link = links.nth(i);

            String href = link.getAttribute("href");

            String testId = link.getAttribute("data-test-id");

            String ariaLabel = link.getAttribute("aria-label");

            String text = link.textContent();

            if (text == null) {
                text = "";
            }

            text = text.replaceAll("\\s+", " ").trim();

            System.out.println();
            System.out.println("--- album link " + i + " ---");
            System.out.println("href = " + href);
            System.out.println("test-id = " + testId);
            System.out.println("aria-label = " + ariaLabel);
            System.out.println("text = " + text);
            inspectParent(link);
        }
    }

    private void inspectParent(Locator link) {
        Locator parent = link.locator("xpath=..");

        String parentTestId = parent.getAttribute("data-test-id");

        String parentTag = parent.evaluate("element => element.tagName").toString();

        System.out.println("parent tag = " + parentTag);
        System.out.println("parent test-id = " + parentTestId);

        Locator buttons = parent.locator("button[data-test-id]");

        int buttonCount = buttons.count();

        if (buttonCount > 0) {
            System.out.println("parent buttons = " + buttonCount);

            for (int i = 0; i < buttonCount; i++) {
                Locator button = buttons.nth(i);

                System.out.println("  button test-id = " + button.getAttribute("data-test-id"));
                System.out.println("  button aria-label = " + button.getAttribute("aria-label"));
            }
        }
    }

    private void inspectAlbumCandidates() {

        System.out.println();
        System.out.println("===== POSSIBLE ALBUM CARDS =====");

        Locator elements = page.locator("[data-test-id]");

        int count = elements.count();

        for (int i = 0; i < count; i++) {
            Locator element = elements.nth(i);

            String testId = element.getAttribute("data-test-id");

            if (testId == null) {
                continue;
            }

            String normalized = testId.toLowerCase();

            if (!normalized.contains("album")) {
                continue;
            }

            System.out.println();
            System.out.println("--- candidate " + i + " ---");
            System.out.println("test-id = " + testId);
            System.out.println("tag = " + element.evaluate("element => element.tagName"));

            String text = element.textContent();

            if (text == null) {
                text = "";
            }

            System.out.println("text = " + text.replaceAll("\\s+", " ").trim());

            Locator links = element.locator("a[href*='/album']");

            System.out.println("album links inside = " + links.count());

            Locator buttons = element.locator("button[data-test-id]");

            System.out.println("buttons inside = " + buttons.count());
        }
    }
}
