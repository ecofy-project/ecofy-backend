package br.com.ecofy.ms_categorization.core.domain.valueobject;

import java.util.Objects;

public final class Merchant {

    private final String raw;
    private final String normalized;

    // Mantém a descrição original (raw) e a versão normalizada (normalized) para matching determinístico.
    public Merchant(String raw, String normalized) {
        this.raw = Objects.requireNonNull(raw, "raw must not be null");
        this.normalized = Objects.requireNonNull(normalized, "normalized must not be null");
    }

    // Retorna a descrição original do merchant (sem normalização).
    public String getRaw() {
        return raw;
    }

    // Retorna a descrição normalizada do merchant (para comparação e regras).
    public String getNormalized() {
        return normalized;
    }

    // Cria um Merchant a partir da descrição, gerando a versão normalizada para uso em regras/matching.
    public static Merchant of(String description) {
        var raw = description == null ? "" : description;
        var normalized = Normalizer.normalize(raw);
        return new Merchant(raw, normalized);
    }

    static final class Normalizer {
        private Normalizer() {}

        // Normaliza texto (lowercase, remove acentos, remove símbolos e compacta espaços) para padronizar comparações.
        static String normalize(String s) {
            if (s == null) return "";
            String t = s.trim().toLowerCase();
            t = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");
            t = t.replaceAll("[^a-z0-9\\s]", " ");
            t = t.replaceAll("\\s+", " ").trim();
            return t;
        }
    }

    // Define igualdade por valor considerando raw e normalized (value object).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Merchant merchant)) return false;
        return raw.equals(merchant.raw) &&
                normalized.equals(merchant.normalized);
    }

    // Gera hash consistente com equals para uso em coleções.
    @Override
    public int hashCode() {
        return Objects.hash(raw, normalized);
    }

    // Representa o Merchant em formato legível para logs/debug.
    @Override
    public String toString() {
        return "Merchant[" +
                "raw=" + raw +
                ", normalized=" + normalized +
                ']';
    }

}
