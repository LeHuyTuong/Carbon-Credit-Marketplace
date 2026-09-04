# 22 — Database Schema Audit

> Evidence: `backend/database/core_ccm.sql` (hand-written reference DDL, db `core_ccm3`) vs JPA entities vs `ddl-auto=update` settings vs repository query assumptions. Actual production schema: UNKNOWN (no access) — flagged where it matters.

## Schema Management Reality

- All profiles set `ddl-auto=update` → Hibernate mutates schema at boot; `core_ccm.sql` is a manually maintained reference, not enforced anywhere.
- Consequence: three sources of truth (entities, dump, live DB), none authoritative. Drift examples below are therefore *confirmed between two of the three*; live-DB drift is UNKNOWN.

## Drift Findings (dump vs entities)

| Item | core_ccm.sql | Entities | Impact |
|---|---|---|---|
| Table name | `payment_order` (`:346`) | `PaymentOrder` → `payment_order`? naming strategy default would give `payment_order` — consistent IF default strategy; `WalletTransaction` vs dump table `wallet_transaction` — matches; `MarketPlaceListing` → dump `marketplace_listings` matches | LOW |
| `marketplace_listings.idempotency_key` | **absent from dump** (`:279-296` block shows no such column) | present (`MarketPlaceListing.idempotencyKey`) | Confirms dead field (never used); dump drift |
| `orders.order_status` enum | `ENUM('CANCELLED','ERROR','FINISHED','PARTIALLY_FILLED','PENDING','SUCCESS')` | `OrderStatus` has same 6 values — but **FINISHED and PARTIALLY_FILLED are dead** (code only writes PENDING/SUCCESS/CANCELLED/ERROR) | Enum pollution; migration to VARCHAR recommended with Flyway |
| `wallets.balance` | `DECIMAL(38,2)` | `BigDecimal` no precision → Hibernate default (38,2) | Consistent by luck |
| `carbon_credits.current_price` | (in dump as double presumably) | `double currentPrice` (`CarbonCredit.java:77`) | **Floating-point money-adjacent field** (N17) |
| FKs | present (orders→listings/companies, wallets→users/company) | JPA relations match | OK |
| Secondary indexes | **none** beyond PK/FK in entire dump | repositories query by: `wallet_transaction.wallet_id ORDER BY created_at`, `marketplace_listings.status+expires_at`, `orders.company_id`, `carbon_credits.company_id`, `payment_order.vnp_txn_ref`, `withdrawal.user_id` | **All unindexed in reference schema** (N13); live DB UNKNOWN — must `EXPLAIN` before assuming |
| Uniqueness | `users.email` unique not visible in dump block read (UNKNOWN); `carbon_credits.credit_code` unique in entity | users duplicate-email protection is app-level (`findByEmail != null` check, `AuthServiceImpl:51`) | Race on concurrent registration possible if no DB unique |
| Timestamps | `DATETIME(6)` w/ timezone-less semantics; app writes `LocalDateTime.now(VIETNAM_ZONE)` in some places, `OffsetDateTime`/`CreationTimestamp` elsewhere | mixed time handling | Medium: consistent UTC policy needed |

## Money-Type Audit

| Field | Type | Verdict |
|---|---|---|
| wallet.balance / carbon_credit_balance | DECIMAL + BigDecimal | OK |
| order quantity/unitPrice/totalPrice/platformFee/sellerPayout | DECIMAL + BigDecimal | OK |
| `PaymentOrderRequest.amount` | **primitive double** (`PaymentServiceImpl:85,158,182` — `request.getAmount() * 100`) | Violation — money as float; Stripe amount computed from double; DB stores DECIMAL but conversion path is lossy |
| `CarbonCredit.currentPrice` | `double` | Violation (display price, still money-adjacent) |
| VNPay amount | Long VND ×100 string | OK (integer cents-equivalent) |

## Missing Constraints (recommended with Flyway baseline)

1. `CHECK (balance >= 0)` on wallets — DB-level backstop for I1.
2. `CHECK (quantity >= 0)`, `CHECK (sold_quantity >= 0)` on listings; `CHECK (amount >= 0)` on carbon_credits — backstop I8.
3. UNIQUE `(payment_order_id)` on a new `wallet_transaction.payment_order_id` column — backstop I3 (single credit per payment), replacing free-text description linkage.
4. UNIQUE `users.email` if absent.
5. Indexes: the six listed above; verify with EXPLAIN under seed dataset (12).

## Schema Risk Matrix

| Risk | Likelihood | Impact | Notes |
|---|---|---|---|
| Live schema drift from entities (update-only never drops/renames) | High (certain over time) | Medium | Old columns linger; dump compounding |
| Missing indexes at production scale | Medium (UNKNOWN live) | Medium-High | Wallet view does full scans of history |
| No rollback for schema changes | Certain today | High | No migrations = no down-script |
| Enum column vs Java enum divergence on future edits | Medium | Medium | MySQL strict mode would fail inserts |
| Floating-point amount corruption (PaymentOrderRequest double) | Low per-occurrence | Medium cumulative | cent-level drift into Stripe calls |
| Concurrent-registration duplicate emails | Low | Medium | app-level check race |

## Flyway Note (per audit instructions: understand before prescribing)

Adopting Flyway requires the **live prod schema** as baseline (not `core_ccm.sql`, which is already stale). Procedure: `mysqldump --no-data` prod → `V1__baseline.sql` → entity diff review → `validate` mode. The dump in-repo is *not* the baseline; treating it as such would recreate drift. Status of live schema: UNKNOWN — requires operator action.
