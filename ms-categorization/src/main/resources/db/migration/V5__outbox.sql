CREATE TABLE cat_outbox_events (
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

CREATE INDEX idx_cat_outbox_eligible
    ON cat_outbox_events (status, next_attempt_at, created_at);

CREATE INDEX idx_cat_outbox_processing_started
    ON cat_outbox_events (processing_started_at)
    WHERE status = 'PROCESSING';

CREATE INDEX idx_cat_outbox_published_at
    ON cat_outbox_events (published_at)
    WHERE status = 'PUBLISHED';

CREATE INDEX idx_cat_outbox_aggregate
    ON cat_outbox_events (aggregate_id);

CREATE INDEX idx_cat_outbox_event_type
    ON cat_outbox_events (event_type);
