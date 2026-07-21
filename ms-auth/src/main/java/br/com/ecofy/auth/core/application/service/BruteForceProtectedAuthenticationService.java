package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.bruteforce.BlockStatus;
import br.com.ecofy.auth.core.port.in.AuthenticateUserUseCase;
import br.com.ecofy.auth.core.port.out.BruteForceProtectionPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

// Protege o fluxo de autenticação contra tentativas repetidas por usuário e IP.
@Service
@Primary
@Slf4j
public class BruteForceProtectedAuthenticationService
        implements AuthenticateUserUseCase {

    private final AuthService delegate;
    private final BruteForceProtectionPort bruteForceProtectionPort;
    private final MeterRegistry meterRegistry;

    public BruteForceProtectedAuthenticationService(
            AuthService delegate,
            BruteForceProtectionPort bruteForceProtectionPort,
            MeterRegistry meterRegistry
    ) {
        this.delegate = Objects.requireNonNull(
                delegate,
                "delegate must not be null"
        );
        this.bruteForceProtectionPort = Objects.requireNonNull(
                bruteForceProtectionPort,
                "bruteForceProtectionPort must not be null"
        );
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null"
        );
    }

    // Valida o bloqueio, delega a autenticação e registra o resultado da tentativa.
    @Override
    public AuthenticationResult authenticate(
            AuthenticationCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        String key = protectionKey(
                command.username(),
                command.ipAddress()
        );

        BlockStatus status =
                bruteForceProtectionPort.status(key);

        if (status.blocked()) {
            meterRegistry.counter(
                    "ecofy.auth.login",
                    "outcome",
                    "blocked"
            ).increment();

            log.warn(
                    "[BruteForceProtectedAuthenticationService] -> Tentativa durante bloqueio temporário clientId={}",
                    command.clientId()
            );

            throw new AuthException(
                    AuthErrorCode.AUTHENTICATION_TEMPORARILY_BLOCKED,
                    "Too many failed attempts. Please try again later."
            );
        }

        try {
            AuthenticationResult result =
                    delegate.authenticate(command);

            bruteForceProtectionPort.reset(key);

            meterRegistry.counter(
                    "ecofy.auth.login",
                    "outcome",
                    "success"
            ).increment();

            return result;
        } catch (AuthException ex) {
            if (ex.getErrorCode()
                    == AuthErrorCode.INVALID_CREDENTIALS) {
                bruteForceProtectionPort.registerFailure(key);

                meterRegistry.counter(
                        "ecofy.auth.login",
                        "outcome",
                        "failure"
                ).increment();
            }

            throw ex;
        }
    }

    // Compõe a chave de proteção com o usuário normalizado e o endereço de origem.
    private String protectionKey(
            String username,
            String ipAddress
    ) {
        String normalizedUser = username == null
                ? ""
                : username.trim().toLowerCase();

        String ip = ipAddress == null || ipAddress.isBlank()
                ? "unknown"
                : ipAddress.trim();

        return "user:"
                + sha256(normalizedUser)
                + "|ip:"
                + ip;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 not available",
                    e
            );
        }
    }
}
