package dev.tolkach.alice;

import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.protocol.music.MusicCommandResponse;
import dev.tolkach.websocket.AgentCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AliceMusicService {

    private static final Logger log = LoggerFactory.getLogger(AliceMusicService.class);

    private static final Duration ALICE_WAIT_TIMEOUT = Duration.ofSeconds(3);

    private static final String DEVICE_ID = "my-pc";

    private final AliceMusicCommandParser parser;
    private final AgentCommandService commandService;
    private final AliceMusicResponseMapper responseMapper;

    public AliceMusicService(AliceMusicCommandParser parser, AgentCommandService commandService, AliceMusicResponseMapper responseMapper) {
        this.parser = parser;
        this.commandService = commandService;
        this.responseMapper = responseMapper;
    }

    public AliceResponse handle(AliceRequest request) {
        if (isActivationRequest(request)) {
            return AliceResponse.text("Готово. Слушаю команды.");
        }

        Optional<MusicCommandRequest> parsed = parser.parse(request);

        if (parsed.isEmpty()) {
            return AliceResponse.text("Не удалось понять музыкальную команду");
        }

        MusicCommandRequest command = parsed.get();

        CompletableFuture<MusicCommandResponse> future = commandService.sendCommandAsync(DEVICE_ID, command);

        try {
            MusicCommandResponse response = future.get(ALICE_WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            logResponse(response);

            return responseMapper.map(command, response);
        }
        catch (TimeoutException exception) {
            log.info("Alice response timeout reached, command continues asynchronously: requestId={}", command.requestId());

            return AliceResponse.text("Команда отправлена");
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            log.error("Alice command interrupted: requestId={}", command.requestId(), exception);

            return AliceResponse.text("Не удалось выполнить команду");
        }
        catch (Exception exception) {
            log.error("Unexpected Alice command error: requestId={}", command.requestId(), exception);

            return AliceResponse.text("Не удалось выполнить команду");
        }
    }

    private boolean isActivationRequest(AliceRequest request) {
        if (request == null || request.request() == null) {
            return false;
        }

        String command = request.request().command();

        String originalUtterance = request.request().originalUtterance();

        return (command == null || command.isBlank()) && (originalUtterance == null || originalUtterance.isBlank());
    }

    private void logResponse(MusicCommandResponse response) {
        if (response.success()) {
            log.info("Alice music command completed: {}", response);

            return;
        }

        log.warn("Alice music command rejected: requestId={}, errorCode={}, errorMessage={}", response.requestId(), response.errorCode(), response.errorMessage());
    }
}
