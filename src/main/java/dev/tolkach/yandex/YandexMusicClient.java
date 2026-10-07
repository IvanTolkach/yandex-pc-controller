package dev.tolkach.yandex;

import dev.tolkach.music.NoActivePlaybackException;
import dev.tolkach.yandex.matching.AlbumMatcher;
import dev.tolkach.yandex.matching.TrackMatcher;
import dev.tolkach.yandex.model.AlbumSearchResult;
import dev.tolkach.yandex.model.PlaybackState;
import dev.tolkach.yandex.model.TrackSearchResult;

import java.time.Duration;
import java.util.List;

public class YandexMusicClient {

    private static final Duration PLAYBACK_VERIFICATION_TIMEOUT = Duration.ofSeconds(10);
    private final YandexMusicPage musicPage;
    private final TrackMatcher trackMatcher;
    private final AlbumMatcher albumMatcher;
    private final PlayerBar playerBar;

    public YandexMusicClient(YandexMusicPage musicPage, TrackMatcher trackMatcher, AlbumMatcher albumMatcher) {
        if (musicPage == null) {
            throw new IllegalArgumentException("musicPage must not be null");
        }

        if (trackMatcher == null) {
            throw new IllegalArgumentException("trackMatcher must not be null");
        }

        this.musicPage = musicPage;
        this.trackMatcher = trackMatcher;
        this.albumMatcher = albumMatcher;
        this.playerBar = musicPage.playerBar();
    }

    public PlaybackState playTrack(String title, String artist) {
        validateTrackRequest(title, artist);

        String query = artist == null ? title : title + " " + artist;

        SearchPage searchPage = musicPage.openSearch();

        searchPage.search(query);

        List<TrackSearchResult> results = searchPage.getTrackResults();

        if (results.isEmpty()) {
            throw new IllegalStateException("No tracks found for query: " + query);
        }

        TrackSearchResult selected;

        if (artist == null) {
            selected = results.getFirst();
        }
        else {
            selected = trackMatcher.findBestMatch(results, title, artist)
                    .orElseThrow(() -> new IllegalStateException("Track not found: " + title + " - " + artist));
        }

        searchPage.playTrack(selected);

        return playerBar.waitUntilPlaying(selected.trackId(), PLAYBACK_VERIFICATION_TIMEOUT);
    }

    public PlaybackState playQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }

        SearchPage searchPage = musicPage.openSearch();

       searchPage.search(query.trim());

       searchPage.playTopResult();

       return playerBar.waitUntilPlaying(PLAYBACK_VERIFICATION_TIMEOUT);
    }

    public PlaybackState playAlbum(String title, String artist) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Album title must not be empty");
        }

        String query = artist == null || artist.isBlank() ? title.trim() : (title.trim() + " " + artist.trim());

        SearchPage searchPage = musicPage.openSearch();

        searchPage.search(query);

        List<AlbumSearchResult> results = searchPage.getAlbumResults();

        if (results.isEmpty()) {
            throw new IllegalStateException("No albums found for query: " + query);
        }

        AlbumSearchResult selected = albumMatcher
                .findBestMatch(results, title)
                .orElseThrow(() -> new IllegalStateException("Album not found: " + title));

        searchPage.playAlbum(selected);

        return playerBar.waitUntilPlayingFromAlbum(selected.albumId(), PLAYBACK_VERIFICATION_TIMEOUT);
    }

    public PlaybackState getPlaybackState() {
        ensurePlayerControlsAvailable();
        return playerBar.getState();
    }

    public void pause() {
        ensurePlayerControlsAvailable();
        playerBar.pause();
    }

    public void resume() {
        ensurePlayerControlsAvailable();
        playerBar.resume();
    }

    public void next() {
        ensurePlayerControlsAvailable();
        playerBar.next();
    }

    public void previous() {
        ensurePlayerControlsAvailable();
        playerBar.previous();
    }

    public void volumeUp() {
        ensurePlayerControlsAvailable();
        playerBar.volumeUp();
    }

    public void volumeDown() {
        ensurePlayerControlsAvailable();
        playerBar.volumeDown();
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

    private void ensurePlayerControlsAvailable() {
        if (!musicPage.ensureStandardPlayerBar()) {
            throw new NoActivePlaybackException();
        }
    }
}
