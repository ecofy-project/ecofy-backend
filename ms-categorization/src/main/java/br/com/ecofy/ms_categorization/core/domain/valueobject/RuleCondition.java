package br.com.ecofy.ms_categorization.core.domain.valueobject;

import br.com.ecofy.ms_categorization.core.domain.enums.MatchOperator;

import java.util.Objects;

public final class RuleCondition {

    private final String field;
    private final MatchOperator operator;
    private final String value;
    private final Integer weight;

    // Define uma condição de regra (campo, operador, valor e peso) garantindo invariantes de null-safety e default de weight=1.
    public RuleCondition(String field, MatchOperator operator, String value, Integer weight) {
        this.field = Objects.requireNonNull(field, "field must not be null");
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
        this.weight = (weight == null) ? 1 : weight;
    }

    // Retorna o campo alvo a ser avaliado pela regra (ex.: description, merchant, currency, amount).
    public String getField() {
        return field;
    }

    // Retorna o operador de comparação que define como o valor será avaliado (ex.: CONTAINS, REGEX, AMOUNT_GREATER_THAN).
    public MatchOperator getOperator() {
        return operator;
    }

    // Retorna o valor esperado usado na comparação (texto, regex ou número em string).
    public String getValue() {
        return value;
    }

    // Retorna o peso da condição usado para ponderar o score total da regra.
    public Integer getWeight() {
        return weight;
    }

    // Define igualdade por valor entre condições (field, operator, value e weight).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuleCondition that)) return false;
        return field.equals(that.field) &&
                operator == that.operator &&
                value.equals(that.value) &&
                weight.equals(that.weight);
    }

    // Gera hash consistente com equals para uso correto em coleções.
    @Override
    public int hashCode() {
        return Objects.hash(field, operator, value, weight);
    }

    // Representa a condição em formato legível para logs/debug.
    @Override
    public String toString() {
        return "RuleCondition[" +
                "field=" + field +
                ", operator=" + operator +
                ", value=" + value +
                ", weight=" + weight +
                ']';
    }

}
