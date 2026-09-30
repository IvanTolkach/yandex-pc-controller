package dev.tolkach.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final String AGENT_ENDPOINT = "/ws/agent/**";

    @Bean
    public AgentSessionRegistry agentSessionRegistry() {
        return new AgentSessionRegistry();
    }

    @Bean
    public AgentWebSocketHandler agentWebSocketHandler(AgentSessionRegistry registry) {
        return new AgentWebSocketHandler(registry);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentWebSocketHandler(agentSessionRegistry()), AGENT_ENDPOINT);
    }
}
