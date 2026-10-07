package dev.tolkach.music.protocol;

import dev.tolkach.music.commands.*;
import dev.tolkach.protocol.music.MusicCommandRequest;

public class MusicCommandMapper {

    public MusicCommand map(MusicCommandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        return switch (request.action()) {
            case PLAY_TRACK -> new PlayTrackCommand(request.title(), request.artist());

            case PLAY_QUERY -> new PlayQueryCommand(request.query());

            case PLAY_ALBUM -> new PlayAlbumCommand(request.title(), request.artist());

            case PAUSE -> new PauseCommand();

            case RESUME -> new ResumeCommand();

            case NEXT -> new NextCommand();

            case PREVIOUS -> new PreviousCommand();

            case VOLUME_UP -> new VolumeUpCommand();

            case VOLUME_DOWN -> new VolumeDownCommand();
        };
    }
}
