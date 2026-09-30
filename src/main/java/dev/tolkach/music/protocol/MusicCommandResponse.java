package dev.tolkach.music.protocol;

import dev.tolkach.yandex.model.PlaybackState;

public record MusicCommandResponse(String requestId, boolean success, PlaybackState playbackState, String errorCode, String errorMessage) {
    public static MusicCommandResponse success(String requestId, PlaybackState playbackState) {
        return new MusicCommandResponse(requestId, true, playbackState, null, null);
    }

    public static MusicCommandResponse failure(String requestId, String errorCode, String errorMessage) {
        return new MusicCommandResponse(requestId, false, null, errorCode, errorMessage);
    }
}
