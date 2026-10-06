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

    private static final Pattern PLAY_ALBUM_PATTERN = Pattern.compile("^(?:включи|поставь|запусти)\\s+альбом\\s+(.+?)(?:\\s+исполнителя\\s+(.+))?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern PAUSE_PATTERN = Pattern.compile("^(поставь\\s+на\\s+паузу|пауза|останови|приостанови|стоп|хватит).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern RESUME_PATTERN = Pattern.compile("^(продолжи|возобнови|воспроизведи).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern NEXT_PATTERN = Pattern.compile("^(вперёд|следующий|следующий\\s+трек|включи\\s+следующий).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PREVIOUS_PATTERN = Pattern.compile("^(назад|предыдущий|предыдущий\\s+трек|включи\\s+предыдущий).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern VOLUME_UP_PATTERN = Pattern.compile("^(?:громче|сделай громче|прибавь громкость).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern VOLUME_DOWN_PATTERN = Pattern.compile("^(?:тише|сделай тише|убавь громкость).*", Pattern.CASE_INSENSITIVE);

    public Optional<MusicCommandRequest> parse(AliceRequest aliceRequest) {
        if (aliceRequest == null || aliceRequest.request() == null) {
            return Optional.empty();
        }

        Optional<MusicCommandRequest> nluCommand = parseNlu(aliceRequest.request().nlu());

        if (nluCommand.isPresent()) {
            return nluCommand;
        }

        return parseFallback(aliceRequest.request().command());
    }

    public Optional<MusicCommandRequest> parseFallback(String command) {
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

        if (VOLUME_UP_PATTERN.matcher(normalized).matches()) {
            return Optional.of(command(MusicAction.VOLUME_UP));
        }

        if (VOLUME_DOWN_PATTERN.matcher(normalized).matches()) {
            return Optional.of(command(MusicAction.VOLUME_DOWN));
        }

        Optional<MusicCommandRequest> album = parsePlayAlbum(normalized);

        if (album.isPresent()) {
            return album;
        }

        Optional<MusicCommandRequest> track = parsePlayTrack(normalized);

        if (track.isPresent()) {
            return track;
        }

        return parsePlayQuery(normalized);
    }

    private Optional<MusicCommandRequest> parseNlu(AliceRequest.Nlu nlu) {
        if (nlu == null || nlu.intents() == null || nlu.intents().isEmpty()) {
            return Optional.empty();
        }

        if (nlu.intents().containsKey("pause")) {
            return Optional.of(command(MusicAction.PAUSE));
        }

        if (nlu.intents().containsKey("resume")) {
            return Optional.of(command(MusicAction.RESUME));
        }

        if (nlu.intents().containsKey("next")) {
            return Optional.of(command(MusicAction.NEXT));
        }

        if (nlu.intents().containsKey("previous")) {
            return Optional.of(command(MusicAction.PREVIOUS));
        }

        if (nlu.intents().containsKey("volumeup")) {
            return Optional.of(command(MusicAction.VOLUME_UP));
        }

        if (nlu.intents().containsKey("volumedown")) {
            return Optional.of(command(MusicAction.VOLUME_DOWN));
        }

        AliceRequest.Intent playAlbum = nlu.intents().get("playalbum");

        if (playAlbum != null) {
            String title = slotValue(playAlbum, "title");

            String artist = slotValue(playAlbum, "artist");

            if (title != null && !title.isBlank()) {
                return Optional.of(
                        new MusicCommandRequest(
                                UUID.randomUUID().toString(),
                                MusicAction.PLAY_ALBUM,
                                title,
                                artist,
                                null
                        )
                );
            }
        }

        AliceRequest.Intent playQuery = nlu.intents().get("playquery");

        if (playQuery != null) {
            String query = slotValue(playQuery, "query");

            if (query != null && !query.isBlank()) {
                return Optional.of(
                        new MusicCommandRequest(
                                UUID.randomUUID().toString(),
                                MusicAction.PLAY_QUERY,
                                null,
                                null,
                                query
                        )
                );
            }
        }

        AliceRequest.Intent playTrack = nlu.intents().get("playtrack");

        if (playTrack != null) {
            String title = slotValue(playTrack, "title");

            String artist = slotValue(playTrack, "artist");

            if (title != null && !title.isBlank()) {
                if (artist == null || artist.isBlank()) {
                    return Optional.of(
                            new MusicCommandRequest(
                                    UUID.randomUUID().toString(),
                                    MusicAction.PLAY_QUERY,
                                    null,
                                    null,
                                    title
                            )
                    );
                }

                return Optional.of(
                        new MusicCommandRequest(
                                UUID.randomUUID().toString(),
                                MusicAction.PLAY_TRACK,
                                title,
                                artist,
                                null
                        )
                );
            }
        }

        return Optional.empty();
    }

    private String slotValue(AliceRequest.Intent intent, String slotName) {
        if (intent.slots() == null) {
            return null;
        }

        AliceRequest.Slot slot = intent.slots().get(slotName);

        if (slot == null) {
            return null;
        }

        if (slot.value() == null || slot.value().isBlank()) {
            return null;
        }

        return slot.value().trim();
    }

    private Optional<MusicCommandRequest> parsePlayAlbum(String command) {
        Matcher matcher = PLAY_ALBUM_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        String title = matcher.group(1).trim();

        String artist = matcher.group(2);

        if (artist != null) {
            artist = artist.trim();

            if (artist.isBlank()) {
                artist = null;
            }
        }

        return Optional.of(new MusicCommandRequest(
                UUID.randomUUID().toString(),
                MusicAction.PLAY_ALBUM,
                title,
                artist,
                null)
        );
    }

    private Optional<MusicCommandRequest> parsePlayTrack(String command) {
        Matcher matcher = PLAY_TRACK_PATTERN.matcher(command);

        if (!matcher.matches()) {
            return  Optional.empty();
        }

        String title = matcher.group(1).trim();

        String artist = matcher.group(2).trim();

        return Optional.of(new MusicCommandRequest(
                UUID.randomUUID().toString(),
                MusicAction.PLAY_TRACK,
                title,
                artist,
                null)
        );
    }

    private Optional<MusicCommandRequest> parsePlayQuery(String command) {
        String query = extractPlayQuery(command);

        if (query == null) {
            return Optional.empty();
        }

        return Optional.of(new MusicCommandRequest(
                UUID.randomUUID().toString(),
                MusicAction.PLAY_QUERY,
                null,
                null,
                query)
        );
    }

    private String extractPlayQuery(String command) {
        String[] prefixes = {"включи ", "поставь ", "запусти "};

        for (String prefix : prefixes) {
            if (command.toLowerCase().startsWith(prefix)) {
                String query = command.substring(prefix.length()).trim();

                if (!query.isBlank()) {
                    return query;
                }
            }
        }

        return null;
    }

    private MusicCommandRequest command(MusicAction action) {
        return new MusicCommandRequest(
                UUID.randomUUID().toString(),
                action,
                null,
                null,
                null
        );
    }
}
