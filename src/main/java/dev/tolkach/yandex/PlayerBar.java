package dev.tolkach.yandex;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import dev.tolkach.yandex.model.PlaybackState;

import java.net.URI;
import java.time.Duration;
import java.util.List;

public class PlayerBar {
    private static final String PLAYERBAR_TEST_ID = "PLAYERBAR_DESKTOP";

    private static final String TRACK_TITLE_TEST_ID = "TRACK_TITLE";

    private static final String ARTIST_TITLE_TEST_ID = "SEPARATED_ARTIST_TITLE";

    private static final String PAUSE_BUTTON_TEST_ID = "PAUSE_BUTTON";

    private static final String PLAY_BUTTON_TEST_ID = "PLAY_BUTTON";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final Page page;

    public PlayerBar(Page page) {
        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }

        this.page = page;
    }

    public PlaybackState getState() {
        Locator playerBar = getPlayerBar();

        Locator titleLink = playerBar.getByTestId(TRACK_TITLE_TEST_ID);

        String title = titleLink.innerText().trim();

        String href = titleLink.getAttribute("href");

        if (href == null || href.isBlank()) {
            throw new IllegalStateException("Current track href is missing");
        }

        String trackId = extractQueryParameter(href, "trackId");

        Locator artistLinks = playerBar.getByTestId(ARTIST_TITLE_TEST_ID);

        List<String> artists = artistLinks.allInnerTexts()
                        .stream()
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList();

        boolean playing = isPlaying(playerBar);

        return new PlaybackState(trackId, title, artists, playing);
    }

    public PlaybackState waitUntilPlaying(String expectedTrackId, Duration timeout) {
        Locator playerBar = getPlayerBar();

        String selector = "[data-test-id='TRACK_TITLE']" + "[href*='trackId=" + expectedTrackId + "']";

        Locator expectedTrack = playerBar.locator(selector);

        expectedTrack.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(timeout.toMillis()));

        Locator pauseButton = playerBar.getByTestId(PAUSE_BUTTON_TEST_ID);

        pauseButton.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(timeout.toMillis()));

        PlaybackState state = getState();

        if (!expectedTrackId.equals(state.trackId())) {
            throw new IllegalStateException("Unexpected track in player. " +
                    "Expected trackId=" + expectedTrackId + ", actual=" + state.trackId());
        }

        if (!state.playing()) {
            throw new IllegalStateException("Track is selected but not playing: " + state);
        }

        return state;
    }

    public boolean isPlaying() {
        return isPlaying(getPlayerBar());
    }

    public void pause() {
        Locator playerBar = getPlayerBar();

        Locator pauseButton = playerBar.getByTestId(PAUSE_BUTTON_TEST_ID);

        if (pauseButton.count() == 0) {
            return;
        }

        pauseButton.click();
    }

    public void resume() {
        Locator playerBar = getPlayerBar();

        Locator playButton = playerBar.getByTestId(PLAY_BUTTON_TEST_ID);

        if (playButton.count() == 0) {
            return;
        }

        playButton.click();
    }

    public void next() {
        Locator playerBar = getPlayerBar();

        playerBar.getByTestId("NEXT_TRACK_BUTTON").click();
    }

    public void previous() {
        Locator playerBar = getPlayerBar();

        playerBar.getByTestId("PREVIOUS_TRACK_BUTTON").click();
    }

    private Locator getPlayerBar() {
        Locator playerBar = page.getByTestId(PLAYERBAR_TEST_ID);

        playerBar.first().waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(DEFAULT_TIMEOUT.toMillis()));

        return playerBar.first();
    }

    private boolean isPlaying(Locator playerBar) {
        Locator pauseButton = playerBar.getByTestId(PAUSE_BUTTON_TEST_ID);

        return pauseButton.count() > 0 && pauseButton.first().isVisible();
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

        throw new IllegalArgumentException("Parameter '" + parameter + "' not found in URL: " + href);
    }
}
