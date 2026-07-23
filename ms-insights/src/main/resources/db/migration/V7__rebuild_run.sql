SET search_path TO ecofy_insights;

CREATE TABLE IF NOT EXISTS insight_rebuild_run (
    id                  UUID          PRIMARY KEY,
    user_id             UUID          NOT NULL,
    insight_type        VARCHAR(40),
    period_start        DATE          NOT NULL,
    period_end          DATE          NOT NULL,
    granularity         VARCHAR(10)   NOT NULL,
    mode                VARCHAR(20)   NOT NULL,

    idempotency_key     VARCHAR(300)  NOT NULL,
    correlation_id      VARCHAR(128),

    status              VARCHAR(30)   NOT NULL,

    checkpoint          DATE,

    processed_items     BIGINT        NOT NULL DEFAULT 0,
    generated_insights  BIGINT        NOT NULL DEFAULT 0,
    failed_items        BIGINT        NOT NULL DEFAULT 0,
    last_error_code     VARCHAR(100),

    created_at          TIMESTAMPTZ   NOT NULL,
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ins_rebuild_user_created
    ON insight_rebuild_run (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ins_rebuild_idem_status
    ON insight_rebuild_run (idempotency_key, status);
