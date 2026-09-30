package dev.tolkach.music.commands;

public record PlayTrackCommand(String title, String artist) implements MusicCommand {

    public PlayTrackCommand {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Track title must not be empty"
            );
        }

        if (artist == null || artist.isBlank()) {
            throw new IllegalArgumentException(
                    "Artist must not be empty"
            );
        }

        title = title.trim();
        artist = artist.trim();
    }
}
