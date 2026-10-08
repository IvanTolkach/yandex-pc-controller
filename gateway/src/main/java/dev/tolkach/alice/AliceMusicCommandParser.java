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

    private static final String INTENT_PAUSE = "pause";
    private static final String INTENT_RESUME = "resume";
    private static final String INTENT_NEXT = "next";
    private static final String INTENT_PREVIOUS = "previous";
    private static final String INTENT_VOLUME_UP = "volumeup";
    private static final String INTENT_VOLUME_DOWN = "volumedown";
    private static final String INTENT_PLAY_ALBUM = "playalbum";
    private static final String INTENT_PLAY_QUERY = "playquery";
    private static final String INTENT_PLAY_TRACK = "playtrack";

    private static final Pattern PLAY_ALBUM_PATTERN = Pattern.compile("^(?:включи|включить|поставь|поставить|запусти|запустить)\\s+альбом\\s+(.+?)" + "(?:\\s+исполнителя\\s+(.+))?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern ALBUM_WITH_ARTIST_PATTERN = Pattern.compile("(?iu)^(.+?)\\s+(?:исполнителя|исполнитель|от)\\s+(.+?)\\s*$");

    private static final Pattern PAUSE_PATTERN = Pattern.compile("^(поставь\\s+на\\s+паузу|пауза|останови|приостанови|хватит).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern RESUME_PATTERN = Pattern.compile("^(продолжи|возобнови|воспроизведи).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern NEXT_PATTERN = Pattern.compile("^(вперёд|следующий|следующий\\s+трек|включи\\s+следующий|включить\\s+следующий).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PREVIOUS_PATTERN = Pattern.compile("^(назад|предыдущий|предыдущий\\s+трек|включи\\s+предыдущий|включить\\s+предыдущий).*", Pattern.CASE_INSENSITIVE);

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

        Optional<MusicCommandRequest> controlCommand = parseControlCommand(normalized);

        if (controlCommand.isPresent()) {
            return controlCommand;
        }

        Optional<MusicCommandRequest> albumCommand = parsePlayAlbum(normalized);

        if (albumCommand.isPresent()) {
            return albumCommand;
        }

        return parsePlayQuery(normalized);
    }

    private Optional<MusicCommandRequest> parseNlu(AliceRequest.Nlu nlu) {
        if (nlu == null || nlu.intents() == null || nlu.intents().isEmpty()) {
            return Optional.empty();
        }

        Optional<MusicCommandRequest> controlCommand = parseControlIntent(nlu);

        if (controlCommand.isPresent()) {
            return controlCommand;
        }

        AliceRequest.Intent playAlbum = nlu.intents().get(INTENT_PLAY_ALBUM);

        if (playAlbum != null) {
            Optional<MusicCommandRequest> albumCommand = parsePlayAlbumIntent(playAlbum);

            if (albumCommand.isPresent()) {
                return albumCommand;
            }
        }

        AliceRequest.Intent playQuery = nlu.intents().get(INTENT_PLAY_QUERY);

        if (playQuery != null) {
            String query = slotValue(playQuery, "query");

            if (query != null) {
                return Optional.of(playQueryCommand(query));
            }
        }

        AliceRequest.Intent playTrack = nlu.intents().get(INTENT_PLAY_TRACK);

        if (playTrack != null) {
            String title = slotValue(playTrack, "title");
            String artist = slotValue(playTrack, "artist");

            String query = combineQuery(title, artist);

            if (query != null) {
                return Optional.of(playQueryCommand(query));
            }
        }

        return Optional.empty();
    }

    private Optional<MusicCommandRequest> parseControlIntent(AliceRequest.Nlu nlu) {
        if (nlu.intents().containsKey(INTENT_PAUSE)) {
            return Optional.of(command(MusicAction.PAUSE));
        }

        if (nlu.intents().containsKey(INTENT_RESUME)) {
            return Optional.of(command(MusicAction.RESUME));
        }

        if (nlu.intents().containsKey(INTENT_NEXT)) {
            return Optional.of(command(MusicAction.NEXT));
        }

        if (nlu.intents().containsKey(INTENT_PREVIOUS)) {
            return Optional.of(command(MusicAction.PREVIOUS));
        }

        if (nlu.intents().containsKey(INTENT_VOLUME_UP)) {
            return Optional.of(command(MusicAction.VOLUME_UP));
        }

        if (nlu.intents().containsKey(INTENT_VOLUME_DOWN)) {
            return Optional.of(command(MusicAction.VOLUME_DOWN));
        }

        return Optional.empty();
    }

    private Optional<MusicCommandRequest> parsePlayAlbumIntent(AliceRequest.Intent intent) {
        String title = slotValue(intent, "title");
        String artist = slotValue(intent, "artist");

        if (title == null) {
            return Optional.empty();
        }

        AlbumParts parts = parseAlbumParts(title, artist);

        return Optional.of(
                new MusicCommandRequest(
                        UUID.randomUUID().toString(),
                        MusicAction.PLAY_ALBUM,
                        parts.title(),
                        parts.artist(),
                        null
                )
        );
    }

    private Optional<MusicCommandRequest> parseControlCommand(String command) {
        if (PAUSE_PATTERN.matcher(command).matches()) {
            return Optional.of(command(MusicAction.PAUSE));
        }

        if (RESUME_PATTERN.matcher(command).matches()) {
            return Optional.of(command(MusicAction.RESUME));
        }

        if (NEXT_PATTERN.matcher(command).matches()) {
            return Optional.of(command(MusicAction.NEXT));
        }

        if (PREVIOUS_PATTERN.matcher(command).matches()) {
            return Optional.of(command(MusicAction.PREVIOUS));
        }

        if (VOLUME_UP_PATTERN.matcher(command).matches()) {
            return Optional.of(command(MusicAction.VOLUME_UP));
        }

        if (VOLUME_DOWN_PATTERN.matcher(command).matches()) {
            return Optional.of(command(MusicAction.VOLUME_DOWN));
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

        AlbumParts parts = parseAlbumParts(title, artist);

        return Optional.of(new MusicCommandRequest(
                UUID.randomUUID().toString(),
                MusicAction.PLAY_ALBUM,
                parts.title(),
                parts.artist(),
                null)
        );
    }

    private Optional<MusicCommandRequest> parsePlayQuery(String command) {
        String query = extractPlayQuery(command);

        if (query == null) {
            return Optional.empty();
        }

        return Optional.of(playQueryCommand(query));
    }

    private MusicCommandRequest playQueryCommand(String query) {
        return new MusicCommandRequest(
                UUID.randomUUID().toString(),
                MusicAction.PLAY_QUERY,
                null,
                null,
                query);
    }

    private String extractPlayQuery(String command) {
        String[] prefixes = {"включить ", "включи ", "поставить ", "поставь ", "запустить ", "запусти "};

        for (String prefix : prefixes) {
            if (command.toLowerCase().startsWith(prefix)) {
                String query = command.substring(prefix.length()).trim();

                if (!query.isBlank()) {
                    return query;
                }

                return null;
            }
        }

        return null;
    }

    private String combineQuery(String title, String artist) {
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasArtist = artist != null && !artist.isBlank();

        if (!hasTitle && !hasArtist) {
            return null;
        }

        if (hasTitle && hasArtist) {
            return title + " " + artist;
        }

        return hasTitle ? title : artist;
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

    private AlbumParts parseAlbumParts(String title, String artist) {
        String normalizedTitle = title == null ? null : title.trim();

        String normalizedArtist = artist == null || artist.isBlank() ? null : artist.trim();

        if (normalizedArtist != null) {
            return new AlbumParts(normalizedTitle, normalizedArtist);
        }

        if (normalizedTitle == null || normalizedTitle.isBlank()) {
            return new AlbumParts(normalizedTitle, null);
        }

        Matcher matcher = ALBUM_WITH_ARTIST_PATTERN.matcher(normalizedTitle);

        if (!matcher.matches()) {
            return new AlbumParts(normalizedTitle, null);
        }

        return new AlbumParts(matcher.group(1).trim(), matcher.group(2).trim());
    }

    private record AlbumParts(String title, String artist) {
    }
}
