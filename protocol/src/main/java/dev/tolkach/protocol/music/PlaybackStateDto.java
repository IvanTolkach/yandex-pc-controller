package dev.tolkach.protocol.music;

import java.util.List;

public record PlaybackStateDto(
        String trackId,
        String title,
        List<String> artists,
        boolean playing
) {
}