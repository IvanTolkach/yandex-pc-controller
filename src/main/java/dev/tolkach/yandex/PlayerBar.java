package dev.tolkach.yandex;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import dev.tolkach.music.NoActivePlaybackException;
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

    private static final String NEXT_TRACK_TEST_ID = "NEXT_TRACK_BUTTON";

    private static final String PREVIOUS_TRACK_TEST_ID = "PREVIOUS_TRACK_BUTTON";

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
        Locator playerBar = requireExistingPlayerBar();

        Locator titleLink = playerBar.getByTestId(TRACK_TITLE_TEST_ID).first();

        if (titleLink.count() == 0) {
            throw new NoActivePlaybackException();
        }

        String title = titleLink.innerText().trim();

        String href = titleLink.getAttribute("href");

        String trackId = extractQueryParameter(href, "trackId");

        List<String> artists = playerBar
                .getByTestId(ARTIST_TITLE_TEST_ID)
                .all()
                .stream()
                .filter(Locator::isVisible)
                .map(Locator::innerText)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        Locator pauseButton = playerBar.getByTestId(PAUSE_BUTTON_TEST_ID);

        boolean playing = pauseButton.count() > 0 && pauseButton.first().isVisible();

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

    public PlaybackState waitUntilPlaying(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            try {
                PlaybackState state = getState();

                if (state != null && state.playing()) {
                    return state;
                }
            }
            catch (Exception ignored) {
            }

            page.waitForTimeout(100);
        }

        PlaybackState state = getState();

        throw new IllegalStateException("Playback did not start. " + "Current state: " + state);
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
        Locator playerBar = requireExistingPlayerBar();

        Locator pauseButton = playerBar.getByTestId(PAUSE_BUTTON_TEST_ID).first();

        if (pauseButton.count() > 0 && pauseButton.isVisible()) {
            pauseButton.click();
            return;
        }

        Locator playButton = playerBar.getByTestId(PLAY_BUTTON_TEST_ID).first();

        if (playButton.count() > 0 && playButton.isVisible()) {
            return;
        }

        throw new NoActivePlaybackException();
    }

    public void resume() {
        Locator playerBar = requireExistingPlayerBar();

        Locator pauseButton = playerBar.getByTestId(PAUSE_BUTTON_TEST_ID);

        if (pauseButton.count() > 0 && pauseButton.first().isVisible()) {
            return;
        }

        Locator playButton = playerBar.getByTestId(PLAY_BUTTON_TEST_ID);

        if (playButton.count() == 0 || !playButton.first().isVisible()) {
            throw new NoActivePlaybackException();
        }

        playButton.first().click();
    }

    public void next() {
        Locator playerBar = requireExistingPlayerBar();

        PlaybackState before = getState();

        Locator nextButton = playerBar.getByTestId("NEXT_TRACK_BUTTON").first();

        if (nextButton.count() == 0 || !nextButton.isVisible()) {
            throw new NoActivePlaybackException();
        }

        if (!nextButton.isEnabled()) {
            return;
        }

        nextButton.click();

        waitUntilTrackChangedAndPlaying(before.trackId(), DEFAULT_TIMEOUT);
    }

    public void previous() {
        Locator playerBar = requireExistingPlayerBar();

        PlaybackState before = getState();

        Locator previousButton = playerBar.getByTestId(PREVIOUS_TRACK_TEST_ID).first();

        if (previousButton.count() == 0 || !previousButton.isVisible()) {
            throw new NoActivePlaybackException();
        }

        if (!previousButton.isEnabled()) {
            return;
        }

        previousButton.click();

        boolean changed = waitUntilTrackChanged(before.trackId(), PREVIOUS_TRACK_ACTION_TIMEOUT);

        if (!changed) {
            previousButton = requireExistingPlayerBar().getByTestId(PREVIOUS_TRACK_TEST_ID).first();

            if (!previousButton.isEnabled()) {
                return;
            }

            previousButton.click();
        }

        waitUntilTrackChangedAndPlaying(before.trackId(), PREVIOUS_TRACK_ACTION_TIMEOUT);
    }

    public void volumeUp() {
        changeVolume(VOLUME_STEP);
    }

    public void volumeDown() {
        changeVolume(-VOLUME_STEP);
    }

    private Locator getPlayerBar() {
        Locator playerBar = page.getByTestId(PLAYERBAR_TEST_ID).first();

        playerBar.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT.toMillis())
        );

        return playerBar;
    }

    private Locator requireExistingPlayerBar() {
        Locator playerBar = page.getByTestId(PLAYERBAR_TEST_ID).first();

        if (playerBar.count() == 0 || !playerBar.isVisible()) {
            throw new NoActivePlaybackException();
        }

        return playerBar;
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

            if (currentState.trackId() != null && !currentState.trackId().equals(originalTrackId)) {

                return true;
            }

            page.waitForTimeout(100);
        }

        return false;
    }

    private void changeVolume(double delta) {
        Locator playerBar = requireExistingPlayerBar();

        Locator slider = playerBar.getByTestId("CHANGE_VOLUME_SLIDER").first();

        if (slider.count() == 0 || !slider.isVisible()) {
            throw new NoActivePlaybackException();
        }

        String valueAttribute = slider.getAttribute("value");

        if (valueAttribute == null) {
            throw new IllegalStateException("Volume slider has no value");
        }

        double current = Double.parseDouble(valueAttribute);

        double target = Math.max(0.0, Math.min(1.0, current + delta));

        setVolume(slider, current, target);
    }

    private void setVolume(Locator slider, double current, double target) {
        final double sliderStep = 0.01;

        int steps = (int) Math.round(Math.abs(target - current) / sliderStep);

        if (steps == 0) {
            return;
        }

        slider.focus();

        String key = target > current ? "ArrowUp" : "ArrowDown";

        for (int i = 0; i < steps; i++) {
            slider.press(key);
        }
    }

    private PlaybackState waitUntilTrackChangedAndPlaying(String previousTrackId, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();

        boolean resumeAttempts = false;

        while (System.nanoTime() < deadline) {
            PlaybackState state = getState();

            boolean trackChanged = state.trackId() != null && !state.trackId().equals(previousTrackId);

            if (trackChanged && state.playing()) {
                return state;
            }

            if (trackChanged && !state.playing() && !resumeAttempts) {
                resume();
                resumeAttempts = true;
            }

            page.waitForTimeout(100);
        }

        PlaybackState state = getState();

        throw new IllegalStateException("Next track did not start playing. Previous trackId=" + previousTrackId + ", current state=" + state);
    }
}
