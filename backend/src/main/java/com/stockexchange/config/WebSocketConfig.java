package com.stockexchange.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
// Enables WebSocket message handling backed by a message broker.
// Without this annotation, @MessageMapping and SimpMessagingTemplate
// in StockPriceSimulator will not work.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${frontend.url}")
    private String frontendUrl;
    // http://localhost:5173 — read from application.properties.
    // Only this origin is allowed to open a WebSocket connection.

    // ── Message Broker ───────────────────────────────────────────────────────

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        registry.enableSimpleBroker("/topic");
        // Enables an in-memory message broker for the "/topic" prefix.
        // StockPriceSimulator sends to "/topic/prices".
        // All frontend clients subscribed to "/topic/prices"
        // receive the message instantly.
        // In production this would be replaced with a full
        // message broker like RabbitMQ or ActiveMQ for scalability.

        registry.setApplicationDestinationPrefixes("/app");
        // "/app" prefix is for messages routed to @MessageMapping methods.
        // Example: frontend sends to "/app/order" →
        // Spring routes it to a method annotated @MessageMapping("/order").
        // Not heavily used in this system since orders go through REST,
        // but kept here for future bi-directional WebSocket use.
    }

    // ── STOMP Endpoint ───────────────────────────────────────────────────────

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws")
                // This is the WebSocket handshake URL.
                // Frontend connects to: ws://localhost:8080/ws
                // This matches the SecurityConfig permit:
                // .requestMatchers("/ws/**").permitAll()

                .setAllowedOrigins(frontendUrl)
                // Only allow WebSocket connections from our React frontend.
                // Prevents other origins from subscribing to live prices.

                .withSockJS();
        // SockJS is a fallback layer.
        // If the browser does not support native WebSockets,
        // SockJS automatically falls back to long-polling or
        // other HTTP-based transports — ensuring compatibility
        // across all browsers.
        // Frontend uses the SockJS client library to connect.
    }
}