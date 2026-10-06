package dev.tolkach.alice;

import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.websocket.AgentCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AliceMusicService {

    private static final Logger log = LoggerFactory.getLogger(AliceMusicService.class);

    private final AliceMusicCommandParser parser;
    private final AgentCommandService commandService;

    public AliceMusicService(AliceMusicCommandParser parser, AgentCommandService commandService) {
        this.parser = parser;
        this.commandService = commandService;
    }

    public boolean handle(AliceRequest request) {
        Optional<MusicCommandRequest> parsed = parser.parse(request);

        if (parsed.isEmpty()) {
            return false;
        }

        MusicCommandRequest musicRequest = parsed.get();

        commandService.sendCommandAsync("my-pc", musicRequest)
                .whenComplete((response, error) -> {
                    if (error != null) {
                        log.error("Alice music command failed: requestId={}", musicRequest.requestId(), error);
                        return;
                    }

                    log.info("Alice music command completed: {}", response);
                });

        return true;
    }
}
