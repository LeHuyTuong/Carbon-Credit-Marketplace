-- 2026-10-20: link a payout distribution to the emission report it pays.
--
-- Why: a retry must pay only the owners whose detail is PENDING/FAILED. Without this
-- link the service can only re-run the whole report, which could pay the already-SUCCESS
-- owners a second time.
--
-- This script is IDEMPOTENT and safe to run on both:
--   * a fresh database created from core_ccm.sql (the column/FK/index already exist), and
--   * an existing database created before this change.
--
-- It also backfills the link for legacy distributions when, and only when, exactly one
-- eligible report exists for the same company + project and that report already existed
-- when the distribution was created. Ambiguous rows are left NULL on purpose.

-- 1. Column -----------------------------------------------------------------
SET @schema_name = DATABASE();

-- Fail fast when the script runs without a schema selected (mysql < script.sql).
-- The schema name comes from SPRING_DATASOURCE_URL, e.g.
--   mysql core_ccm < backend/database/migrations/<file>.sql
SET @ddl = IF(DATABASE() IS NULL,
    'SELECT * FROM __no_schema_selected__',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'profit_distribution'
      AND COLUMN_NAME = 'emission_report_id'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `profit_distribution` ADD COLUMN `emission_report_id` BIGINT NULL AFTER `project_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Backfill (single unambiguous candidate only) ----------------------------
CREATE TEMPORARY TABLE IF NOT EXISTS `tmp_profit_distribution_report_link` (
    `distribution_id` BIGINT NOT NULL PRIMARY KEY,
    `report_id` BIGINT NOT NULL
);

TRUNCATE TABLE `tmp_profit_distribution_report_link`;

INSERT INTO `tmp_profit_distribution_report_link` (`distribution_id`, `report_id`)
SELECT d.`id`, MIN(r.`id`) AS `report_id`
FROM `profit_distribution` d
JOIN `company` c ON c.`user_id` = d.`company_user_id`
JOIN `emission_reports` r
    ON r.`project_id` = d.`project_id`
   AND r.`seller_id` = c.`id`
WHERE d.`emission_report_id` IS NULL
  AND r.`status` IN ('CREDIT_ISSUED', 'PAID_OUT')
  AND (d.`created_at` IS NULL OR r.`created_at` IS NULL OR r.`created_at` <= d.`created_at`)
GROUP BY d.`id`
HAVING COUNT(*) = 1;

UPDATE `profit_distribution` d
JOIN `tmp_profit_distribution_report_link` t ON t.`distribution_id` = d.`id`
SET d.`emission_report_id` = t.`report_id`
WHERE d.`emission_report_id` IS NULL;

DROP TEMPORARY TABLE `tmp_profit_distribution_report_link`;

-- 3. Index -----------------------------------------------------------------
SET @has_index = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'profit_distribution'
      AND INDEX_NAME = 'idx_profit_distribution_report'
);
SET @ddl = IF(@has_index = 0,
    'CREATE INDEX `idx_profit_distribution_report` ON `profit_distribution` (`emission_report_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. Foreign key -----------------------------------------------------------
SET @has_fk = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @schema_name
      AND TABLE_NAME = 'profit_distribution'
      AND CONSTRAINT_NAME = 'fk_profit_distribution_report'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @ddl = IF(@has_fk = 0,
    'ALTER TABLE `profit_distribution` ADD CONSTRAINT `fk_profit_distribution_report` '
        'FOREIGN KEY (`emission_report_id`) REFERENCES `emission_reports` (`id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
