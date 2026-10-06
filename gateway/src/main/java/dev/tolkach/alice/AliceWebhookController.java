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
        log.info("Alice request: '{}'", request);

        boolean accepted = musicService.handle(request);

        if (!accepted) {
            return AliceResponse.text("Я не поняла команду.");
        }

        return AliceResponse.text("Выполняю.");
    }
}
