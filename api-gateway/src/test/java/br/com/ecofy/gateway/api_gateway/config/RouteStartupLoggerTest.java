package br.com.ecofy.gateway.api_gateway.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.core.publisher.Flux;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do registro de rotas na inicialização")
class RouteStartupLoggerTest {

    @Mock
    private RouteLocator routeLocator;

    private RouteStartupLogger routeStartupLogger;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        routeStartupLogger = new RouteStartupLogger(routeLocator);

        logger = (Logger) LoggerFactory.getLogger(RouteStartupLogger.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    @DisplayName("Deve registrar o identificador e a URI de todas as rotas configuradas")
    void logRoutes_rotasConfiguradas_deveRegistrarTodasAsRotas() {
        // Arrange
        Route authRoute = mock(Route.class);
        Route usersRoute = mock(Route.class);

        URI authUri = URI.create("http://localhost:8081");
        URI usersUri = URI.create("http://localhost:8087");

        when(authRoute.getId()).thenReturn("v1-auth");
        when(authRoute.getUri()).thenReturn(authUri);
        when(usersRoute.getId()).thenReturn("v1-users");
        when(usersRoute.getUri()).thenReturn(usersUri);
        when(routeLocator.getRoutes()).thenReturn(Flux.just(authRoute, usersRoute));

        // Act
        routeStartupLogger.logRoutes();

        // Assert
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly(
                        "[gateway] rota carregada: id=v1-auth uri=http://localhost:8081",
                        "[gateway] rota carregada: id=v1-users uri=http://localhost:8087",
                        "[gateway] inicialização de rotas concluída"
                );

        verify(routeLocator).getRoutes();
        verify(authRoute).getId();
        verify(authRoute).getUri();
        verify(usersRoute).getId();
        verify(usersRoute).getUri();
    }

    @Test
    @DisplayName("Deve registrar a conclusão quando nenhuma rota estiver configurada")
    void logRoutes_semRotasConfiguradas_deveRegistrarConclusao() {
        // Arrange
        when(routeLocator.getRoutes()).thenReturn(Flux.empty());

        // Act
        routeStartupLogger.logRoutes();

        // Assert
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly("[gateway] inicialização de rotas concluída");

        verify(routeLocator).getRoutes();
    }
}