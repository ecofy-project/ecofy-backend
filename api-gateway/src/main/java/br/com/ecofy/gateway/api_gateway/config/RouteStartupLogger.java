package br.com.ecofy.gateway.api_gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Registra as rotas carregadas após a inicialização do Gateway.
@Component
public class RouteStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(RouteStartupLogger.class);

    private final RouteLocator routeLocator;

    public RouteStartupLogger(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    // Registra o identificador e a URI de cada rota configurada.
    @EventListener(ApplicationReadyEvent.class)
    public void logRoutes() {
        routeLocator.getRoutes()
                .doOnNext(route -> log.info(
                        "[gateway] rota carregada: id={} uri={}",
                        route.getId(),
                        route.getUri()
                ))
                .doOnComplete(() -> log.info(
                        "[gateway] inicialização de rotas concluída"
                ))
                .subscribe();
    }
}
