package dev.tolkach.diagnostics;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class SearchResultInspector {
    private static final String TRACK_CARD_TEST_ID = "SEARCH_TRACK_CARD";
    private static final int MAX_INSPECTED_TRACK_CARDS = 5;

    public void inspect(Page page) {
        Locator cards = page.getByTestId(TRACK_CARD_TEST_ID);

        int count = cards.count();

        System.out.println();
        System.out.println("===== TRACK CARDS =====");
        System.out.println("Track cards found: " + count);

        int cardsToInspect = Math.min(count, MAX_INSPECTED_TRACK_CARDS);

        for (int i = 0; i < cardsToInspect; i++) {
            inspectCard(cards.nth(i), i);
        }
    }

    private void inspectCard(Locator card, int index) {
        System.out.println();
        System.out.println("===== TRACK CARD #" + index + " =====");

        System.out.println("Text:");
        System.out.println(indent(card.innerText()));

        System.out.println();
        System.out.println("Card attributes:");
        printAttribute(card, "role");
        printAttribute(card, "aria-label");
        printAttribute(card, "data-test-id");

        System.out.println();
        System.out.println("Links:");
        inspectLinks(card);

        System.out.println();
        System.out.println("Buttons:");
        inspectButtons(card);

        System.out.println();
        System.out.println("Test IDs inside card:");
        inspectTestIds(card);
    }

    private void inspectLinks(Locator card) {

        Locator links = card.locator("a");

        System.out.println("Found: " + links.count());

        for (int i = 0; i < links.count(); i++) {

            Locator link = links.nth(i);

            System.out.println("--- link " + i + " ---");
            System.out.println("text = " + safe(link.innerText()));
            System.out.println("aria-label = " + safe(link.getAttribute("aria-label")));
            System.out.println("href = " + safe(link.getAttribute("href")));
            System.out.println("data-test-id = " + safe(link.getAttribute("data-test-id")));
        }
    }

    private void inspectButtons(Locator card) {

        Locator buttons = card.locator("button");

        System.out.println("Found: " + buttons.count());

        for (int i = 0; i < buttons.count(); i++) {

            Locator button = buttons.nth(i);

            System.out.println("--- button " + i + " ---");
            System.out.println("text = " + safe(button.innerText()));
            System.out.println("aria-label = " + safe(button.getAttribute("aria-label")));
            System.out.println("data-test-id = " + safe(button.getAttribute("data-test-id")));
            System.out.println("title = " + safe(button.getAttribute("title")));
        }
    }

    private void inspectTestIds(Locator card) {

        Locator elements = card.locator("[data-test-id]");

        System.out.println("Found: " + elements.count());

        for (int i = 0; i < elements.count(); i++) {
            Locator element = elements.nth(i);
            System.out.println("[" + i + "] " + safe(element.getAttribute("data-test-id")));
        }
    }

    private void printAttribute(Locator element, String attribute) {
        System.out.println(attribute + " = " + safe(element.getAttribute(attribute)));
    }

    private String indent(String value) {
        if (value == null || value.isBlank()) {
            return "  <empty>";
        }
        return value.replace("\r", "").replace("\n", "\n  ");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
