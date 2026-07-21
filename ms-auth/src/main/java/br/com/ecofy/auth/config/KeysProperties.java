package br.com.ecofy.auth.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Configura as chaves utilizadas na assinatura, verificação e rotação dos tokens.
@ConfigurationProperties(prefix = "ecofy.auth.keys")
public class KeysProperties {

    private boolean allowGeneratedKey = true;

    private String activeKid = "local-dev-key";

    private String algorithm = "RS256";

    private String activePrivateKey;

    private String activePrivateKeyLocation;

    private Duration retentionWindow = Duration.ofHours(24);

    private List<RetiringKey> retiring = new ArrayList<>();

    public boolean isAllowGeneratedKey() {
        return allowGeneratedKey;
    }

    public void setAllowGeneratedKey(boolean allowGeneratedKey) {
        this.allowGeneratedKey = allowGeneratedKey;
    }

    public String getActiveKid() {
        return activeKid;
    }

    public void setActiveKid(String activeKid) {
        this.activeKid = activeKid;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getActivePrivateKey() {
        return activePrivateKey;
    }

    public void setActivePrivateKey(String activePrivateKey) {
        this.activePrivateKey = activePrivateKey;
    }

    public String getActivePrivateKeyLocation() {
        return activePrivateKeyLocation;
    }

    public void setActivePrivateKeyLocation(
            String activePrivateKeyLocation
    ) {
        this.activePrivateKeyLocation = activePrivateKeyLocation;
    }

    public Duration getRetentionWindow() {
        return retentionWindow;
    }

    public void setRetentionWindow(Duration retentionWindow) {
        this.retentionWindow = retentionWindow;
    }

    public List<RetiringKey> getRetiring() {
        return retiring;
    }

    public void setRetiring(List<RetiringKey> retiring) {
        this.retiring = retiring == null
                ? new ArrayList<>()
                : retiring;
    }

    // Representa uma chave pública mantida durante o período de rotação.
    public static class RetiringKey {

        private String kid;
        private String publicKey;
        private String publicKeyLocation;

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPublicKeyLocation() {
            return publicKeyLocation;
        }

        public void setPublicKeyLocation(String publicKeyLocation) {
            this.publicKeyLocation = publicKeyLocation;
        }
    }
}
