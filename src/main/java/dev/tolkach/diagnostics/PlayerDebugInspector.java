package dev.tolkach.diagnostics;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class PlayerDebugInspector {
    private static final String PLAYERBAR_TEST_ID = "PLAYERBAR_DESKTOP";

    private final Page page;

    public PlayerDebugInspector(Page page) {this.page = page;}

    public void inspect() {
        System.out.println();
        System.out.println("===== PLAYER DOM DEBUG =====");
        System.out.println("Page URL: " + page.url());

        Locator playerBar = page.getByTestId(PLAYERBAR_TEST_ID);

        System.out.println();
        System.out.println("PlayerBar count: " + playerBar.count());

        if (playerBar.count() == 0) {System.out.println("PLAYERBAR_DESKTOP not found.");
            return;
        }

        Locator root = playerBar.first();

        System.out.println();
        System.out.println("===== PLAYERBAR ROOT =====");
        System.out.println("role = " + safe(root.getAttribute("role")));
        System.out.println("aria-label = " + safe(root.getAttribute("aria-label")));
        System.out.println("text = " + safeText(root));

        inspectDescendants(root);
        inspectButtons(root);
        inspectLinks(root);
        inspectInputs(root);
    }

    private void inspectDescendants(Locator root) {
        System.out.println();
        System.out.println("===== PLAYERBAR TEST IDS =====");

        Locator elements = root.locator("[data-test-id]");

        int count = elements.count();

        System.out.println("Found: " + count);

        for (int i = 0; i < count; i++) {
            Locator element = elements.nth(i);

            System.out.println("--- element " + i + " ---");
            System.out.println("test-id = " + safe(element.getAttribute("data-test-id")));
            System.out.println("tag = " + safe((String) element.evaluate("el => el.tagName")));
            System.out.println("role = " + safe(element.getAttribute("role")));
            System.out.println("aria-label = " + safe(element.getAttribute("aria-label")));
            System.out.println("text = " + safeText(element));
        }
    }

    private void inspectButtons(Locator root) {
        System.out.println();
        System.out.println("===== PLAYERBAR BUTTONS =====");

        Locator buttons = root.locator("button");

        int count = buttons.count();

        System.out.println("Found: " + count);

        for (int i = 0; i < count; i++) {
            Locator button = buttons.nth(i);

            System.out.println("--- button " + i + " ---");
            System.out.println("test-id = " + safe(button.getAttribute("data-test-id")));
            System.out.println("aria-label = " + safe(button.getAttribute("aria-label")));
            System.out.println("aria-pressed = " + safe(button.getAttribute("aria-pressed")));
            System.out.println("text = " + safeText(button));

            Locator use = button.locator("use");

            if (use.count() > 0) {
                System.out.println("icon href = " + safe(use.first().getAttribute("xlink:href")));
                System.out.println("icon href attr = " + safe(use.first().getAttribute("href")));
            }
        }
    }

    private void inspectLinks(Locator root) {
        System.out.println();
        System.out.println("===== PLAYERBAR LINKS =====");

        Locator links = root.locator("a");

        int count = links.count();

        System.out.println("Found: " + count);

        for (int i = 0; i < count; i++) {

            Locator link = links.nth(i);

            System.out.println("--- link " + i + " ---");
            System.out.println("test-id = " + safe(link.getAttribute("data-test-id")));
            System.out.println("aria-label = " + safe(link.getAttribute("aria-label")));
            System.out.println("href = " + safe(link.getAttribute("href")));
            System.out.println("text = " + safeText(link));
        }
    }

    private void inspectInputs(Locator root) {
        System.out.println();
        System.out.println("===== PLAYERBAR INPUTS =====");

        Locator inputs = root.locator("input");

        int count = inputs.count();

        System.out.println("Found: " + count);

        for (int i = 0; i < count; i++) {
            Locator input = inputs.nth(i);

            System.out.println("--- input " + i + " ---");
            System.out.println("type = " + safe(input.getAttribute("type")));
            System.out.println("test-id = " + safe(input.getAttribute("data-test-id")));
            System.out.println("aria-label = " + safe(input.getAttribute("aria-label")));
            System.out.println("value = " + safe(input.getAttribute("value")));
            System.out.println("aria-valuetext = " + safe(input.getAttribute("aria-valuetext")));
        }
    }

    private String safeText(Locator locator) {
        String text = locator.textContent();

        if (text == null) {
            return "";
        }

        return text
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
