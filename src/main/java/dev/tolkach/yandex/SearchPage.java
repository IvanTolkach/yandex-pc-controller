package dev.tolkach.yandex;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import dev.tolkach.yandex.model.AlbumSearchResult;
import dev.tolkach.yandex.model.TrackSearchResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchPage {

    private static final String SEARCH_INPUT_TEST_ID = "SEARCH_PAGE_SEARCH_INPUT";

    private static final String TRACK_CARD_TEST_ID = "SEARCH_TRACK_CARD";

    private static final String TRACK_TITLE_TEST_ID = "TRACK_TITLE";

    private static final String ARTIST_TITLE_TEST_ID = "SEPARATED_ARTIST_TITLE";

    private static final String TRACK_DURATION_TEST_ID = "TRACK_DURATION";

    private static final String PLAY_BUTTON_TEST_ID = "PLAY_BUTTON";

    private static final String ALBUM_CONTAINER_SELECTOR = "[data-test-id='ALBUM_ITEM'], " +
            "[data-test-id='HORIZONTAL_ALBUM_CARD']";

    private static final String ALBUM_TITLE_LINK_TEST_ID = "ALBUM_TITLE_LINK";

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

        page.waitForFunction("""
                expected => {
                    const url = new URL(window.location.href);
                    return url.searchParams.get("text") === expected;
                }
                """, query);

        waitForTrackResult();
    }

    public void printSearchElements() {

        Locator elements = page.locator("[data-test-id*='SEARCH']");

        System.out.println("Search-related elements: " + elements.count());

        for (int i = 0; i < elements.count(); i++) {
            Locator element = elements.nth(i);

            System.out.println("[" + i + "] " + element.getAttribute("data-test-id"));
        }
    }

    public List<TrackSearchResult> getTrackResults() {
        Locator cards = page.getByTestId(TRACK_CARD_TEST_ID);

        cards.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));

        int count = cards.count();

        List<TrackSearchResult> results = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Locator card = cards.nth(i);
            results.add(readTrack(card));
        }

        return  List.copyOf(results);
    }

    public void playTrack(TrackSearchResult track) {
        Locator card = findCardByTrackId(track.trackId());

        Locator playButton = card.getByTestId(PLAY_BUTTON_TEST_ID);

        playButton.click();
    }

    public List<AlbumSearchResult> getAlbumResults() {
        Locator albumLinks = page.locator("a[href*='/album?albumId=']");

        albumLinks.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));

        int count = albumLinks.count();

        Map<String, AlbumSearchResult> uniqueAlbums = new LinkedHashMap<>();

        for (int i = 0; i < count; i++) {
            Locator link = albumLinks.nth(i);

            String href = link.getAttribute("href");

            if (href == null || href.isBlank()) {
                continue;
            }

            String albumId = extractQueryParameter(href, "albumId");

            String title = link.innerText().trim();

            if (title.isBlank()) {
                continue;
            }

            uniqueAlbums.putIfAbsent(albumId, new AlbumSearchResult(title, albumId));
        }

        return List.copyOf(uniqueAlbums.values());
    }

    public void playAlbum(AlbumSearchResult album) {
        Locator containers = page.locator(ALBUM_CONTAINER_SELECTOR);

        for (int i = 0; i < containers.count(); i++) {
            Locator container = containers.nth(i);

            Locator albumLink = container.locator("a[href*='/album?albumId=']");

            if (albumLink.count() == 0) {
                continue;
            }

            String href = albumLink.first().getAttribute("href");

            if (href == null) {
                continue;
            }

            String albumId = extractQueryParameter(href, "albumId");

            if (!album.albumId().equals(albumId)) {
                continue;
            }

            Locator playButton = container.getByTestId(PLAY_BUTTON_TEST_ID);

            if (playButton.count() == 0) {
                continue;
            }

            if (!playButton.first().isEnabled()) {
                throw new IllegalStateException("Album play button is disabled: " + album);
            }

            playButton.first().click();

            return;
        }

        throw new IllegalStateException("Album container not found: " + album);
    }

    private void waitForTrackResult() {
        Locator cards = page.getByTestId(TRACK_CARD_TEST_ID);

        cards.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
    }

    private TrackSearchResult readTrack(Locator card) {
        Locator titleLink = card.getByTestId(TRACK_TITLE_TEST_ID);
        Locator artistLinks = card.getByTestId(ARTIST_TITLE_TEST_ID);
        Locator durationElement = card.getByTestId(TRACK_DURATION_TEST_ID);

        String title = titleLink.innerText().trim();
        List<String> artists = artistLinks.allInnerTexts()
                .stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .toList();
        String duration = durationElement.innerText().trim();
        String href = titleLink.getAttribute("href");

        if (href == null || href.isBlank()) {
            throw new IllegalStateException("Track href is missing for: " + title);
        }

        String albumId = extractQueryParameter(href, "albumId");
        String trackId = extractQueryParameter(href, "trackId");

        return new TrackSearchResult(title, artists, duration, albumId, trackId);
    }

    private Locator findCardByTrackId(String trackId) {
        Locator cards = page.getByTestId(TRACK_CARD_TEST_ID);

        for (int i = 0; i < cards.count(); i++) {
            Locator card = cards.nth(i);

            Locator titleLink = card.getByTestId(TRACK_TITLE_TEST_ID);

            String href = titleLink.getAttribute("href");

            if (href == null) {
                continue;
            }

            String currentTrackId = extractQueryParameter(href, "trackId");

            if (trackId.equals(currentTrackId)) {
                return card;
            }
        }
        throw new IllegalStateException("Track card not found. trackId=" + trackId);
    }

    private String extractQueryParameter(String href, String parameter) {
        URI uri = URI.create(href);

        String query = uri.getRawQuery();

        if (query == null) {
            throw new IllegalArgumentException("URL has no query parameters: " + href);
        }

        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);

            if (pair.length == 2 && parameter.equals(pair[0])) {
                return pair[1];
            }
        }
        throw new IllegalArgumentException("Parameter '" + parameter + "' not found in: " + href);
    }

    private AlbumSearchResult readAlbum(Locator albumItem) {
        Locator titleLink = albumItem.getByTestId(ALBUM_TITLE_LINK_TEST_ID);

        String title = titleLink.innerText().trim();

        String href = titleLink.getAttribute("href");

        if (href == null || href.isBlank()) {
            throw new IllegalStateException("Album href is missing for: " + title);
        }

        String albumId = extractQueryParameter(href, "albumId");

        return new AlbumSearchResult(title, albumId);
    }

    public Page page() {
        return page;
    }
}
