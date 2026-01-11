package br.com.ecofy.ms_users.core.port.out;

public interface PublishUserEventPort {
    void publishUserProfileCreated(String key, Object payload);
    void publishUserProfileUpdated(String key, Object payload);
}