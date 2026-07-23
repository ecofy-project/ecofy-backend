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

// Avalia regras de categorização e calcula suas pontuações.
final class RuleEngine {

    private static final int BASE_CONTAINS = 30;
    private static final int BASE_STARTS = 35;
    private static final int BASE_ENDS = 25;
    private static final int BASE_EQUALS = 40;
    private static final int BASE_REGEX = 45;
    private static final int BASE_AMOUNT = 20;

    private final ConcurrentHashMap<String, Pattern> regexCache = new ConcurrentHashMap<>();

    // Calcula a pontuação total quando todas as condições são atendidas.
    IntStream score(Transaction tx, CategorizationRule rule) {
        Objects.requireNonNull(tx, "tx must not be null");
        Objects.requireNonNull(rule, "rule must not be null");

        if (rule.getStatus() != RuleStatus.ACTIVE) return IntStream.empty();

        List<RuleCondition> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) return IntStream.empty();

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
                return IntStream.empty();
            }

            total += score * w;
        }

        return total > 0 ? IntStream.of(total) : IntStream.empty();
    }

    // Avalia uma condição contra os dados normalizados da transação.
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

    // Pontua comparações textuais conforme o operador informado.
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

    // Pontua comparações monetárias conforme o operador informado.
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

    // Avalia expressões regulares reutilizando padrões compilados.
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

    // Extrai o código monetário independentemente da representação recebida.
    private static String extractCurrency(Money money) {
        if (money == null) return "";
        Object c = money.getCurrency();

        if (c == null) return "";
        if (c instanceof String s) return s;
        if (c instanceof Currency cur) return cur.getCurrencyCode();

        return c.toString();
    }

    // Normaliza textos para comparações consistentes.
    private static String normStr(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String a, String b) {
        return !b.isEmpty() && a.contains(b);
    }

    private static boolean starts(String a, String b) {
        return !b.isEmpty() && a.startsWith(b);
    }

    private static boolean ends(String a, String b) {
        return !b.isEmpty() && a.endsWith(b);
    }

    private static boolean equals(String a, String b) {
        return !b.isEmpty() && a.equals(b);
    }

    // Normaliza o peso garantindo o valor mínimo permitido.
    private static int safeWeight(Integer w) {
        if (w == null) return 1;
        return Math.max(1, w);
    }
}
