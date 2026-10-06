package dev.tolkach.music.commands;

import dev.tolkach.music.MusicController;
import dev.tolkach.yandex.model.PlaybackState;

import java.util.Objects;

public class MusicCommandHandler {

    private final MusicController musicController;

    public MusicCommandHandler(MusicController musicController) {
        this.musicController = Objects.requireNonNull(musicController, "MusicController must not be null");
    }

    public PlaybackState handle(MusicCommand command) {
        Objects.requireNonNull(command, "Command must not be null");

        return switch (command) {
            case PlayTrackCommand play -> musicController.playTrack(play.title(), play.artist());

            case PlayQueryCommand query -> musicController.playQuery(query.query());

            case PlayAlbumCommand album -> musicController.playAlbum(album.title(), album.artist());

            case PauseCommand ignored -> {
                musicController.pause();
                yield musicController.getPlaybackState();
            }

            case ResumeCommand ignored -> {
                musicController.resume();
                yield musicController.getPlaybackState();
            }

            case NextCommand ignored -> {
                musicController.next();
                yield musicController.getPlaybackState();
            }

            case PreviousCommand ignored -> {
                musicController.previous();
                yield musicController.getPlaybackState();
            }

            case VolumeUpCommand ignored -> {
                musicController.volumeUp();
                yield musicController.getPlaybackState();
            }

            case VolumeDownCommand ignored -> {
                musicController.volumeDown();
                yield musicController.getPlaybackState();
            }
        };
    }
}
