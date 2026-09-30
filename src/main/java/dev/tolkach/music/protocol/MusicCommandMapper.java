package dev.tolkach.music.protocol;

import dev.tolkach.music.commands.*;

public class MusicCommandMapper {
    public MusicCommand map(MusicCommandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        return switch (request.action()) {
            case PLAY_TRACK -> mapPlayTrack(request);

            case PAUSE -> new PauseCommand();

            case RESUME -> new ResumeCommand();

            case NEXT -> new NextCommand();

            case PREVIOUS -> new PreviousCommand();
        };
    }

    private MusicCommand mapPlayTrack(MusicCommandRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Title is requested for PLAY_TRACK");
        }

        if (request.artist() == null || request.artist().isBlank()) {
            throw new IllegalArgumentException("Artist is requested for PLAY_TRACK");
        }

        return new PlayTrackCommand(request.title(), request.artist());
    }
}
