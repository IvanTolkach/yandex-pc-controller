package dev.tolkach.alice;

import dev.tolkach.protocol.music.MusicAction;
import dev.tolkach.protocol.music.MusicCommandRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AliceMusicCommandParser {

    private static final Pattern PLAY_TRACK_PATTERN = Pattern.compile("^включи\\s+(.+?)\\s+исполнителя\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern PAUSE_PATTERN = Pattern.compile("^(поставь\\s+на\\s+паузу|пауза|останови|приостанови|стоп|хватит).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern RESUME_PATTERN = Pattern.compile("^(продолжи|возобнови|воспроизведи).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern NEXT_PATTERN = Pattern.compile("^(вперёд|следующий|следующий\\s+трек|включи\\s+следующий).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PREVIOUS_PATTERN = Pattern.compile("^(назад|предыдущий|предыдущий\\s+трек|включи\\s+предыдущий).*", Pattern.CASE_INSENSITIVE);

    public Optional<MusicCommandRequest> parse(String command) {
        if (command == null || command.isBlank()) {
            return Optional.empty();
        }

        String normalized = command.trim();

        if (PAUSE_PATTERN.matcher(normalized).matches()) {
            return Optional.of(command(MusicAction.PAUSE));
        }

        if (RESUME_PATTERN.matcher(normalized).matches()) {
            return Optional.of(command(MusicAction.RESUME));
        }

        if (NEXT_PATTERN.matcher(normalized).matches()) {
            return Optional.of(command(MusicAction.NEXT));
        }

        if (PREVIOUS_PATTERN.matcher(normalized).matches()) {
            return Optional.of(command(MusicAction.PREVIOUS));
        }

        return parsePlayTrack(normalized);
    }

    private Optional<MusicCommandRequest> parsePlayTrack(String command) {
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

    private MusicCommandRequest command(MusicAction action) {
        return new MusicCommandRequest(UUID.randomUUID().toString(), action, null, null);
    }
}
