package dev.tolkach.music.connection;

public class MusicUnavailableException extends RuntimeException {

    public MusicUnavailableException(String message) {
        super(message);
    }

    public MusicUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
