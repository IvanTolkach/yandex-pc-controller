package dev.tolkach.music.protocol;

public record MusicCommandRequest(String requersId, MusicAction action, String title, String artist) {
    public MusicCommandRequest {
        if (requersId == null || requersId.isBlank()) {
            throw  new IllegalArgumentException("Request id must not be empty");
        }

        if (action == null) {
            throw  new IllegalArgumentException("Action id must not null");
        }

        requersId = requersId.trim();

        if (title != null) {
            title = title.trim();
        }

        if (artist != null) {
            artist = artist.trim();
        }
    }
}
