package dev.tolkach.alice;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AliceResponse(Response response, String version) {

    public record Response(
            String text,

            @JsonProperty("end_session")
            boolean endSession) {}

    public static AliceResponse text(String text) {
        return new AliceResponse(new Response(text, true), "1.0");
    }
}
