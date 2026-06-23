package com.example.api_gateway.bff;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bff/v1")
public class BffController {

    private final WebClient inventoryClient;
    private final WebClient ordersClient;
    private final WebClient usersClient;

    public BffController(
            @Qualifier("inventory") WebClient inventoryClient,
            @Qualifier("orders") WebClient ordersClient,
            @Qualifier("users") WebClient usersClient) {
        this.inventoryClient = inventoryClient;
        this.ordersClient = ordersClient;
        this.usersClient = usersClient;
    }

    record DashboardStats(int productos, int sucursales, int bodegas, int ordenes, int usuarios, int roles) {
    }

    @GetMapping("/dashboard")
    public Mono<DashboardStats> dashboard() {
        Mono<Integer> productos = count(inventoryClient, "/api/v1/products");
        Mono<Integer> sucursales = count(inventoryClient, "/api/v1/branches");
        Mono<Integer> bodegas = count(inventoryClient, "/api/v1/warehouses");
        Mono<Integer> ordenes = count(ordersClient, "/api/v1/orders");
        Mono<Integer> usuarios = count(usersClient, "/api/v1/users");
        Mono<Integer> roles = count(usersClient, "/api/v1/roles");

        return Mono.zip(productos, sucursales, bodegas, ordenes, usuarios, roles)
                .map(t -> new DashboardStats(t.getT1(), t.getT2(), t.getT3(),
                        t.getT4(), t.getT5(), t.getT6()));
    }

    private Mono<Integer> count(WebClient client, String uri) {
        return client.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(Object.class)
                .count()
                .map(Long::intValue)
                .onErrorReturn(0);
    }

}
