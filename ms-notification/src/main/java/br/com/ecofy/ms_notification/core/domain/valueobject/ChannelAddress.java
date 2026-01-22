package br.com.ecofy.ms_notification.core.domain.valueobject;

import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;

import java.util.Objects;

public record ChannelAddress(NotificationChannel channel, String address) {

    // Garante que o endereço do canal seja sempre válido: channel não nulo e address não nulo/nem em branco.
    public ChannelAddress {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(address, "address must not be null");
        if (address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank");
        }
    }

}
