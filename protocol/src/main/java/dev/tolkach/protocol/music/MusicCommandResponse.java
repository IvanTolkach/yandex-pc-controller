package dev.tolkach.protocol.music;

public record MusicCommandResponse(String requestId, boolean success, PlaybackStateDto playbackState, String errorCode, String errorMessage) {
    public static MusicCommandResponse success(String requestId, PlaybackStateDto playbackState) {
        return new MusicCommandResponse(requestId, true, playbackState, null, null);
    }

    public static MusicCommandResponse failure(String requestId, String errorCode, String errorMessage) {
        return new MusicCommandResponse(requestId, false, null, errorCode, errorMessage);
    }
}
