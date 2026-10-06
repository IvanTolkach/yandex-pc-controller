package dev.tolkach.alice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record AliceRequest(Request request, Session session, String version) {

    public record Request(
            String type,
            String command,

            @JsonProperty("original_utterance")
            String originalUtterance,
            Nlu nlu
    ) {}

    public record Nlu(
            List<String> tokens,
            Map<String, Intent> intents
    ) {}

    public record Intent(
            Map<String, Slot> slots
    ) {}

    public record Slot(
            String type,
            String value
    ) {}

    public record Session(
            @JsonProperty("session_id")
            String sessionId,

            @JsonProperty("message_id")
            int messageId,

            @JsonProperty("new")
            boolean newSession
    ) {}
}
