package br.com.ecofy.gateway.api_gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observabilidade mínima: registra no log, na inicialização, as rotas estáticas
 * carregadas pelo Spring Cloud Gateway (id -> uri). Ajuda no diagnóstico de
 * "qual rota o gateway conhece" sem precisar chamar o endpoint do Actuator.
 *
 * Não loga headers, tokens, Authorization nem query strings.
 */
@Component
public class RouteStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(RouteStartupLogger.class);

    private final RouteLocator routeLocator;

    public RouteStartupLogger(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logRoutes() {
        routeLocator.getRoutes()
                .doOnNext(route -> log.info("[gateway] rota carregada: id={} uri={}",
                        route.getId(), route.getUri()))
                .doOnComplete(() -> log.info("[gateway] inicialização de rotas concluída"))
                .subscribe();
    }
}
