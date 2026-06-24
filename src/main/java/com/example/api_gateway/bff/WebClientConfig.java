package com.example.api_gateway.bff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${INVENTORY_IP:localhost}")
    String inventoryIp;
    @Value("${ORDERS_IP:localhost}")
    String ordersIp;
    @Value("${USERS_IP:localhost}")
    String usersIp;

    @Bean("inventory")
    public WebClient inventoryClient(WebClient.Builder builder) {
        return builder.baseUrl("http://" + inventoryIp + ":8081").build();
    }

    @Bean("orders")
    public WebClient ordersClient(WebClient.Builder builder) {
        return builder.baseUrl("http://" + ordersIp + ":8081").build();
    }

    @Bean("users")
    public WebClient usersClient(WebClient.Builder builder) {
        return builder.baseUrl("http://" + usersIp + ":8081").build();
    }

    // Provide a WebClient.Builder bean so other @Bean methods can accept it as a
    // parameter.
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Return a plain builder. Per-service timeouts or connectors can be configured
        // where needed.
        return WebClient.builder();
    }
}
