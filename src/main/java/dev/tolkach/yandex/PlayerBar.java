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

    private static final double PREVIOUS_TRACK_START_THRESHOLD_SECONDS = 3.0;

    private static final Duration PREVIOUS_TRACK_ACTION_TIMEOUT = Duration.ofSeconds(2);

    private static final double VOLUME_STEP = 0.10;

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

    public PlaybackState waitUntilPlayingFromAlbum(String expectedAlbumId, Duration timeout) {
        Locator playerBar = getPlayerBar();

        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            Locator titleLink = playerBar.getByTestId(TRACK_TITLE_TEST_ID);

            if (titleLink.count() > 0 && titleLink.first().isVisible()) {
                String href = titleLink.first().getAttribute("href");

                if (href != null && !href.isBlank()) {
                    String actualAlbumId = extractQueryParameter(href, "albumId");

                    boolean playing = isPlaying(playerBar);

                    if (expectedAlbumId.equals(actualAlbumId) && playing) {
                        return getState();
                    }
                }
            }

            page.waitForTimeout(100);
        }

        PlaybackState state = getState();

        throw new IllegalStateException("Album playback was not verified. " +
                        "Expected albumId=" +
                        expectedAlbumId +
                        ", actual state=" +
                        state
        );
    }

    public String getCurrentAlbumId() {
        Locator playerBar = getPlayerBar();

        Locator titleLink = playerBar.getByTestId(TRACK_TITLE_TEST_ID);

        String href = titleLink.getAttribute("href");

        if (href == null || href.isBlank()) {
            throw new IllegalStateException("Current track href is missing");
        }

        return extractQueryParameter(href, "albumId");
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
        Locator previousButton = getPlayerBar().getByTestId("PREVIOUS_TRACK_BUTTON");

        if (!previousButton.isEnabled()) {
            return;
        }

        String originalTrackId = getState().trackId();

        previousButton.click();

        if (waitUntilTrackChanged(originalTrackId, Duration.ofSeconds(2))) {
            return;
        }

        if (previousButton.isEnabled()) {
            previousButton.click();
        }
    }

    public void volumeUp() {
        changeVolume(VOLUME_STEP);
    }

    public void volumeDown() {
        changeVolume(-VOLUME_STEP);
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

    private double getCurrentPositionSeconds(Locator playerBar) {
        Locator slider = playerBar.getByTestId("TIMECODE_SLIDEBAR");

        String value = slider.getAttribute("value");

        if (value == null || value.isBlank()) {
            return 0.0;
        }

        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid timecode slider value: " + value, exception);
        }
    }

    private boolean waitUntilTrackChanged(String originalTrackId, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            PlaybackState currentState = getState();

            if (!originalTrackId.equals(currentState.trackId())) {
                return true;
            }

            page.waitForTimeout(100);
        }

        return false;
    }

    private void changeVolume(double delta) {
        Locator volumeSlider = getPlayerBar().getByTestId("CHANGE_VOLUME_SLIDER");

        String value = volumeSlider.getAttribute("value");

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Volume slider value is missing");
        }

        double currentVolume;

        try {
            currentVolume = Double.parseDouble(value);
        }
        catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid volume value: " + value, exception);
        }

        double newVolume = Math.max(0.0, Math.min(1.0, currentVolume + delta));

        if (Double.compare(currentVolume, newVolume) == 0) {
            return;
        }

        setVolume(volumeSlider, newVolume);
    }

    private void setVolume(Locator volumeSlider, double targetVolume) {
        double currentVolume = Double.parseDouble(volumeSlider.getAttribute("value"));

        int steps = (int) Math.round(Math.abs(targetVolume - currentVolume) / 0.01);

        if (steps == 0) {
            return;
        }

        volumeSlider.focus();

        String key = targetVolume > currentVolume ? "ArrowUp" : "ArrowDown";

        for (int i = 0; i < steps; i++) {
            volumeSlider.press(key);
        }
    }
}
