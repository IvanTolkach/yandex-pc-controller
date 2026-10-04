package dev.tolkach.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class CommandController {

    private final AgentCommandService commandService;

    private static final Logger log = LoggerFactory.getLogger(CommandController.class);

    public CommandController(AgentCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping (value = "/{deviceId}/commands", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> execute(@PathVariable String deviceId, @RequestBody String commandJson) {
        try {
            String response = commandService.sendCommand(deviceId, commandJson);

            return ResponseEntity.ok(response);
        }
        catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\":\"" + exception.getMessage() + "\"}");
        }
        catch (Exception exception) {
            log.error("Gateway command failed. deviceId={}", deviceId, exception);

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"Gateway command failed" + exception.getMessage() + "\"}");
        }
    }
}
