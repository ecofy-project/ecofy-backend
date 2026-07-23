package br.com.ecofy.ms_categorization.adapters.out.messaging;

// Centraliza os tipos e versões dos eventos publicados, cujos valores são contrato público.
public final class EventTypes {

    public static final String PRODUCER = "ms-categorization";

    public static final String TRANSACTION_CATEGORIZED = "TRANSACTION_CATEGORIZED";
    public static final int TRANSACTION_CATEGORIZED_VERSION = 1;

    public static final String CATEGORIZATION_APPLIED = "CATEGORIZATION_APPLIED";
    public static final int CATEGORIZATION_APPLIED_VERSION = 1;

    public static final String AGGREGATE_TYPE_TRANSACTION = "Transaction";

    private EventTypes() {
    }
}
