package dev.tolkach.music.commands;

import java.util.Objects;

public record PlayTrackCommand(String title, String artist) implements MusicCommand {

    public PlayTrackCommand {
        Objects.requireNonNull(title, "Track title must not be null");

        if (title.isBlank()) {
            throw new IllegalArgumentException("Track title must not be null");
        }

        title = title.trim();

        if (artist != null) {
            artist = artist.trim();

            if (artist.isBlank()) {
                artist = null;
            }
        }
    }
}
