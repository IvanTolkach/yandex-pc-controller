package dev.tolkach.alice;

import dev.tolkach.protocol.music.MusicAction;
import dev.tolkach.protocol.music.MusicCommandRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AliceCommandParser {

    private static final Pattern PLAY_TRACK_PATTERN = Pattern.compile("^включи\\s+(.+?)\\s+исполнителя\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    public Optional<MusicCommandRequest> parse(String command) {
        if (command == null || command.isBlank()) {
            return Optional.empty();
        }

        String normalized = command.trim();

        Matcher matcher = PLAY_TRACK_PATTERN.matcher(normalized);

        if (!matcher.matches()) {
            return  Optional.empty();
        }

        String title = matcher.group(1).trim();

        String artist = matcher.group(2).trim();

        return Optional.of(new MusicCommandRequest(
                UUID.randomUUID().toString(),
                MusicAction.PLAY_TRACK,
                title,
                artist)
        );
    }
}
