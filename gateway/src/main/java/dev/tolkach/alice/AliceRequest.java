package dev.tolkach.alice;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AliceRequest(Request request, Session session, String command) {

    public record Request(
            String type,
            String command,

            @JsonProperty("original_utterance")
            String originalUtterance) {}

    public record Session(
            @JsonProperty("session_id")
            String sessionId,

            @JsonProperty("message_id")
            int messageId,

            @JsonProperty("new")
            boolean newSession) {}
}
