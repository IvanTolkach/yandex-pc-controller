package dev.tolkach.alice;

import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.protocol.music.MusicCommandResponse;
import org.springframework.stereotype.Component;

@Component
public class AliceMusicResponseMapper {

    public AliceResponse map(MusicCommandRequest command, MusicCommandResponse response) {
        if (!response.success()) {
            return mapError(response);
        }

        return switch (command.action()) {
            case PLAY_TRACK, PLAY_QUERY, PLAY_ALBUM -> AliceResponse.text("Включаю");

            case PAUSE -> AliceResponse.text("Поставила на паузу");

            case RESUME -> AliceResponse.text("Продолжаю");

            case NEXT -> AliceResponse.text("Включаю следующий");

            case PREVIOUS -> AliceResponse.text("Включаю предыдущий");

            case VOLUME_UP -> AliceResponse.text("Сделала громче");

            case VOLUME_DOWN -> AliceResponse.text("Сделала тише");
        };
    }

    private AliceResponse mapError(MusicCommandResponse response) {
        return switch (response.errorCode()) {
            case "AGENT_UNAVAILABLE" -> AliceResponse.text("Компьютер сейчас недоступен");

            case "AGENT_TIMEOUT" -> AliceResponse.text("Компьютер не ответил на команду");

            case "MUSIC_UNAVAILABLE" -> AliceResponse.text("Яндекс Музыка сейчас недоступна");

            case "NO_ACTIVE_PLAYBACK" -> AliceResponse.text("Сейчас ничего не воспроизводится");

            case "INVALID_COMMAND" -> AliceResponse.text("Не удалось понять музыкальную команду");

            default -> AliceResponse.text("Не удалось выполнить команду");
        };
    }
}
