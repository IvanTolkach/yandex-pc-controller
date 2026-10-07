package dev.tolkach.music;

public class NoActivePlaybackException extends RuntimeException {
    public NoActivePlaybackException() {
        super("No active playback");
    }
}
