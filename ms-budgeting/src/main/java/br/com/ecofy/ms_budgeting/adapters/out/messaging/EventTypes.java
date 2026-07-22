package br.com.ecofy.ms_budgeting.adapters.out.messaging;

// Centraliza os tipos e versões dos eventos publicados, cujos valores serializados são contrato público.
public final class EventTypes {

    public static final String PRODUCER = "ms-budgeting";

    public static final String BUDGET_ALERT = "BUDGET_ALERT";
    public static final int BUDGET_ALERT_VERSION = 1;

    public static final String AGGREGATE_TYPE_BUDGET = "Budget";

    private EventTypes() {
    }
}
