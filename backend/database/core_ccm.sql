CREATE DATABASE IF NOT EXISTS core_ccm3 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE core_ccm3;

-- Tắt kiểm tra khóa ngoại để tránh lỗi khi tạo lại bảng
SET FOREIGN_KEY_CHECKS = 0;

-- =======================================================
-- 1. MODULE: AUTHENTICATION & USERS
-- =======================================================

-- Bảng users (Tài khoản gốc)
CREATE TABLE IF NOT EXISTS `users` (
                                       `id`            BIGINT NOT NULL AUTO_INCREMENT,
                                       `email`         VARCHAR(255) NOT NULL,
                                       `password_hash` VARCHAR(255) NOT NULL,
                                       `status`        ENUM('ACTIVE','BANNED','INACTIVE','PENDING') NOT NULL DEFAULT 'PENDING',
                                       `otp_code`      VARCHAR(255) DEFAULT NULL,
                                       `otp_expiry_at` DATETIME(6) DEFAULT NULL,
                                       `otp_purpose`   ENUM('FORGOT_PASSWORD','REGISTER') DEFAULT NULL,
                                       `created_at`    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                       `updated_at`    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB;

-- Bảng roles
CREATE TABLE IF NOT EXISTS `roles` (
                                       `role_id`     BIGINT NOT NULL AUTO_INCREMENT,
                                       `name`        VARCHAR(50) NOT NULL UNIQUE, -- VD: ROLE_USER, ROLE_ADMIN
                                       `description` VARCHAR(255) DEFAULT NULL,
                                       PRIMARY KEY (`role_id`)
) ENGINE=InnoDB;

-- Bảng user_role (Many-to-Many)
CREATE TABLE IF NOT EXISTS `user_role` (
                                           `user_id` BIGINT NOT NULL,
                                           `role_id` BIGINT NOT NULL,
                                           PRIMARY KEY (`user_id`, `role_id`),
                                           CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                                           CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB;

-- Bảng admin (Profile Admin)
CREATE TABLE IF NOT EXISTS `admin` (
                                       `admin_id`    BIGINT NOT NULL AUTO_INCREMENT,
                                       `user_id`     BIGINT NOT NULL,
                                       `name`        VARCHAR(100) NOT NULL,
                                       `first_name`  VARCHAR(50),
                                       `last_name`   VARCHAR(50),
                                       `birthday`    VARCHAR(15),
                                       `phone`       VARCHAR(15),
                                       `city`        VARCHAR(100),
                                       `country`     VARCHAR(100),
                                       `avatar_url`  VARCHAR(512),
                                       `created_at`  DATETIME(6) DEFAULT NULL,
                                       `updated_at`  DATETIME(6) DEFAULT NULL,
                                       PRIMARY KEY (`admin_id`),
                                       UNIQUE KEY `uk_admin_user` (`user_id`),
                                       CONSTRAINT `fk_admin_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

-- Bảng cva (Chuyên gia thẩm định)
CREATE TABLE IF NOT EXISTS `cva` (
                                     `cva_id`         BIGINT NOT NULL AUTO_INCREMENT,
                                     `user_id`        BIGINT NOT NULL,
                                     `name`           VARCHAR(255) NOT NULL,
                                     `code`           VARCHAR(255),
                                     `email`          VARCHAR(255) NOT NULL,
                                     `organization`   VARCHAR(255),
                                     `position_title` VARCHAR(255),
                                     `avatar_url`     VARCHAR(255),
                                     `status`         ENUM('ACTIVE','BANNED','INACTIVE','PENDING') NOT NULL DEFAULT 'PENDING',
                                     `created_at`     DATETIME(6) DEFAULT NULL,
                                     `updated_at`     DATETIME(6) DEFAULT NULL,
                                     PRIMARY KEY (`cva_id`),
                                     UNIQUE KEY `uk_cva_user` (`user_id`),
                                     CONSTRAINT `fk_cva_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

-- Bảng company (Doanh nghiệp)
CREATE TABLE IF NOT EXISTS `company` (
                                         `id`               BIGINT NOT NULL AUTO_INCREMENT,
                                         `user_id`          BIGINT NOT NULL,
                                         `code`             VARCHAR(16) UNIQUE,
                                         `company_name`     VARCHAR(100),
                                         `business_license` VARCHAR(100) NOT NULL,
                                         `tax_code`         VARCHAR(100),
                                         `address`          TEXT,
                                         `created_at`       DATETIME(6) DEFAULT NULL,
                                         `updated_at`       DATETIME(6) DEFAULT NULL,
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_company_user` (`user_id`),
                                         CONSTRAINT `fk_company_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

-- Bảng ev_owner (Chủ sở hữu xe điện)
CREATE TABLE IF NOT EXISTS `ev_owner` (
                                          `user_id`         BIGINT NOT NULL, -- Dùng luôn ID của User làm khóa chính (OneToOne)
                                          `name`            VARCHAR(255) NOT NULL,
                                          `email`           VARCHAR(100) NOT NULL,
                                          `phone`           VARCHAR(32) NOT NULL,
                                          `address`         VARCHAR(100) NOT NULL,
                                          `country`         VARCHAR(100) NOT NULL,
                                          `birth_date`      DATE NOT NULL,
                                          `gender`          ENUM('FEMALE','MALE','OTHER') NOT NULL,
                                          `document_type`   ENUM('CCCD','CMND','DRIVE_CARD') NOT NULL,
                                          `document_number` VARCHAR(255),
                                          `company_id`      BIGINT, -- Có thể thuộc về 1 công ty vận tải
                                          `created_at`      DATETIME(6) DEFAULT NULL,
                                          `updated_at`      DATETIME(6) DEFAULT NULL,
                                          PRIMARY KEY (`user_id`),
                                          CONSTRAINT `fk_ev_owner_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                                          CONSTRAINT `fk_ev_owner_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`)
) ENGINE=InnoDB;

-- Bảng vehicles (Phương tiện của EV Owner)
CREATE TABLE IF NOT EXISTS `vehicles` (
                                          `id`           BIGINT NOT NULL AUTO_INCREMENT,
                                          `ev_owner_id`  BIGINT NOT NULL,
                                          `company_id`   BIGINT NOT NULL,
                                          `plate_number` VARCHAR(64) NOT NULL,
                                          `brand`        VARCHAR(128) NOT NULL,
                                          `model`        VARCHAR(128) NOT NULL,
                                          `document_url` VARCHAR(255),
                                          `created_at`   DATETIME(6) DEFAULT NULL,
                                          `updated_at`   DATETIME(6) DEFAULT NULL,
                                          PRIMARY KEY (`id`),
                                          CONSTRAINT `fk_vehicles_ev_owner` FOREIGN KEY (`ev_owner_id`) REFERENCES `ev_owner` (`user_id`),
                                          CONSTRAINT `fk_vehicles_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`)
) ENGINE=InnoDB;

-- =======================================================
-- 2. MODULE: PROJECT & CORE ASSETS
-- =======================================================

-- Bảng project
CREATE TABLE IF NOT EXISTS `project` (
                                         `id`                         BIGINT NOT NULL AUTO_INCREMENT,
                                         `code`                       VARCHAR(16) UNIQUE,
                                         `title`                      VARCHAR(100) NOT NULL,
                                         `description`                VARCHAR(255) NOT NULL,
                                         `status`                     ENUM('CLOSE','COMING_SOON','OPEN') NOT NULL,
                                         `logo`                       VARCHAR(512),
                                         `commitments`                TEXT,
                                         `technical_indicators`       TEXT,
                                         `measurement_method`         TEXT,
                                         `legal_docs_file`            VARCHAR(255),
                                         `emission_factor_kg_per_kwh` DECIMAL(10, 4),
                                         `buffer_reserve_pct`         DECIMAL(5, 4),
                                         `uncertainty_pct`            DECIMAL(5, 4),
                                         `leakage_pct`                DECIMAL(5, 4),
                                         `start_date`                 DATE,
                                         `end_date`                   DATE,
                                         `created_at`                 DATETIME(6) DEFAULT NULL,
                                         `updated_at`                 DATETIME(6) DEFAULT NULL,
                                         PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- Bảng project_application (Hồ sơ đăng ký dự án)
CREATE TABLE IF NOT EXISTS `project_application` (
                                                     `id`                    BIGINT NOT NULL AUTO_INCREMENT,
                                                     `project_id`            BIGINT NOT NULL,
                                                     `company_id`            BIGINT NOT NULL,
                                                     `reviewer_id`           BIGINT DEFAULT NULL,
                                                     `final_reviewer_id`     BIGINT DEFAULT NULL,
                                                     `status`                ENUM('ADMIN_APPROVED','ADMIN_REJECTED','CVA_APPROVED','CVA_REJECTED','NEEDS_REVISION','SUBMITTED','UNDER_REVIEW') NOT NULL,
                                                     `application_docs_path` VARCHAR(512),
                                                     `application_docs_url`  VARCHAR(512),
                                                     `review_note`           TEXT,
                                                     `final_review_note`     TEXT,
                                                     `submitted_at`          DATETIME(6),
                                                     `reviewed_at`           DATETIME(6),
                                                     `created_at`            DATETIME(6) NOT NULL,
                                                     `updated_at`            DATETIME(6) NOT NULL,
                                                     PRIMARY KEY (`id`),
                                                     CONSTRAINT `fk_app_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
                                                     CONSTRAINT `fk_app_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`)
) ENGINE=InnoDB;

-- Bảng emission_reports (Báo cáo giảm phát thải)
CREATE TABLE IF NOT EXISTS `emission_reports` (
                                                  `id`                       BIGINT NOT NULL AUTO_INCREMENT,
                                                  `project_id`               BIGINT NOT NULL,
                                                  `seller_id`                BIGINT NOT NULL, -- Company ID
                                                  `period`                   VARCHAR(16) NOT NULL,
                                                  `source`                   VARCHAR(16) NOT NULL,
                                                  `status`                   ENUM('ADMIN_APPROVED','ADMIN_REJECTED','APPROVED','CREDIT_ISSUED','CVA_APPROVED','CVA_REJECTED','DRAFT','PAID_OUT','REJECTED','SUBMITTED') NOT NULL,
                                                  `total_co2`                DECIMAL(14, 4) NOT NULL,
                                                  `total_energy`             DECIMAL(14, 4) NOT NULL,
                                                  `residual_tco2e`           DECIMAL(10, 3),
                                                  `vehicle_count`            INT NOT NULL,
                                                  `upload_original_filename` VARCHAR(255),
                                                  `upload_storage_url`       VARCHAR(1024),
                                                  `upload_sha256`            VARCHAR(128),
                                                  `verified_by_cva_id`       BIGINT,
                                                  `verified_by_cva_name`     VARCHAR(128),
                                                  `verification_score`       DECIMAL(4, 2),
                                                  `submitted_at`             DATETIME(6),
                                                  `verified_at`              DATETIME(6),
                                                  `approved_at`              DATETIME(6),
                                                  `created_at`               DATETIME(6),
                                                  `updated_at`               DATETIME(6),
                                                  PRIMARY KEY (`id`),
                                                  CONSTRAINT `fk_report_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
                                                  CONSTRAINT `fk_report_company` FOREIGN KEY (`seller_id`) REFERENCES `company` (`id`)
) ENGINE=InnoDB;

-- Bảng emission_report_details (Chi tiết báo cáo)
CREATE TABLE IF NOT EXISTS `emission_report_details` (
                                                         `id`           BIGINT NOT NULL AUTO_INCREMENT,
                                                         `report_id`    BIGINT NOT NULL,
                                                         `project_id`   BIGINT NOT NULL,
                                                         `company_id`   BIGINT NOT NULL,
                                                         `vehicle_plate` VARCHAR(50),
                                                         `total_energy` DECIMAL(18, 4) NOT NULL,
                                                         `co2_kg`       DECIMAL(18, 4) NOT NULL,
                                                         `period`       VARCHAR(16) NOT NULL,
                                                         PRIMARY KEY (`id`),
                                                         CONSTRAINT `fk_detail_report` FOREIGN KEY (`report_id`) REFERENCES `emission_reports` (`id`)
) ENGINE=InnoDB;

-- Bảng credit_batches (Lô tín chỉ)
CREATE TABLE IF NOT EXISTS `credit_batches` (
                                                `id`             BIGINT NOT NULL AUTO_INCREMENT,
                                                `batch_code`     VARCHAR(64) NOT NULL,
                                                `project_id`     BIGINT NOT NULL,
                                                `company_id`     BIGINT NOT NULL,
                                                `report_id`      BIGINT NOT NULL,
                                                `vintage_year`   INT NOT NULL,
                                                `total_tco2e`    DECIMAL(18, 3) NOT NULL,
                                                `residual_tco2e` DECIMAL(18, 3) NOT NULL,
                                                `credits_count`  INT NOT NULL,
                                                `serial_prefix`  VARCHAR(64) NOT NULL,
                                                `serial_from`    BIGINT NOT NULL,
                                                `serial_to`      BIGINT NOT NULL,
                                                `status`         VARCHAR(16) NOT NULL,
                                                `issued_by`      VARCHAR(100),
                                                `issued_at`      DATETIME(6),
                                                `expires_at`     DATE,
                                                `created_at`     DATETIME(6),
                                                PRIMARY KEY (`id`),
                                                CONSTRAINT `fk_batch_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
                                                CONSTRAINT `fk_batch_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`)
) ENGINE=InnoDB;

-- Bảng carbon_credits (Tín chỉ Carbon)
CREATE TABLE IF NOT EXISTS `carbon_credits` (
                                                `id`               BIGINT NOT NULL AUTO_INCREMENT,
                                                `credit_code`      VARCHAR(64) NOT NULL UNIQUE,
                                                `name`             VARCHAR(128) NOT NULL DEFAULT 'Carbon Credit',
                                                `status`           ENUM('AVAILABLE','EXPIRED','ISSUE','ISSUED','LISTED','PENDING','RETIRED','SOLD','TRADED') NOT NULL,
                                                `vintage_year`     INT NOT NULL,
                                                `t_co2e`           DECIMAL(18, 3) NOT NULL,
                                                `amount`           DECIMAL(18, 2), -- Số lượng tiền (nếu cần) hoặc giá trị
                                                `carbon_credit`    DECIMAL(18, 3) NOT NULL, -- Số lượng credit
                                                `listed_amount`    DECIMAL(18, 2),
                                                `current_price`    DOUBLE NOT NULL,
                                                `project_id`       BIGINT,
                                                `company_id`       BIGINT,
                                                `batch_id`         BIGINT,
                                                `source_credit_id` BIGINT, -- Để trace nguồn gốc nếu tách nhỏ
                                                `issued_by`        VARCHAR(100),
                                                `issued_at`        DATETIME(6),
                                                `expiry_date`      DATE,
                                                `created_at`       DATETIME(6) NOT NULL,
                                                `updated_at`       DATETIME(6) NOT NULL,
                                                PRIMARY KEY (`id`),
                                                CONSTRAINT `fk_credit_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
                                                CONSTRAINT `fk_credit_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`),
                                                CONSTRAINT `fk_credit_batch` FOREIGN KEY (`batch_id`) REFERENCES `credit_batches` (`id`),
                                                CONSTRAINT `fk_credit_source` FOREIGN KEY (`source_credit_id`) REFERENCES `carbon_credits` (`id`)
) ENGINE=InnoDB;

-- =======================================================
-- 3. MODULE: MARKETPLACE & TRADING
-- =======================================================

-- Bảng marketplace_listings (Tin đăng bán)
CREATE TABLE IF NOT EXISTS `marketplace_listings` (
                                                      `id`                BIGINT NOT NULL AUTO_INCREMENT,
                                                      `idempotency_key`   VARCHAR(255),
                                                      `company_id`        BIGINT NOT NULL,
                                                      `carbon_credits_id` BIGINT NOT NULL,
                                                      `quantity`          DECIMAL(18, 4) NOT NULL,
                                                      `original_quantity` DECIMAL(18, 4) NOT NULL DEFAULT 0,
                                                      `sold_quantity`     DECIMAL(18, 4) NOT NULL DEFAULT 0,
                                                      `price_per_credit`  DECIMAL(18, 2) NOT NULL,
                                                      `status`            ENUM('AVAILABLE','CANCELLED','EXPIRED','LISTED','SOLD') NOT NULL,
                                                      `expires_at`        DATE NOT NULL,
                                                      `created_at`        DATETIME(6),
                                                      PRIMARY KEY (`id`),
                                                      CONSTRAINT `fk_listing_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`),
                                                      CONSTRAINT `fk_listing_credit` FOREIGN KEY (`carbon_credits_id`) REFERENCES `carbon_credits` (`id`)
) ENGINE=InnoDB;

-- Bảng orders (Đơn hàng)
CREATE TABLE IF NOT EXISTS `orders` (
                                        `id`               BIGINT NOT NULL AUTO_INCREMENT,
                                        `listing_id`       BIGINT NOT NULL,
                                        `company_id`       BIGINT NOT NULL, -- Người mua/bán
                                        `carbon_credit_id` BIGINT NOT NULL,
                                        `order_type`       ENUM('BUY','SELL') NOT NULL,
                                        `order_status`     ENUM('CANCELLED','ERROR','FINISHED','PARTIALLY_FILLED','PENDING','SUCCESS') NOT NULL,
                                        `quantity`         DECIMAL(18, 4) NOT NULL,
                                        `unit_price`       DECIMAL(18, 2) NOT NULL,
                                        `total_price`      DECIMAL(18, 2) NOT NULL,
                                        `platform_fee`     DECIMAL(18, 2) NOT NULL,
                                        `seller_payout`    DECIMAL(18, 2) NOT NULL,
                                        `created_at`       DATETIME(6),
                                        `completed_at`     DATETIME(6),
                                        PRIMARY KEY (`id`),
                                        CONSTRAINT `fk_orders_listing` FOREIGN KEY (`listing_id`) REFERENCES `marketplace_listings` (`id`),
                                        CONSTRAINT `fk_orders_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`)
) ENGINE=InnoDB;

-- =======================================================
-- 4. MODULE: FINANCE & WALLET
-- =======================================================

-- Bảng wallets (Ví tiền & Tín chỉ)
CREATE TABLE IF NOT EXISTS `wallets` (
                                         `id`                    BIGINT NOT NULL AUTO_INCREMENT,
                                         `user_id`               BIGINT NOT NULL,
                                         `company_id`            BIGINT,
                                         `carbon_credit_id`      BIGINT, -- Có thể null nếu ví này giữ tiền chung
                                         `balance`               DECIMAL(38, 2) DEFAULT 0,
                                         `carbon_credit_balance` DECIMAL(18, 4) DEFAULT 0,
                                         PRIMARY KEY (`id`),
                                         CONSTRAINT `fk_wallets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                                         CONSTRAINT `fk_wallets_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`)
) ENGINE=InnoDB;

-- Bảng payment_details (Thông tin ngân hàng rút tiền)
CREATE TABLE IF NOT EXISTS `payment_details` (
                                                 `id`                  BIGINT NOT NULL AUTO_INCREMENT,
                                                 `user_id`             BIGINT,
                                                 `bank_code`           VARCHAR(20) NOT NULL,
                                                 `account_number`      VARCHAR(255) NOT NULL,
                                                 `account_holder_name` VARCHAR(100) NOT NULL,
                                                 `customer_name`       VARCHAR(255) NOT NULL,
                                                 PRIMARY KEY (`id`),
                                                 CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

-- Bảng payment_order (Giao dịch nạp tiền qua cổng thanh toán)
CREATE TABLE IF NOT EXISTS `payment_order` (
                                               `id`               BIGINT NOT NULL AUTO_INCREMENT,
                                               `user_id`          BIGINT,
                                               `amount`           BIGINT,
                                               `payment_method`   ENUM('PAYPAL','STRIPE','VNPAY'),
                                               `status`           TINYINT,
                                               `vnp_txn_ref`      VARCHAR(255),
                                               `created_at`       DATETIME(6) NOT NULL,
                                               `updated_at`       DATETIME(6) NOT NULL,
                                               PRIMARY KEY (`id`),
                                               CONSTRAINT `fk_payment_order_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

-- Bảng wallet_transaction (Lịch sử biến động số dư)
CREATE TABLE IF NOT EXISTS `wallet_transaction` (
                                                    `id`               BIGINT NOT NULL AUTO_INCREMENT,
                                                    `wallet_id`        BIGINT NOT NULL,
                                                    `transaction_type` ENUM('ADD_MONEY','BUY_CARBON_CREDIT','EV_OWNER_PAYOUT','EV_OWNER_PAYOUT_RECEIVE','ISSUE_CREDIT','PROFIT_SHARING','RETIRE_CREDIT','SELL_CARBON_CREDIT','SELL_CARBON_CREDIT_SHARE','WITHDRAWAL') NOT NULL,
                                                    `amount`           DECIMAL(18, 2) NOT NULL,
                                                    `balance_before`   DECIMAL(18, 2),
                                                    `balance_after`    DECIMAL(18, 2),
                                                    `description`      VARCHAR(500),
                                                    `order_id`         BIGINT,
                                                    `payment_order_id` BIGINT,
                                                    `credit_batch_id`  BIGINT,
                                                    `distribution_id`  BIGINT,
                                                    `created_at`       DATETIME(6),
                                                    PRIMARY KEY (`id`),
                                                    CONSTRAINT `fk_tx_wallet` FOREIGN KEY (`wallet_id`) REFERENCES `wallets` (`id`)
) ENGINE=InnoDB;

-- Bảng withdrawal (Yêu cầu rút tiền)
CREATE TABLE IF NOT EXISTS `withdrawal` (
                                            `id`                 BIGINT NOT NULL AUTO_INCREMENT,
                                            `user_id`            BIGINT,
                                            `payment_details_id` BIGINT,
                                            `amount`             DECIMAL(38, 2),
                                            `status`             ENUM('APPROVED','FAILED','PENDING','REJECTED','SUCCEEDED'),
                                            `requested_at`       DATETIME(6),
                                            `processed_at`       DATETIME(6),
                                            PRIMARY KEY (`id`),
                                            CONSTRAINT `fk_widthdraw_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
                                            CONSTRAINT `fk_widthdraw_detail` FOREIGN KEY (`payment_details_id`) REFERENCES `payment_details` (`id`)
) ENGINE=InnoDB;

-- Bảng profit_distribution (Chia sẻ lợi nhuận cho EV Owner)
CREATE TABLE IF NOT EXISTS `profit_distribution` (
                                                     `id`                        BIGINT NOT NULL AUTO_INCREMENT,
                                                     `project_id`                BIGINT,
                                                     `company_user_id`           BIGINT NOT NULL,
                                                     `total_credits_distributed` DECIMAL(38, 2) NOT NULL,
                                                     `total_money_distributed`   DECIMAL(38, 2) NOT NULL,
                                                     `status`                    ENUM('COMPLETED','FAILED','PENDING','PROCESSING') NOT NULL,
                                                     `created_at`                DATETIME(6) NOT NULL,
                                                     `updated_at`                DATETIME(6) NOT NULL,
                                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- Bảng profit_distribution_detail
CREATE TABLE IF NOT EXISTS `profit_distribution_detail` (
                                                            `id`              BIGINT NOT NULL AUTO_INCREMENT,
                                                            `distribution_id` BIGINT NOT NULL,
                                                            `ev_owner_id`     BIGINT NOT NULL, -- User ID của EV Owner
                                                            `credit_amount`   DECIMAL(38, 2) NOT NULL,
                                                            `money_amount`    DECIMAL(38, 2) NOT NULL,
                                                            `energy_amount`   DECIMAL(38, 2) NOT NULL,
                                                            `status`          VARCHAR(255) NOT NULL,
                                                            `error_message`   VARCHAR(255),
                                                            `created_at`      DATETIME(6) NOT NULL,
                                                            `updated_at`      DATETIME(6) NOT NULL,
                                                            PRIMARY KEY (`id`),
                                                            CONSTRAINT `fk_dist_detail_main` FOREIGN KEY (`distribution_id`) REFERENCES `profit_distribution` (`id`)
) ENGINE=InnoDB;

-- Bảng credit_certificates (Chứng chỉ)
CREATE TABLE IF NOT EXISTS `credit_certificates` (
                                                     `id`               BIGINT NOT NULL AUTO_INCREMENT,
                                                     `batch_id`         BIGINT NOT NULL,
                                                     `certificate_code` VARCHAR(128) NOT NULL,
                                                     `issued_to`        VARCHAR(256),
                                                     `issued_email`     VARCHAR(256),
                                                     `standard`         VARCHAR(256),
                                                     `registry`         VARCHAR(256),
                                                     `methodology`      VARCHAR(256),
                                                     `certificate_url`  VARCHAR(1024),
                                                     `pdf_url`          VARCHAR(2048),
                                                     `qr_code_url`      VARCHAR(1024),
                                                     `issued_at`        DATETIME(6),
                                                     PRIMARY KEY (`id`),
                                                     CONSTRAINT `fk_cert_batch` FOREIGN KEY (`batch_id`) REFERENCES `credit_batches` (`id`)
) ENGINE=InnoDB;

-- Bật lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 1;