package com.example.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayCorsConfigTest {

    private final GatewayCorsConfig config = new GatewayCorsConfig();

    @Test
    void buildsFilterForExplicitOrigins() {
        // entradas en blanco deben filtrarse; sin comodín → setAllowedOrigins
        CorsWebFilter filter = config.corsWebFilter("http://localhost:5173, http://example.com , ");
        assertNotNull(filter);
    }

    @Test
    void buildsFilterForWildcardOrigins() {
        // con comodín → setAllowedOriginPatterns
        CorsWebFilter filter = config.corsWebFilter("http://*.example.com");
        assertNotNull(filter);
    }
}
