package dev.tolkach.yandex;

import dev.tolkach.yandex.matching.TrackMatcher;
import dev.tolkach.yandex.model.TrackSearchResult;

import java.util.List;

public class YandexMusicClient {

    private final YandexMusicPage musicPage;
    private final TrackMatcher trackMatcher;

    public YandexMusicClient(YandexMusicPage musicPage, TrackMatcher trackMatcher) {
        this.musicPage = musicPage;
        this.trackMatcher = trackMatcher;
    }

    public TrackSearchResult playTrack(String title, String artist) {
        String query = buildSearchQuery(title, artist);

        SearchPage searchPage = musicPage.openSearch();

        searchPage.search(query);

        List<TrackSearchResult> results = searchPage.getTrackResults();

        TrackSearchResult selected = trackMatcher.findBestMatch(results, title, artist)
                .orElseThrow(() -> new IllegalStateException("Track not found: " + title + " - " + artist));

        searchPage.playTrack(selected);

        return selected;
    }

    private String buildSearchQuery(String title, String artist) {
        return (title + " " + artist).trim();
    }
}
