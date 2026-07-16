package br.com.ecofy.ms_categorization.core.application.service;

import br.com.ecofy.ms_categorization.core.domain.CategorizationRule;
import br.com.ecofy.ms_categorization.core.domain.Transaction;
import br.com.ecofy.ms_categorization.core.domain.enums.MatchOperator;
import br.com.ecofy.ms_categorization.core.domain.enums.RuleStatus;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Merchant;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Money;
import br.com.ecofy.ms_categorization.core.domain.valueobject.RuleCondition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine();

    private Transaction tx(String desc, String amount, String currency) {
        return new Transaction(
                UUID.randomUUID(), UUID.randomUUID(), "ext", desc, Merchant.of(desc),
                LocalDate.of(2026, 1, 15), new Money(new BigDecimal(amount), currency),
                "FILE_CSV", null, Instant.now(), Instant.now());
    }

    private CategorizationRule rule(RuleStatus status, int priority, RuleCondition... conds) {
        return new CategorizationRule(UUID.randomUUID(), UUID.randomUUID(), "rule", status, priority,
                List.of(conds), Instant.now(), Instant.now());
    }

    private RuleCondition cond(String field, MatchOperator op, String value, int weight) {
        return new RuleCondition(field, op, value, weight);
    }

    @Test
    void contains_match_producesScore() {
        var r = rule(RuleStatus.ACTIVE, 1, cond("description", MatchOperator.CONTAINS, "coffee", 1));
        int score = engine.score(tx("Coffee Shop", "10.00", "BRL"), r).max().orElse(0);
        assertEquals(30, score); // BASE_CONTAINS * weight 1
    }

    @Test
    void weight_isApplied() {
        var r = rule(RuleStatus.ACTIVE, 1, cond("description", MatchOperator.CONTAINS, "coffee", 3));
        int score = engine.score(tx("Coffee Shop", "10.00", "BRL"), r).max().orElse(0);
        assertEquals(90, score); // 30 * 3
    }

    @Test
    void multipleConditions_allMustMatch_sumsScores() {
        var r = rule(RuleStatus.ACTIVE, 1,
                cond("description", MatchOperator.CONTAINS, "coffee", 1),
                cond("currency", MatchOperator.EQUALS_IGNORE_CASE, "brl", 1));
        int score = engine.score(tx("Coffee Shop", "10.00", "BRL"), r).max().orElse(0);
        assertEquals(30 + 40, score); // CONTAINS + EQUALS
    }

    @Test
    void oneConditionFails_wholeRuleFails() {
        var r = rule(RuleStatus.ACTIVE, 1,
                cond("description", MatchOperator.CONTAINS, "coffee", 1),
                cond("description", MatchOperator.CONTAINS, "restaurant", 1));
        assertTrue(engine.score(tx("Coffee Shop", "10.00", "BRL"), r).max().isEmpty());
    }

    @Test
    void inactiveRule_producesNoScore() {
        var r = rule(RuleStatus.INACTIVE, 1, cond("description", MatchOperator.CONTAINS, "coffee", 1));
        assertTrue(engine.score(tx("Coffee Shop", "10.00", "BRL"), r).max().isEmpty());
    }

    @Test
    void invalidRegex_isSafelyIgnored_noMatch() {
        var r = rule(RuleStatus.ACTIVE, 1, cond("description", MatchOperator.REGEX, "[invalid(", 1));
        assertTrue(engine.score(tx("anything", "10.00", "BRL"), r).max().isEmpty());
    }

    @Test
    void validRegex_matches() {
        var r = rule(RuleStatus.ACTIVE, 1, cond("description", MatchOperator.REGEX, "cof+ee", 1));
        int score = engine.score(tx("Coffee Shop", "10.00", "BRL"), r).max().orElse(0);
        assertEquals(45, score); // BASE_REGEX
    }

    @Test
    void amountGreaterThan_matches() {
        var r = rule(RuleStatus.ACTIVE, 1, cond("amount", MatchOperator.AMOUNT_GREATER_THAN, "100", 1));
        int score = engine.score(tx("x", "250.00", "BRL"), r).max().orElse(0);
        assertEquals(20, score); // BASE_AMOUNT
    }

    @Test
    void noMatch_producesNoScore() {
        var r = rule(RuleStatus.ACTIVE, 1, cond("description", MatchOperator.CONTAINS, "gym", 1));
        assertTrue(engine.score(tx("Coffee Shop", "10.00", "BRL"), r).max().isEmpty());
    }
}
