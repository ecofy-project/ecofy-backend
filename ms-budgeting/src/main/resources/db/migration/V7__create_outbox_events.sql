CREATE TABLE outbox_events
(
    id                    UUID                     NOT NULL,
    aggregate_type        VARCHAR(100)             NOT NULL,
    aggregate_id          UUID                     NOT NULL,
    event_type            VARCHAR(150)             NOT NULL,
    event_version         INTEGER                  NOT NULL,
    topic                 VARCHAR(255)             NOT NULL,
    partition_key         VARCHAR(255)             NOT NULL,
    payload               TEXT                     NOT NULL,
    correlation_id        VARCHAR(128),
    causation_id          UUID,
    status                VARCHAR(30)              NOT NULL,
    attempts              INTEGER                  NOT NULL DEFAULT 0,
    next_attempt_at       TIMESTAMP WITH TIME ZONE,
    occurred_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    processing_started_at TIMESTAMP WITH TIME ZONE,
    published_at          TIMESTAMP WITH TIME ZONE,
    last_error_code       VARCHAR(100),
    last_error_at         TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_status_next_attempt
    ON outbox_events (status, next_attempt_at);

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_events_created_at
    ON outbox_events (created_at);