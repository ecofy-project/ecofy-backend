SET search_path TO ecofy_insights;

CREATE TABLE IF NOT EXISTS outbox_events (
    id                     UUID          PRIMARY KEY,

    aggregate_type         VARCHAR(100)  NOT NULL,
    aggregate_id           UUID          NOT NULL,
    event_type             VARCHAR(150)  NOT NULL,
    event_version          INTEGER       NOT NULL,

    topic                  VARCHAR(255)  NOT NULL,
    partition_key          VARCHAR(255)  NOT NULL,

    payload                TEXT          NOT NULL,

    correlation_id         VARCHAR(128),
    causation_id           UUID,

    -- PENDING | PROCESSING | PUBLISHED | FAILED | DISCARDED (transições guardadas no domínio)
    status                 VARCHAR(30)   NOT NULL,
    attempts               INTEGER       NOT NULL DEFAULT 0,
    next_attempt_at        TIMESTAMPTZ,

    occurred_at            TIMESTAMPTZ   NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL,
    processing_started_at  TIMESTAMPTZ,
    published_at           TIMESTAMPTZ,

    last_error_code        VARCHAR(100),
    last_error_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_ins_outbox_eligible
    ON outbox_events (status, next_attempt_at, created_at);

CREATE INDEX IF NOT EXISTS idx_ins_outbox_processing_started
    ON outbox_events (processing_started_at)
    WHERE status = 'PROCESSING';

CREATE INDEX IF NOT EXISTS idx_ins_outbox_published_at
    ON outbox_events (published_at)
    WHERE status = 'PUBLISHED';

CREATE INDEX IF NOT EXISTS idx_ins_outbox_aggregate
    ON outbox_events (aggregate_id);

CREATE INDEX IF NOT EXISTS idx_ins_outbox_event_type
    ON outbox_events (event_type);
