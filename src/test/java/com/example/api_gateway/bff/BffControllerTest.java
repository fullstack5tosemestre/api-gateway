package com.example.api_gateway.bff;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BffControllerTest {

    /** Crea un WebClient cuya respuesta JSON depende del path solicitado. */
    private WebClient webClientReturning(String path, String json) {
        ExchangeFunction exchangeFunction = request -> {
            if (request.url().getPath().equals(path)) {
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }
            return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        };
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    private WebClient webClientFailing() {
        ExchangeFunction exchangeFunction = request ->
                Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    /** WebClient que responde según una de varias rutas posibles (para el cliente de inventario, que se reutiliza en 3 endpoints). */
    private WebClient webClientMultiRoute(java.util.Map<String, String> jsonByPath) {
        ExchangeFunction exchangeFunction = request -> {
            String path = request.url().getPath();
            String json = jsonByPath.get(path);
            if (json != null) {
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }
            return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        };
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    @Test
    void dashboardAggregatesCountsFromAllServices() {
        WebClient inventoryClient = webClientMultiRoute(java.util.Map.of(
                "/api/v1/products", "[{},{},{}]",
                "/api/v1/branches", "[{},{}]",
                "/api/v1/warehouses", "[{}]"
        ));
        WebClient ordersClient = webClientReturning("/api/v1/orders", "[{},{},{},{}]");
        WebClient usersClient = webClientMultiRoute(java.util.Map.of(
                "/api/v1/users", "[{},{},{},{},{}]",
                "/api/v1/roles", "[{},{},{}]"
        ));

        BffController controller = new BffController(inventoryClient, ordersClient, usersClient);

        StepVerifier.create(controller.dashboard())
                .assertNext(stats -> {
                    assertEquals(3, stats.productos());
                    assertEquals(2, stats.sucursales());
                    assertEquals(1, stats.bodegas());
                    assertEquals(4, stats.ordenes());
                    assertEquals(5, stats.usuarios());
                    assertEquals(3, stats.roles());
                })
                .verifyComplete();
    }

    @Test
    void dashboardReturnsZeroForServicesThatFail() {
        WebClient inventoryClient = webClientFailing();
        WebClient ordersClient = webClientFailing();
        WebClient usersClient = webClientFailing();

        BffController controller = new BffController(inventoryClient, ordersClient, usersClient);

        StepVerifier.create(controller.dashboard())
                .assertNext(stats -> {
                    assertEquals(0, stats.productos());
                    assertEquals(0, stats.sucursales());
                    assertEquals(0, stats.bodegas());
                    assertEquals(0, stats.ordenes());
                    assertEquals(0, stats.usuarios());
                    assertEquals(0, stats.roles());
                })
                .verifyComplete();
    }

    @Test
    void dashboardReturnsZeroOnlyForTheServiceThatFails() {
        WebClient inventoryClient = webClientMultiRoute(java.util.Map.of(
                "/api/v1/products", "[{},{}]",
                "/api/v1/branches", "[{}]",
                "/api/v1/warehouses", "[{}]"
        ));
        WebClient ordersClient = webClientFailing();
        WebClient usersClient = webClientMultiRoute(java.util.Map.of(
                "/api/v1/users", "[{}]",
                "/api/v1/roles", "[{}]"
        ));

        BffController controller = new BffController(inventoryClient, ordersClient, usersClient);

        StepVerifier.create(controller.dashboard())
                .assertNext(stats -> {
                    assertEquals(2, stats.productos());
                    assertEquals(0, stats.ordenes());
                    assertEquals(1, stats.usuarios());
                })
                .verifyComplete();
    }
}
