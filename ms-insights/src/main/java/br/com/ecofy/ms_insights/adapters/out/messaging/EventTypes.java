package br.com.ecofy.ms_insights.adapters.out.messaging;

// Centraliza os tipos e versões dos eventos publicados, cujos valores serializados são contrato público.
public final class EventTypes {

    public static final String PRODUCER = "ms-insights";

    public static final String INSIGHT_CREATED = "INSIGHT_CREATED";
    public static final int INSIGHT_CREATED_VERSION = 1;

    public static final String AGGREGATE_TYPE_INSIGHT = "Insight";

    private EventTypes() {
    }
}
