package br.com.ecofy.ms_categorization.core.application.service;

import br.com.ecofy.ms_categorization.core.domain.CategorizationRule;
import br.com.ecofy.ms_categorization.core.domain.Transaction;
import br.com.ecofy.ms_categorization.core.domain.enums.MatchOperator;
import br.com.ecofy.ms_categorization.core.domain.enums.RuleStatus;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Money;
import br.com.ecofy.ms_categorization.core.domain.valueobject.RuleCondition;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

final class RuleEngine {

    private static final int BASE_CONTAINS = 30;
    private static final int BASE_STARTS = 35;
    private static final int BASE_ENDS = 25;
    private static final int BASE_EQUALS = 40;
    private static final int BASE_REGEX = 45;
    private static final int BASE_AMOUNT = 20;

    private final ConcurrentHashMap<String, Pattern> regexCache = new ConcurrentHashMap<>();

    /**
     * Retorna um IntStream vazio quando a regra não "bate" 100% (qualquer condição falhando invalida a regra).
     * Retorna um IntStream com 1 elemento (score total) quando a regra é satisfeita.
     *
     * Isso fica compatível com o service:
     *   int best = engine.score(tx, rule).max().orElse(0);
     */
    // Calcula o score total da regra para a transação e retorna vazio se qualquer condição falhar.
    IntStream score(Transaction tx, CategorizationRule rule) {
        Objects.requireNonNull(tx, "tx must not be null");
        Objects.requireNonNull(rule, "rule must not be null");

        if (rule.getStatus() != RuleStatus.ACTIVE) return IntStream.empty();

        List<RuleCondition> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) return IntStream.empty();

        // Campos normalizados (evita recalcular lower/trim toda hora)
        final String desc = normStr(tx.getDescription());
        final String merchant = tx.getMerchant() != null ? normStr(tx.getMerchant().getNormalized()) : "";
        final Money money = tx.getMoney();
        final String currency = normStr(extractCurrency(money));
        final BigDecimal amount = money != null ? money.getAmount() : null;

        int total = 0;

        for (RuleCondition c : conditions) {
            if (c == null) return IntStream.empty();

            int w = safeWeight(c.getWeight());
            int score = scoreCondition(desc, merchant, currency, amount, c);

            if (score <= 0) {
                // Falhou qualquer condição => regra inteira falha
                return IntStream.empty();
            }

            total += score * w;
        }

        return total > 0 ? IntStream.of(total) : IntStream.empty();
    }

    // Avalia uma condição individual (campo+operador+valor) contra os dados normalizados da transação.
    private int scoreCondition(
            String desc,
            String merchant,
            String currency,
            BigDecimal amount,
            RuleCondition c
    ) {
        String field = c.getField();
        MatchOperator op = c.getOperator();
        String expected = c.getValue();

        if (field == null || field.isBlank()) return 0;
        if (op == null) return 0;
        if (expected == null) expected = "";

        return switch (field) {
            case "description" -> scoreString(desc, op, expected);
            case "merchant" -> scoreString(merchant, op, expected);
            case "currency" -> scoreString(currency, op, expected);
            case "amount" -> scoreAmount(amount, op, expected);
            default -> 0;
        };
    }

    // Pontua comparações baseadas em string (contains/starts/ends/equals/regex) conforme o operador.
    private int scoreString(String actual, MatchOperator op, String expectedRaw) {
        final String expected = normStr(expectedRaw);

        return switch (op) {
            case CONTAINS -> contains(actual, expected) ? BASE_CONTAINS : 0;
            case STARTS_WITH -> starts(actual, expected) ? BASE_STARTS : 0;
            case ENDS_WITH -> ends(actual, expected) ? BASE_ENDS : 0;
            case EQUALS_IGNORE_CASE -> equals(actual, expected) ? BASE_EQUALS : 0;
            case REGEX -> regex(actual, expectedRaw) ? BASE_REGEX : 0;
            default -> 0;
        };
    }

    // Pontua comparações numéricas de amount (greater/less) parseando o valor esperado.
    private int scoreAmount(BigDecimal amount, MatchOperator op, String expectedRaw) {
        if (amount == null) return 0;

        BigDecimal expected;
        try {
            expected = new BigDecimal(expectedRaw);
        } catch (Exception ex) {
            return 0;
        }

        return switch (op) {
            case AMOUNT_GREATER_THAN -> amount.compareTo(expected) > 0 ? BASE_AMOUNT : 0;
            case AMOUNT_LESS_THAN -> amount.compareTo(expected) < 0 ? BASE_AMOUNT : 0;
            default -> 0;
        };
    }

    // Executa match via regex com cache de Pattern para reduzir overhead de compilação.
    private boolean regex(String actual, String patternRaw) {
        if (patternRaw == null || patternRaw.isBlank()) return false;

        try {
            Pattern p = regexCache.computeIfAbsent(patternRaw, k ->
                    Pattern.compile(k, Pattern.CASE_INSENSITIVE)
            );
            return p.matcher(actual).find();
        } catch (Exception ex) {
            return false;
        }
    }

    // Extrai o código de moeda tolerando Money.currency como String ou java.util.Currency.
    private static String extractCurrency(Money money) {
        if (money == null) return "";
        Object c = money.getCurrency();

        if (c == null) return "";
        if (c instanceof String s) return s;
        if (c instanceof Currency cur) return cur.getCurrencyCode();

        return c.toString();
    }

    // Normaliza strings para comparação (trim + lower-case) evitando NPE com default vazio.
    private static String normStr(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    // Verifica se a string atual contém o esperado (ignora esperado vazio).
    private static boolean contains(String a, String b) {
        return !b.isEmpty() && a.contains(b);
    }

    // Verifica se a string atual começa com o esperado (ignora esperado vazio).
    private static boolean starts(String a, String b) {
        return !b.isEmpty() && a.startsWith(b);
    }

    // Verifica se a string atual termina com o esperado (ignora esperado vazio).
    private static boolean ends(String a, String b) {
        return !b.isEmpty() && a.endsWith(b);
    }

    // Verifica igualdade exata entre strings já normalizadas (ignora esperado vazio).
    private static boolean equals(String a, String b) {
        return !b.isEmpty() && a.equals(b);
    }

    // Normaliza o peso da condição garantindo mínimo 1 quando nulo ou inválido.
    private static int safeWeight(Integer w) {
        if (w == null) return 1;
        return Math.max(1, w);
    }

}
