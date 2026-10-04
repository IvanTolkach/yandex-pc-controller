package dev.tolkach.music.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MusicCommandRequest(
        @JsonProperty("requestId")
        String requestId,

        @JsonProperty("action")
        MusicAction action,

        @JsonProperty("title")
        String title,

        @JsonProperty("artist")
        String artist
) {
    public MusicCommandRequest {
        if (requestId == null || requestId.isBlank()) {
            throw  new IllegalArgumentException("Request id must not be empty");
        }

        if (action == null) {
            throw  new IllegalArgumentException("Action id must not null");
        }

        requestId = requestId.trim();

        if (title != null) {
            title = title.trim();
        }

        if (artist != null) {
            artist = artist.trim();
        }
    }
}
