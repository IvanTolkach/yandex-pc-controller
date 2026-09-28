package dev.tolkach.yandex.model;

import java.util.List;

public record TrackSearchResult(String title, List<String> artists, String duration, String albumId, String trackId) {

    public TrackSearchResult {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Track title must not be empty");
        }

        if (artists == null || artists.isEmpty()) {
            throw new IllegalArgumentException("Track artist must not be empty");
        }

        if (artists.stream().anyMatch(artist -> artist == null || artist.isBlank())) {
            throw new IllegalArgumentException("Artist names must not be empty");
        }

        if (trackId == null || trackId.isBlank()) {
            throw new IllegalArgumentException("Track id must not be empty");
        }

        artists = List.copyOf(artists);
    }

    public String artistDisplayName() {
        return String.join(", ", artists);
    }

    @Override
    public String toString() {
        return title + " - " + artistDisplayName() + " [" + duration + ", trackId=" + trackId + "]";
    }
}
