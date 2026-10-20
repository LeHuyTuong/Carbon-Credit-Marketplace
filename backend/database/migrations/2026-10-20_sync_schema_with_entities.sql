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

-- Fail fast when the script runs without a schema selected (mysql < script.sql).
-- The schema name comes from SPRING_DATASOURCE_URL, e.g.
--   mysql core_ccm < backend/database/migrations/<file>.sql
SET @ddl = IF(DATABASE() IS NULL,
    'SELECT * FROM __no_schema_selected__',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


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

-- Column types -----------------------------------------------------------------
-- Hibernate's schema validation compares the COLUMN TYPE, not only its name, so a
-- DOUBLE where the entity maps BigDecimal(18,2) (or a shorter VARCHAR) stops the
-- prod profile from booting. Each ALTER only runs when the live type differs.
SET @current_type = (
    SELECT COLUMN_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'carbon_credits' AND COLUMN_NAME = 'current_price'
);
SET @ddl = IF(@current_type IS NOT NULL AND @current_type <> 'decimal(18,2)',
    'ALTER TABLE `carbon_credits` MODIFY COLUMN `current_price` DECIMAL(18, 2) NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @current_type = (
    SELECT COLUMN_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'carbon_credits' AND COLUMN_NAME = 'amount'
);
SET @ddl = IF(@current_type IS NOT NULL AND @current_type <> 'decimal(38,2)',
    'ALTER TABLE `carbon_credits` MODIFY COLUMN `amount` DECIMAL(38, 2)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @current_type = (
    SELECT COLUMN_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'carbon_credits' AND COLUMN_NAME = 'listed_amount'
);
SET @ddl = IF(@current_type IS NOT NULL AND @current_type <> 'decimal(38,2)',
    'ALTER TABLE `carbon_credits` MODIFY COLUMN `listed_amount` DECIMAL(38, 2)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- payment_details.bank_code / account_holder_name are NOT narrowed on purpose: the
-- entity maps them from the bean-validation @Size, but Hibernate's validator only
-- compares the JDBC type code (VARCHAR), not the width. Narrowing a live column would
-- abort the migration on rows that are longer than the new width (or truncate them).

-- wallet_transaction.transaction_type: the enum must contain every value the
-- WalletTransactionType enum can write, otherwise the withdrawal-refund insert is
-- truncated and the rejected withdrawal is never refunded.
SET @current_type = (
    SELECT COLUMN_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'wallet_transaction' AND COLUMN_NAME = 'transaction_type'
);
SET @ddl = IF(@current_type IS NOT NULL AND @current_type NOT LIKE '%WITHDRAWAL_REFUND%',
    'ALTER TABLE `wallet_transaction` MODIFY COLUMN `transaction_type` '
        'ENUM(''ADD_MONEY'',''BUY_CARBON_CREDIT'',''EV_OWNER_PAYOUT'',''EV_OWNER_PAYOUT_RECEIVE'','
        '''ISSUE_CREDIT'',''PROFIT_SHARING'',''RETIRE_CREDIT'',''SELL_CARBON_CREDIT'','
        '''SELL_CARBON_CREDIT_SHARE'',''WITHDRAWAL'',''WITHDRAWAL_REFUND'') NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- wallets: the one-to-one mapping implies a unique key per owner/company/credit. Without
-- it, two concurrent wallet creations leave duplicate rows and every later lookup
-- (findByUserId) throws IncorrectResultSizeDataAccessException for that user forever.
-- The index is only created when the current data has no duplicates.
SET @duplicate_wallets = (
    SELECT COUNT(*) FROM (
        SELECT `user_id` FROM `wallets` WHERE `user_id` IS NOT NULL
        GROUP BY `user_id` HAVING COUNT(*) > 1
    ) AS duplicates
);
SET @has_index = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'wallets' AND INDEX_NAME = 'uk_wallets_user'
);
SET @ddl = IF(@has_index = 0 AND @duplicate_wallets = 0,
    'CREATE UNIQUE INDEX `uk_wallets_user` ON `wallets` (`user_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @duplicate_companies = (
    SELECT COUNT(*) FROM (
        SELECT `company_id` FROM `wallets` WHERE `company_id` IS NOT NULL
        GROUP BY `company_id` HAVING COUNT(*) > 1
    ) AS duplicates
);
SET @has_index = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'wallets' AND INDEX_NAME = 'uk_wallets_company'
);
SET @ddl = IF(@has_index = 0 AND @duplicate_companies = 0,
    'CREATE UNIQUE INDEX `uk_wallets_company` ON `wallets` (`company_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @duplicate_credits = (
    SELECT COUNT(*) FROM (
        SELECT `carbon_credit_id` FROM `wallets` WHERE `carbon_credit_id` IS NOT NULL
        GROUP BY `carbon_credit_id` HAVING COUNT(*) > 1
    ) AS duplicates
);
SET @has_index = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'wallets' AND INDEX_NAME = 'uk_wallets_carbon_credit'
);
SET @ddl = IF(@has_index = 0 AND @duplicate_credits = 0,
    'CREATE UNIQUE INDEX `uk_wallets_carbon_credit` ON `wallets` (`carbon_credit_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
