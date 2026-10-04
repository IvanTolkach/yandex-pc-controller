package dev.tolkach.websocket;

import dev.tolkach.protocol.music.MusicCommandRequest;
import dev.tolkach.protocol.music.MusicCommandResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class CommandController {

    private final AgentCommandService commandService;

    public CommandController(AgentCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping (value = "/{deviceId}/commands", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MusicCommandResponse> execute(@PathVariable String deviceId, @RequestBody MusicCommandRequest request) {
        try {
            MusicCommandResponse response = commandService.sendCommand(deviceId, request);

            return ResponseEntity.ok(response);
        }
        catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
