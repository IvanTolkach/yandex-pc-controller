package dev.tolkach.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.List;

public class CdpBrowser implements AutoCloseable{

    private final Playwright playwright;
    private final Browser browser;

    public CdpBrowser(Playwright playwright, Browser browser) {
        this.playwright = playwright;
        this.browser = browser;
    }

    public static CdpBrowser connect(String cdpUrl) {
        Playwright playwright = Playwright.create();

        try {


            playwright.selectors().setTestIdAttribute("data-test-id");

            Browser browser = playwright.chromium().connectOverCDP(cdpUrl,
                    new BrowserType.ConnectOverCDPOptions().setNoDefaults(true).setIsLocal(true)
            );

            return new CdpBrowser(playwright, browser);
        }
        catch (RuntimeException exception) {
            playwright.close();
            throw exception;
        }
    }

    public Browser browser(){
        return browser;
    }

    public List<Page> pages() {
        return browser.contexts().stream().flatMap(context -> context.pages().stream()).toList();
    }

    @Override
    public void close() {
        playwright.close();
    }
}
