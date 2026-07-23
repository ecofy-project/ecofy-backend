-- V2__add_ownership_idempotency_and_counters.sql
-- contadores de processamento parcial.
--
-- V1 não é alterada. Colunas NOT NULL entram em duas fases (add nullable -> backfill ->
-- set not null) para que a migration funcione em bases dev que já tenham linhas.

-- import_file: dono + hash do conteúdo (chave de idempotência)
ALTER TABLE import_file ADD COLUMN user_id         UUID;
ALTER TABLE import_file ADD COLUMN file_hash       VARCHAR(128);
ALTER TABLE import_file ADD COLUMN idempotency_key VARCHAR(128);

-- Backfill: linhas anteriores à Etapa 4 não têm dono nem hash conhecidos. Recebem o
-- UUID nulo como dono sentinela (nenhum JWT real resolve para ele, então ficam
-- inacessíveis pela API) e um hash sintético derivado do id, que é único por
-- construção e portanto não colide no índice de idempotência.
UPDATE import_file
   SET user_id   = '00000000-0000-0000-0000-000000000000'
 WHERE user_id IS NULL;

UPDATE import_file
   SET file_hash = 'legacy:' || id::text
 WHERE file_hash IS NULL;

ALTER TABLE import_file ALTER COLUMN user_id   SET NOT NULL;
ALTER TABLE import_file ALTER COLUMN file_hash SET NOT NULL;

-- Proteção FINAL de idempotência: o mesmo conteúdo, do mesmo dono, só existe
-- uma vez. É esta constraint — não um existsBy — que resolve o upload concorrente.
CREATE UNIQUE INDEX uk_import_file_user_file_hash
    ON import_file (user_id, file_hash);

-- Idempotency-Key é opcional; o índice parcial só vale para quem a enviou.
CREATE UNIQUE INDEX uk_import_file_user_idempotency_key
    ON import_file (user_id, idempotency_key)
 WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_import_file_user_id
    ON import_file (user_id);

-- import_job: dono (desnormalizado), correlation ID, contadores e causa da falha
ALTER TABLE import_job ADD COLUMN user_id UUID;

-- Dono do job = dono do arquivo que o originou.
UPDATE import_job j
   SET user_id = f.user_id
  FROM import_file f
 WHERE j.import_file_id = f.id
   AND j.user_id IS NULL;

ALTER TABLE import_job ALTER COLUMN user_id SET NOT NULL;

-- Desnormalizado a partir de import_file para que o histórico paginado (ECO-10)
-- filtre por dono e ordene por created_at usando um único índice, sem join.
CREATE INDEX idx_import_job_user_created_at
    ON import_job (user_id, created_at DESC);

CREATE INDEX idx_import_job_user_status
    ON import_job (user_id, status);

ALTER TABLE import_job ADD COLUMN duplicate_records INTEGER     NOT NULL DEFAULT 0;
ALTER TABLE import_job ADD COLUMN published_records INTEGER     NOT NULL DEFAULT 0;
ALTER TABLE import_job ADD COLUMN recorded_errors   INTEGER     NOT NULL DEFAULT 0;
ALTER TABLE import_job ADD COLUMN errors_truncated  BOOLEAN     NOT NULL DEFAULT FALSE;

-- Causa resumida da falha global. VARCHAR curto e deliberado: stack trace nunca é
-- persistido.
ALTER TABLE import_job ADD COLUMN failure_code      VARCHAR(64);
ALTER TABLE import_job ADD COLUMN failure_reason    VARCHAR(500);

-- Deadline de processamento (§13): permite detectar job abandonado em RUNNING.
ALTER TABLE import_job ADD COLUMN deadline_at       TIMESTAMPTZ;

-- Correlation ID da requisição que originou o job.
-- 128 = CorrelationId.MAX_LENGTH, o mesmo teto validado pelo filtro e pelo Gateway.
ALTER TABLE import_job ADD COLUMN correlation_id    VARCHAR(128);

-- raw_transaction: chave estável por linha
ALTER TABLE raw_transaction ADD COLUMN row_hash VARCHAR(128);

-- Linhas legadas recebem hash sintético único (mesma lógica do backfill acima).
UPDATE raw_transaction
   SET row_hash = 'legacy:' || id::text
 WHERE row_hash IS NULL;

ALTER TABLE raw_transaction ALTER COLUMN row_hash SET NOT NULL;

-- Idempotência por linha dentro de um mesmo arquivo: reprocessar o arquivo (retry) ou
-- reenviá-lo não recria a mesma transação.
CREATE UNIQUE INDEX uk_raw_tx_file_row_hash
    ON raw_transaction (import_file_id, row_hash);
