package dev.tolkach.music.commands;

public record PlayAlbumCommand(String title, String artist) implements MusicCommand {

    public PlayAlbumCommand {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Album title must not be empty");
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
