package dev.tolkach.protocol.music;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MusicCommandRequest(
        @JsonProperty("requestId")
        String requestId,

        @JsonProperty("action")
        MusicAction action,

        @JsonProperty("title")
        String title,

        @JsonProperty("artist")
        String artist,

        @JsonProperty("query")
        String query
) {
    public MusicCommandRequest {
        validateRequestId(requestId);
        validateAction(action);

        requestId = requestId.trim();

        if (title != null) {
            title = title.trim();
        }

        if (artist != null) {
            artist = artist.trim();
        }

        if (query != null) {
            query = query.trim();
        }

        validatePayload(action, title, query);
    }

    private static void validateRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw  new IllegalArgumentException("Request id must not be empty");
        }
    }

    private static void validateAction(MusicAction action) {
        if (action == null) {
            throw new IllegalArgumentException("Action must not be null");
        }
    }

    private void validatePayload(MusicAction action, String title, String query) {
        switch (action) {
            case PLAY_TRACK, PLAY_ALBUM -> requireTitle(title);

            case PLAY_QUERY -> requireQuery(query);

            case PAUSE, RESUME, NEXT, PREVIOUS -> { }
        }
    }

    private void requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
    }

    private void requireQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }
    }
}
