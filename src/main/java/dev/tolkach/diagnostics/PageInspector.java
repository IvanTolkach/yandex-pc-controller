package dev.tolkach.diagnostics;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PageInspector {

    public void inspect(Page page) {
        System.out.println();
        System.out.println("===== PAGE =====");
        System.out.println("URL: " + page.url());
        System.out.println("TITE: " + page.title());

        System.out.println();
        System.out.println("===== INPUTS =====");
        dumpElements(page.locator("input"));

        System.out.println();
        System.out.println("===== BUTTONS =====");
        dumpElements(page.locator("button"));

        System.out.println();
        System.out.println("===== LINKS =====");
        dumpElements(page.locator("a"));

        System.out.println();
        System.out.println("===== BODY TEXT =====");
        dumpElements(page.locator("body"));
    }

    public void saveHtml(Page page, String path) {
        String html = page.content();

        Path file = Path.of(path);

        try {
            Files.writeString(file, html);
            System.out.println("HTML saved to: " + file.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save HTML", e);
        }
    }

    public void saveScreenshot(Page page, String path) {
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(path)).setFullPage(true));
        System.out.println("Screenshot saved to: " + Path.of(path).toAbsolutePath());
    }

    public void inspectSearchElements(Page page) {
        System.out.println();
        System.out.println("===== SEARCH CANDIDATES =====");

        inspectLocator("input", page.locator("input"));
        inspectLocator("textarea", page.locator("textarea"));
        inspectLocator("contenteditable", page.locator("[contenteditable='true']"));
        inspectLocator("role=textbox", page.getByRole(AriaRole.TEXTBOX));

        System.out.println();
        System.out.println("===== SEARCH TEST IDS =====");

        Locator searchTestIds = page.locator("[data-test-id*='SEARCH']");

        inspectLocator("data-test-id contains SEARCH", searchTestIds);
    }

    private void dumpElements(Locator locator) {
        int count = locator.count();

        System.out.println("Found: " + count);

        for (int i = 0; i < count; i++) {
            Locator element = locator.nth(i);

            String text = safe(element.innerText());
            String ariaLabel = element.getAttribute("aria-label");
            String placeholder = element.getAttribute("placeholder");
            String title = element.getAttribute("title");
            String role = element.getAttribute("role");
            String type = element.getAttribute("type");

            System.out.println("--- element " + i + " ---");
            System.out.println("text        = " + text);
            System.out.println("aria-label  = " + ariaLabel);
            System.out.println("placeholder = " + placeholder);
            System.out.println("title       = " + title);
            System.out.println("role        = " + role);
            System.out.println("type        = " + type);
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\n", " ").replace("\r", " ").trim();
    }

    private void inspectLocator(String name, Locator locator) {
        int count = locator.count();

        System.out.println();
        System.out.println(name + ": " + count);

        for (int i = 0; i < count; i++) {
            Locator element = locator.nth(i);

            System.out.println("--- element " + i + " ---");
            System.out.println("tag         = " + safe(element.getAttribute("el => el.tagName")));
            System.out.println("type        = " + safe(element.getAttribute("type")));
            System.out.println("placeholder = " + safe(element.getAttribute("placeholder")));
            System.out.println("aria-label  = " + safe(element.getAttribute("aria-label")));
            System.out.println("role        = " + safe(element.getAttribute("role")));
            System.out.println("test-id     = " + safe(element.getAttribute("data-test-id")));
            System.out.println("outerHTML   = " + safe(element.getAttribute("el => el.outerHTML")));
        }
    }
}
