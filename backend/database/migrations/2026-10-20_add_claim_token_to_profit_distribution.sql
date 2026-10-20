-- 2026-10-20: add the per-run claim token used to fence payout workers.
--
-- Why: a PROCESSING distribution can be reclaimed after
-- profit-sharing.stale-processing-minutes without a heartbeat. Without a token, the
-- worker that lost its claim can still overwrite the new claim's status (FAILED),
-- and two workers can run the same retry. Every claim now writes a UUID; heartbeat
-- and COMPLETED/FAILED transitions only apply while that UUID still matches.
--
-- IDEMPOTENT: safe on a fresh database created from core_ccm.sql (column exists) and
-- on an existing database created before this change.

SET @schema_name = DATABASE();

SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'profit_distribution'
      AND COLUMN_NAME = 'claim_token'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `profit_distribution` ADD COLUMN `claim_token` VARCHAR(36) NULL AFTER `status`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
