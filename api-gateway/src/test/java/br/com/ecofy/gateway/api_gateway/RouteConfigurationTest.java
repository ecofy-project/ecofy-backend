package br.com.ecofy.gateway.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que as rotas estáticas declaradas no application.yml são carregadas
 * pelo Spring Cloud Gateway (estratégia oficial de roteamento do gateway).
 */
@SpringBootTest
class RouteConfigurationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void allExpectedStaticRoutesAreLoaded() {
        List<Route> routes = Flux.from(routeLocator.getRoutes()).collectList().block();

        assertThat(routes).isNotNull();

        Map<String, String> byId = routes.stream()
                .collect(Collectors.toMap(Route::getId, r -> r.getUri().toString(), (a, b) -> a));

        assertThat(byId.keySet()).contains(
                "ms-auth",
                "ms-ingestion",
                "ms-categorization",
                "ms-budgeting",
                "ms-insights",
                "ms-notification",
                "ms-users");
    }

    @Test
    void msAuthRouteExists() {
        List<Route> routes = Flux.from(routeLocator.getRoutes()).collectList().block();

        assertThat(routes)
                .as("a rota do ms-auth deve existir")
                .anyMatch(r -> r.getId().equals("ms-auth"));
    }
}
