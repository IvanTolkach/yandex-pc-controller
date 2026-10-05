package dev.tolkach.alice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alice")
public class AliceWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AliceWebhookController.class);

    private final AliceMusicService musicService;

    public AliceWebhookController(AliceMusicService musicService) {
        this.musicService = musicService;
    }

    @PostMapping("/webhook")
    public AliceResponse webhook(@RequestBody AliceRequest request) {
        String command = request.request() != null ? request.request().command() : null;

        log.info("Alice command: '{}'", command);

        if (command == null || command.isBlank()) {
            return AliceResponse.text("Я  не поняла команду.");
        }

        boolean accepted = musicService.handle(command);

        if (!accepted) {
            return AliceResponse.text("Пока я умею включать треки в формате: включи название исполнителя исполнитель.");
        }

        return AliceResponse.text("Включаю.");
    }
}
