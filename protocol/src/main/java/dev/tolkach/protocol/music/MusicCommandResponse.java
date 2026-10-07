package dev.tolkach.protocol.music;

public record MusicCommandResponse(String requestId, boolean success, PlaybackStateDto playbackState, MusicErrorCode errorCode, String errorMessage) {

    public MusicCommandResponse {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be empty");
        }

        requestId = requestId.trim();

        if (success && errorCode != null) {
            throw new IllegalArgumentException("Successful response must not contain errorCode");
        }

        if (!success && errorCode == null) {
            throw new IllegalArgumentException("Failed response must contain errorCode");
        }
    }

    public static MusicCommandResponse success(String requestId, PlaybackStateDto playbackState) {
        return new MusicCommandResponse(requestId, true, playbackState, null, null);
    }

    public static MusicCommandResponse failure(String requestId, MusicErrorCode errorCode, String errorMessage) {
        return new MusicCommandResponse(requestId, false, null, errorCode, errorMessage);
    }
}
