package dev.tolkach.yandex;

import com.microsoft.playwright.Locator;

public record TrackSearchResult(String title, String artist, Locator root) {
}
