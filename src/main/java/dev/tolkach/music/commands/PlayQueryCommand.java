package dev.tolkach.music.commands;

public record PlayQueryCommand(String query) implements MusicCommand {

    public  PlayQueryCommand {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }

        query = query.trim();
    }
}
