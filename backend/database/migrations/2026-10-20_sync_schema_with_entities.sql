-- 2026-10-20: align the live schema with the JPA entity mappings.
--
-- Why: prod runs spring.jpa.hibernate.ddl-auto=validate, so Hibernate refuses to start
-- when a mapped column or table is missing. The entity model had grown columns and one
-- table that core_ccm.sql never received, so a database created from core_ccm.sql (the
-- README path for a fresh install) could not boot the prod profile.
--
-- This script is IDEMPOTENT: every column is added only when missing, the table is
-- created only when absent, and the unique indexes are added only when they do not
-- exist yet (and only when the data allows them).

SET @schema_name = DATABASE();

-- emission_reports.comment
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'comment'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `comment` TEXT',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.upload_mime_type
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'upload_mime_type'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `upload_mime_type` VARCHAR(255)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.upload_size_bytes
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'upload_size_bytes'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `upload_size_bytes` BIGINT',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.upload_storage_key
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'upload_storage_key'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `upload_storage_key` VARCHAR(255)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.upload_rows
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'upload_rows'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `upload_rows` INT',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.parse_error
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'parse_error'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `parse_error` TEXT',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.ai_pre_score
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'ai_pre_score'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `ai_pre_score` DECIMAL(4, 2)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.ai_version
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'ai_version'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `ai_version` VARCHAR(16)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.ai_pre_notes
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'ai_pre_notes'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `ai_pre_notes` TEXT',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.verification_comment
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'verification_comment'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `verification_comment` TEXT',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emission_reports.admin_approved_by_name
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'emission_reports'
      AND COLUMN_NAME = 'admin_approved_by_name'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `emission_reports` ADD COLUMN `admin_approved_by_name` VARCHAR(128)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- credit_certificates.storage_key
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'credit_certificates'
      AND COLUMN_NAME = 'storage_key'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `credit_certificates` ADD COLUMN `storage_key` VARCHAR(512)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- credit_certificates.verify_url
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'credit_certificates'
      AND COLUMN_NAME = 'verify_url'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `credit_certificates` ADD COLUMN `verify_url` VARCHAR(1024)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- payment_order.payment_order_id
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'payment_order'
      AND COLUMN_NAME = 'payment_order_id'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `payment_order` ADD COLUMN `payment_order_id` BIGINT',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- payment_order.provider_ref
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'payment_order'
      AND COLUMN_NAME = 'provider_ref'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `payment_order` ADD COLUMN `provider_ref` VARCHAR(255)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.wallet_id
SET @has_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'wallet_id'
);
SET @ddl = IF(@has_column = 0,
    'ALTER TABLE `users` ADD COLUMN `wallet_id` BIGINT',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Table credit_serial_counters (mapped by CreditSerialCounter, never present before)
CREATE TABLE IF NOT EXISTS `credit_serial_counters` (
    `id`           BIGINT NOT NULL AUTO_INCREMENT,
    `vintage_year` INT NOT NULL,
    `project_id`   BIGINT NOT NULL,
    `company_id`   BIGINT NOT NULL,
    `next_serial`  BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_serial_counter` (`vintage_year`, `project_id`, `company_id`)
) ENGINE=InnoDB;

-- Unique index on users.wallet_id (mapped as a one-to-one). Only added when the current
-- data has no duplicates; otherwise the script leaves the column without the index and
-- the duplicates have to be cleaned by hand.
SET @duplicate_wallets = (
    SELECT COUNT(*) FROM (
        SELECT `wallet_id` FROM `users`
        WHERE `wallet_id` IS NOT NULL
        GROUP BY `wallet_id` HAVING COUNT(*) > 1
    ) AS duplicates
);
SET @has_index = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'users'
      AND INDEX_NAME = 'uk_users_wallet'
);
SET @ddl = IF(@has_index = 0 AND @duplicate_wallets = 0,
    'CREATE UNIQUE INDEX `uk_users_wallet` ON `users` (`wallet_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
