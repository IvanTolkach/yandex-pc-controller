package dev.tolkach.music.protocol;

import dev.tolkach.music.commands.*;
import dev.tolkach.protocol.music.MusicCommandRequest;

public class MusicCommandMapper {

    public MusicCommand map(MusicCommandRequest request) {
        return switch (request.action()) {
            case PLAY_TRACK -> new PlayTrackCommand(request.title(), request.artist());

            case PLAY_QUERY -> new PlayQueryCommand(request.query());

            case PLAY_ALBUM -> new PlayAlbumCommand(request.title(), request.query());

            case PAUSE -> new PauseCommand();

            case RESUME -> new ResumeCommand();

            case NEXT -> new NextCommand();

            case PREVIOUS -> new PreviousCommand();
        };
    }
}
