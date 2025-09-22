-- Schema: carbon_market
CREATE DATABASE IF NOT EXISTS carbon_market
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE carbon_market;

-- 1) Người dùng & KYC
CREATE TABLE users (
                       id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name     VARCHAR(255) NOT NULL,
                       role          ENUM('EV_OWNER','CC_BUYER','CVA','ADMIN') NOT NULL,
                       status        ENUM('ACTIVE','SUSPENDED') DEFAULT 'ACTIVE',
                       created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE user_profiles (
                               user_id    BIGINT UNSIGNED PRIMARY KEY,
                               phone      VARCHAR(30),
                               country    VARCHAR(100),
                               kyc_status ENUM('NONE','PENDING','VERIFIED','REJECTED') DEFAULT 'NONE',
                               FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE kyc_documents (
                               id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                               user_id      BIGINT UNSIGNED NOT NULL,
                               doc_type     ENUM('ID','DRIVER_LICENSE','UTILITY_BILL','BUSINESS_CERT') NOT NULL,
                               file_url     VARCHAR(500) NOT NULL,
                               status       ENUM('SUBMITTED','APPROVED','REJECTED') DEFAULT 'SUBMITTED',
                               submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               reviewed_at  DATETIME NULL,
                               reviewed_by  BIGINT UNSIGNED NULL,
                               notes        TEXT,
                               FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
                               FOREIGN KEY (reviewed_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- 2) Xe điện & dữ liệu hoạt động
CREATE TABLE evs (
                     id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                     owner_id     BIGINT UNSIGNED NOT NULL,
                     vin          VARCHAR(64) UNIQUE,
                     make         VARCHAR(100),
                     model        VARCHAR(100),
                     model_year   SMALLINT,
                     battery_kwh  DECIMAL(10,2),
                     created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                     FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
                     INDEX (owner_id)
) ENGINE=InnoDB;

CREATE TABLE charging_sessions (
                                   id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                   ev_id            BIGINT UNSIGNED NOT NULL,
                                   start_time       DATETIME NOT NULL,
                                   end_time         DATETIME NOT NULL,
                                   energy_kwh       DECIMAL(12,3) NOT NULL,
                                   grid_ef_g_per_kwh DECIMAL(10,2) NULL,    -- hệ số phát thải lưới (gCO2/kWh)
                                   renewable_pct    DECIMAL(5,2) NULL,      -- % năng lượng tái tạo (nếu có)
                                   location         VARCHAR(255),
                                   FOREIGN KEY (ev_id) REFERENCES evs(id) ON DELETE CASCADE,
                                   INDEX (ev_id, start_time)
) ENGINE=InnoDB;

CREATE TABLE trips (
                       id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                       ev_id        BIGINT UNSIGNED NOT NULL,
                       start_time   DATETIME NOT NULL,
                       end_time     DATETIME NOT NULL,
                       distance_km  DECIMAL(12,3),
                       energy_kwh   DECIMAL(12,3),
                       origin       VARCHAR(255),
                       destination  VARCHAR(255),
                       FOREIGN KEY (ev_id) REFERENCES evs(id) ON DELETE CASCADE,
                       INDEX (ev_id, start_time)
) ENGINE=InnoDB;

-- 3) Tính toán & xác minh
CREATE TABLE emission_calculations (
                                       id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                       ev_id              BIGINT UNSIGNED NOT NULL,
                                       period_start       DATE NOT NULL,
                                       period_end         DATE NOT NULL,
                                       method             ENUM('SIMPLE_BASELINE','GRID_FACTOR','OTHER') NOT NULL,
                                       baseline_kgco2e    DECIMAL(18,6) NOT NULL,
                                       actual_kgco2e      DECIMAL(18,6) NOT NULL,
                                       reduction_kgco2e   DECIMAL(18,6) NOT NULL,
                                       status             ENUM('DRAFT','READY','SUBMITTED','VERIFIED','REJECTED') DEFAULT 'DRAFT',
                                       created_by         BIGINT UNSIGNED NOT NULL,
                                       created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       UNIQUE KEY uq_calc (ev_id, period_start, period_end),
                                       FOREIGN KEY (ev_id) REFERENCES evs(id) ON DELETE CASCADE,
                                       FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE verification_requests (
                                       id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                       owner_id        BIGINT UNSIGNED NOT NULL,
                                       calc_id         BIGINT UNSIGNED NOT NULL,
                                       status          ENUM('SUBMITTED','REVIEWING','APPROVED','REJECTED') DEFAULT 'SUBMITTED',
                                       assigned_cva_id BIGINT UNSIGNED NULL,
                                       submitted_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       decided_at      DATETIME NULL,
                                       notes           TEXT,
                                       FOREIGN KEY (owner_id)        REFERENCES users(id) ON DELETE CASCADE,
                                       FOREIGN KEY (calc_id)         REFERENCES emission_calculations(id) ON DELETE CASCADE,
                                       FOREIGN KEY (assigned_cva_id) REFERENCES users(id),
                                       UNIQUE KEY uq_req (calc_id)
) ENGINE=InnoDB;

CREATE TABLE verification_evidence (
                                       id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                       verification_id  BIGINT UNSIGNED NOT NULL,
                                       type             ENUM('TRIP_LOG','CHARGE_LOG','IMAGE','INVOICE','OTHER') NOT NULL,
                                       url              VARCHAR(500) NOT NULL,
                                       description      VARCHAR(500),
                                       uploaded_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (verification_id) REFERENCES verification_requests(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4) Tín chỉ & nắm giữ
CREATE TABLE carbon_certificates (
                                     id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                     owner_id       BIGINT UNSIGNED NOT NULL,     -- chủ sở hữu ban đầu
                                     verification_id BIGINT UNSIGNED NOT NULL,
                                     serial_no      VARCHAR(100) NOT NULL UNIQUE,
                                     vintage_year   SMALLINT NOT NULL,
                                     total_tco2e    DECIMAL(12,4) NOT NULL,       -- tổng phát hành (tCO2e)
                                     available_tco2e DECIMAL(12,4) NOT NULL,      -- còn có thể bán/chuyển nhượng
                                     status         ENUM('MINTED','LISTED','LOCKED','SOLD_OUT','RETIRED','CANCELLED') DEFAULT 'MINTED',
                                     issued_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     retired_at     DATETIME NULL,
                                     FOREIGN KEY (owner_id)        REFERENCES users(id),
                                     FOREIGN KEY (verification_id) REFERENCES verification_requests(id),
                                     INDEX (owner_id)
) ENGINE=InnoDB;

CREATE TABLE holdings (
                          id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                          user_id         BIGINT UNSIGNED NOT NULL,
                          certificate_id  BIGINT UNSIGNED NOT NULL,
                          amount_tco2e    DECIMAL(12,4) NOT NULL,
                          UNIQUE KEY uq_holding (user_id, certificate_id),
                          FOREIGN KEY (user_id)        REFERENCES users(id) ON DELETE CASCADE,
                          FOREIGN KEY (certificate_id) REFERENCES carbon_certificates(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE retirements (
                             id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                             user_id         BIGINT UNSIGNED NOT NULL,
                             certificate_id  BIGINT UNSIGNED NOT NULL,
                             amount_tco2e    DECIMAL(12,4) NOT NULL,
                             reason          VARCHAR(255),
                             retired_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             FOREIGN KEY (user_id)        REFERENCES users(id),
                             FOREIGN KEY (certificate_id) REFERENCES carbon_certificates(id)
) ENGINE=InnoDB;

-- 5) Thị trường (niêm yết, đặt mua, đấu giá, khớp lệnh)
CREATE TABLE marketplace_listings (
                                      id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                      certificate_id   BIGINT UNSIGNED NOT NULL,
                                      seller_id        BIGINT UNSIGNED NOT NULL,
                                      type             ENUM('FIXED','AUCTION') NOT NULL,
                                      quantity_tco2e   DECIMAL(12,4) NOT NULL,
                                      unit_price_usd   DECIMAL(18,6) NULL,        -- dùng với FIXED
                                      status           ENUM('ACTIVE','CANCELLED','FILLED','EXPIRED') DEFAULT 'ACTIVE',
                                      created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      expires_at       DATETIME NULL,
                                      FOREIGN KEY (certificate_id) REFERENCES carbon_certificates(id),
                                      FOREIGN KEY (seller_id)      REFERENCES users(id),
                                      INDEX (type, status),
                                      INDEX (seller_id)
) ENGINE=InnoDB;

CREATE TABLE orders (
                        id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                        listing_id       BIGINT UNSIGNED NOT NULL,
                        buyer_id         BIGINT UNSIGNED NOT NULL,
                        quantity_tco2e   DECIMAL(12,4) NOT NULL,
                        unit_price_usd   DECIMAL(18,6) NOT NULL,
                        status           ENUM('PENDING','PAID','CANCELLED','FILLED') DEFAULT 'PENDING',
                        created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (listing_id) REFERENCES marketplace_listings(id),
                        FOREIGN KEY (buyer_id)   REFERENCES users(id),
                        INDEX (buyer_id, status)
) ENGINE=InnoDB;

CREATE TABLE trades (
                        id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                        order_id         BIGINT UNSIGNED NULL,
                        listing_id       BIGINT UNSIGNED NOT NULL,
                        buyer_id         BIGINT UNSIGNED NOT NULL,
                        seller_id        BIGINT UNSIGNED NOT NULL,
                        certificate_id   BIGINT UNSIGNED NOT NULL,
                        quantity_tco2e   DECIMAL(12,4) NOT NULL,
                        price_total_usd  DECIMAL(18,6) NOT NULL,
                        fee_usd          DECIMAL(18,6) NOT NULL DEFAULT 0,
                        settled_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (order_id)       REFERENCES orders(id),
                        FOREIGN KEY (listing_id)     REFERENCES marketplace_listings(id),
                        FOREIGN KEY (buyer_id)       REFERENCES users(id),
                        FOREIGN KEY (seller_id)      REFERENCES users(id),
                        FOREIGN KEY (certificate_id) REFERENCES carbon_certificates(id),
                        INDEX (buyer_id), INDEX (seller_id)
) ENGINE=InnoDB;

-- Đấu giá
CREATE TABLE auctions (
                          id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                          listing_id       BIGINT UNSIGNED NOT NULL UNIQUE,
                          start_price_usd  DECIMAL(18,6) NOT NULL,
                          reserve_price_usd DECIMAL(18,6) NULL,
                          ends_at          DATETIME NOT NULL,
                          status           ENUM('OPEN','CLOSED','CANCELLED') DEFAULT 'OPEN',
                          FOREIGN KEY (listing_id) REFERENCES marketplace_listings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE bids (
                      id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                      auction_id  BIGINT UNSIGNED NOT NULL,
                      bidder_id   BIGINT UNSIGNED NOT NULL,
                      bid_price_usd DECIMAL(18,6) NOT NULL,
                      quantity_tco2e DECIMAL(12,4) NOT NULL,
                      created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                      FOREIGN KEY (bidder_id)  REFERENCES users(id),
                      INDEX (auction_id), INDEX (bidder_id)
) ENGINE=InnoDB;

-- 6) Ví & giao dịch tiền
CREATE TABLE wallet_accounts (
                                 id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                 user_id     BIGINT UNSIGNED NOT NULL,
                                 currency    CHAR(3) NOT NULL DEFAULT 'USD',
                                 balance_cached DECIMAL(18,6) NOT NULL DEFAULT 0,  -- có thể tính từ ledger
                                 UNIQUE KEY uq_wallet (user_id, currency),
                                 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE wallet_ledger (
                               id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                               account_id  BIGINT UNSIGNED NOT NULL,
                               entry_type  ENUM('DEPOSIT','WITHDRAWAL','TRADE_SETTLEMENT','FEE','REFUND') NOT NULL,
                               amount_usd  DECIMAL(18,6) NOT NULL, -- dương: cộng, âm: trừ
                               ref_type    ENUM('ORDER','TRADE','LISTING','WITHDRAWAL','DEPOSIT') NULL,
                               ref_id      BIGINT UNSIGNED NULL,
                               created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (account_id) REFERENCES wallet_accounts(id) ON DELETE CASCADE,
                               INDEX (account_id, created_at)
) ENGINE=InnoDB;

-- 7) Quản trị hệ thống
CREATE TABLE disputes (
                          id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                          raised_by     BIGINT UNSIGNED NOT NULL,
                          against_user  BIGINT UNSIGNED NULL,
                          listing_id    BIGINT UNSIGNED NULL,
                          trade_id      BIGINT UNSIGNED NULL,
                          status        ENUM('OPEN','UNDER_REVIEW','RESOLVED','REJECTED') DEFAULT 'OPEN',
                          reason        VARCHAR(255),
                          detail        TEXT,
                          created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          resolved_at   DATETIME NULL,
                          FOREIGN KEY (raised_by)    REFERENCES users(id),
                          FOREIGN KEY (against_user) REFERENCES users(id),
                          FOREIGN KEY (listing_id)   REFERENCES marketplace_listings(id),
                          FOREIGN KEY (trade_id)     REFERENCES trades(id)
) ENGINE=InnoDB;

CREATE TABLE notifications (
                               id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                               user_id    BIGINT UNSIGNED NOT NULL,
                               type       ENUM('SYSTEM','ORDER','BID','VERIFICATION','PAYMENT') NOT NULL,
                               content    VARCHAR(500) NOT NULL,
                               is_read    TINYINT(1) NOT NULL DEFAULT 0,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                               INDEX (user_id, is_read)
) ENGINE=InnoDB;


