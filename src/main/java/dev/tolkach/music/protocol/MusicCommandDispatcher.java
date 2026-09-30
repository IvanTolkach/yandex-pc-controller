package dev.tolkach.music.protocol;

import dev.tolkach.music.commands.MusicCommandHandler;
import dev.tolkach.music.commands.MusicCommand;
import dev.tolkach.yandex.model.PlaybackState;

public class MusicCommandDispatcher {

    private final MusicCommandMapper commandMapper;

    private final MusicCommandHandler commandHandler;

    public MusicCommandDispatcher(MusicCommandMapper commandMapper, MusicCommandHandler commandHandler) {
        if (commandMapper == null) {
            throw new IllegalArgumentException("Command mapper must not be null");
        }

        if (commandHandler == null) {
            throw new IllegalArgumentException("Command handler must not be null");
        }

        this.commandMapper = commandMapper;
        this.commandHandler = commandHandler;
    }

    public MusicCommandResponse dispatch(MusicCommandRequest request) {
        try {
            MusicCommand command = commandMapper.map(request);

            PlaybackState state = commandHandler.handle(command);

            return MusicCommandResponse.success(request.requersId(), state);
        } catch (IllegalArgumentException exception) {
            return MusicCommandResponse.failure(request.requersId(), "INVALID_COMMAND", exception.getMessage());
        } catch (IllegalStateException exception) {
            return MusicCommandResponse.failure(request.requersId(), "PLAYER_ERROR", exception.getMessage());
        }
    }
}
