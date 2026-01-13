package br.com.ecofy.ms_users.adapters.in.kafka.mapper;

import br.com.ecofy.ms_users.adapters.in.kafka.dto.AuthUserCreatedEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Component
public class InboundEventMapper {

    public AuthUserCreatedEventMessage toAuthUserCreated(Object payload) {
        Objects.requireNonNull(payload, "payload must not be null");

        if (payload instanceof AuthUserCreatedEventMessage msg) {
            log.debug(
                    "[InboundEventMapper] - [toAuthUserCreated] -> mapped payloadType={} userId={} emailPresent={}",
                    msg.getClass().getSimpleName(),
                    safeUserId(msg),
                    safeEmailPresent(msg)
            );
            return msg;
        }

        if (payload instanceof byte[] bytes) {
            String preview = preview(new String(bytes, StandardCharsets.UTF_8));
            log.warn(
                    "[InboundEventMapper] - [toAuthUserCreated] -> unsupported payloadType=byte[] preview={}",
                    preview
            );
            throw new IllegalArgumentException(
                    "Unsupported payload type=byte[]. Configure Kafka JsonDeserializer to deserialize into AuthUserCreatedEventMessage. " +
                            "payloadPreview=" + preview
            );
        }

        if (payload instanceof String s) {
            String preview = preview(s);
            log.warn(
                    "[InboundEventMapper] - [toAuthUserCreated] -> unsupported payloadType=String preview={}",
                    preview
            );
            throw new IllegalArgumentException(
                    "Unsupported payload type=String. Configure Kafka JsonDeserializer to deserialize into AuthUserCreatedEventMessage. " +
                            "payloadPreview=" + preview
            );
        }

        log.warn(
                "[InboundEventMapper] - [toAuthUserCreated] -> unsupported payloadType={} expectedType={}",
                payload.getClass().getName(),
                AuthUserCreatedEventMessage.class.getName()
        );

        throw new IllegalArgumentException(
                "Unsupported payload type=" + payload.getClass().getName() +
                        ". Expected " + AuthUserCreatedEventMessage.class.getName()
        );
    }

    private static String preview(String s) {
        if (s == null) return "<null>";
        String v = s.replaceAll("\\s+", " ").trim();
        return v.length() <= 200 ? v : v.substring(0, 200) + "...";
    }

    // Ajuste os getters conforme o DTO real (record vs class). Mantive defensivo.
    private static Object safeUserId(AuthUserCreatedEventMessage msg) {
        try {
            return msg.userId();
        } catch (Exception ignore) {
            return "<unknown>";
        }
    }

    private static boolean safeEmailPresent(AuthUserCreatedEventMessage msg) {
        try {
            var email = msg.email();
            return email != null && !email.isBlank();
        } catch (Exception ignore) {
            return false;
        }
    }
}
