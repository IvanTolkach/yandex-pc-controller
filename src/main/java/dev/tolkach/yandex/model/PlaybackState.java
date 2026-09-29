package dev.tolkach.yandex.model;

import java.util.List;

public record PlaybackState(String trackId, String title, List<String> artists, boolean playing) {

    public PlaybackState {
        if (trackId == null || trackId.isBlank()) {
            throw new IllegalArgumentException("Track id must not be empty");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Track title must not be empty");
        }

        if (artists == null || artists.isEmpty()) {
            throw new IllegalArgumentException("Track must have at least one artist");
        }

        artists =List.copyOf(artists);
    }

    public String artistDisplayName() {
        return  String.join(", ", artists);
    }

    @Override
    public String toString() {
        return title + " - " + artistDisplayName() + " [" + trackId + ", " + (playing ? "PLAYING" : "PAUSED") + "]";
    }
}
