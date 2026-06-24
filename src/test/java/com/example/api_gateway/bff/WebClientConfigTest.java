package com.example.api_gateway.bff;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebClientConfigTest {

    private WebClientConfig newConfig() {
        WebClientConfig config = new WebClientConfig();
        config.inventoryIp = "inventory-host";
        config.ordersIp = "orders-host";
        config.usersIp = "users-host";
        config.microservicePort = "8081";
        return config;
    }

    @Test
    void buildsAllClientsAndBuilder() {
        WebClientConfig config = newConfig();
        WebClient.Builder builder = config.webClientBuilder();
        assertNotNull(builder);
        assertNotNull(config.inventoryClient(builder));
        assertNotNull(config.ordersClient(builder));
        assertNotNull(config.usersClient(builder));
    }
}
