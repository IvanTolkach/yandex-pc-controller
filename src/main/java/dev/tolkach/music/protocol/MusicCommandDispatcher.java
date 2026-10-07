package dev.tolkach.music.protocol;

import dev.tolkach.music.NoActivePlaybackException;
import dev.tolkach.music.commands.MusicCommandHandler;
import dev.tolkach.music.commands.MusicCommand;
import dev.tolkach.music.connection.MusicUnavailableException;
import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.protocol.music.MusicCommandResponse;
import dev.tolkach.protocol.music.PlaybackStateDto;
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

            PlaybackStateDto stateDto = toDto(state);

            return MusicCommandResponse.success(request.requestId(), stateDto);
        } catch (NoActivePlaybackException exception) {
            return MusicCommandResponse.failure(request.requestId(), "NO_ACTIVE_PLAYBACK", exception.getMessage());
        } catch (MusicUnavailableException exception) {
            return MusicCommandResponse.failure(request.requestId(), "MUSIC_UNAVAILABLE", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return MusicCommandResponse.failure(request.requestId(), "INVALID_COMMAND", exception.getMessage());
        } catch (IllegalStateException exception) {
            return MusicCommandResponse.failure(request.requestId(), "PLAYER_ERROR", exception.getMessage());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return MusicCommandResponse.failure(request.requestId(), "INTERNAL_ERROR", exception.getMessage());
        }
    }

    private PlaybackStateDto toDto(PlaybackState state) {
        return new PlaybackStateDto(state.trackId(), state.title(), state.artists(), state.playing());
    }
}
