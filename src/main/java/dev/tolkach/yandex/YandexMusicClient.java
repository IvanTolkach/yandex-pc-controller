package dev.tolkach.yandex;

import dev.tolkach.yandex.matching.TrackMatcher;
import dev.tolkach.yandex.model.PlaybackState;
import dev.tolkach.yandex.model.TrackSearchResult;

import java.time.Duration;
import java.util.List;

public class YandexMusicClient {

    private static final Duration PLAYBACK_VERIFICATION_TIMEOUT = Duration.ofSeconds(10);
    private final YandexMusicPage musicPage;
    private final TrackMatcher trackMatcher;
    private final PlayerBar playerBar;

    public YandexMusicClient(YandexMusicPage musicPage, TrackMatcher trackMatcher) {
        if (musicPage == null) {
            throw new IllegalArgumentException("musicPage must not be null");
        }

        if (trackMatcher == null) {
            throw new IllegalArgumentException("trackMatcher must not be null");
        }

        this.musicPage = musicPage;
        this.trackMatcher = trackMatcher;
        this.playerBar = musicPage.playerBar();
    }

    public PlaybackState playTrack(String title, String artist) {
        validateTrackRequest(title, artist);

        String query = buildSearchQuery(title, artist);

        SearchPage searchPage = musicPage.openSearch();

        searchPage.search(query);

        List<TrackSearchResult> results = searchPage.getTrackResults();

        if (results.isEmpty()) {
            throw new IllegalStateException("No track results found for query: " + query);
        }

        TrackSearchResult selected = trackMatcher.findBestMatch(results, title, artist)
                .orElseThrow(() -> new IllegalStateException("Track not found: " + title + " - " + artist));

        System.out.println();
        System.out.println("Found " + results.size() + " track results.");

        System.out.println("Selected: " + selected);

        searchPage.playTrack(selected);

        System.out.println("Play command sent to Yandex Music.");

        PlaybackState state = playerBar.waitUntilPlaying(selected.trackId(), PLAYBACK_VERIFICATION_TIMEOUT);

        System.out.println("Playback verified: " + state);

        return state;
    }

    public PlaybackState getPlaybackState() {
        return playerBar.getState();
    }

    public void pause() {
        playerBar.pause();
    }

    public void resume() {
        playerBar.resume();
    }

    public void next() {
        playerBar.next();
    }

    public void previous() {
        playerBar.previous();
    }

    private String buildSearchQuery(String title, String artist) {
        return (title.trim() + " " + artist.trim()).trim();
    }

    private void validateTrackRequest(String title, String artist) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Track title must not be empty");
        }

        if (artist == null || artist.isBlank()) {
            throw new IllegalArgumentException("Artist must not be empty");
        }
    }
}
