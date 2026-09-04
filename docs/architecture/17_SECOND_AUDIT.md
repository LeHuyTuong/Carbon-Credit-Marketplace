# 17 — Second Audit (Audit of the Audit)

> Adversarial re-audit, 2026-08-16. Every P0/P1 claim from the first audit was re-verified against source; new areas (withdrawal, registration, KYC admin creation, schema dump, deployment files, prod properties) were read for the first time. No application source was modified.

## 1. Re-Verification of First-Audit Findings

| # | First-audit finding | Independent verification | Verdict |
|---|---|---|---|
| 1 | Free-money deposit endpoint (`paymentConfirmed = true`) | `PaymentServiceImpl.java:126` re-read; exploit paths re-derived | **CONFIRMED** (Critical) |
| 2 | `transferFunds` writes sender balances into receiver row | `WalletServiceImpl.java:379-380` — `toTransaction` uses `fromBefore/fromAfter` | **CONFIRMED** (Critical) |
| 3 | Secrets in `docker-compose.yml` (Gmail app password, MySQL root) | Re-read; **understated** — `application-prod.properties` is also committed with **non-empty** DB password, mail password, AWS secret key, Stripe key, PayPal secret | **PARTIALLY CONFIRMED → worse than reported** |
| 4 | `anyRequest().permitAll()` + FileUploadTestController | `AppConfig.java:81`; controller exists at `/api/test/upload1e` | **CONFIRMED** |
| 5 | Buyer wallet unlocked in settlement | `OrderServiceImpl:214-227` + `WalletTransactionServiceImpl:47-52` | **CONFIRMED** |
| 6 | Order ERROR status lost to rollback | `OrderServiceImpl:319-324` | **CONFIRMED** |
| 7 | Fractional credits "paid but truncated via intValue()" | **Counter-evidence**: `CreditIssuanceServiceImpl.issueTradeCredit:588` uses `quantity.intValueExact()` which **throws** on fractional quantity → settlement fails and rolls back. There is no silent truncation of money; the real defects are (a) fractional orders accepted at creation then failing at settlement with a confusing error, and (b) the ERROR-status/rollback bug masking it | **OVERSTATED → corrected** (see §3-C1) |
| 8 | `getOrderById` IDOR | `OrderServiceImpl:126-138` — no ownership check (contrast `cancelOrder:165`) | **CONFIRMED** |
| 9 | jakarta vs Spring `@Transactional` 16/7 split | grep re-run | **CONFIRMED** |
| 10 | 500 handler leaks `ex.getMessage()` | `GlobalExceptionHandler:118-124` | **CONFIRMED** |
| 11 | CORS wildcard entries dead config | `AppConfig:132-139` uses `setAllowedOrigins` (exact match) with pattern strings | **CONFIRMED** (Low) |
| 12 | MapStruct dead, Redis OTP-only, idempotencyKey unused, 26 tests | Re-verified | **CONFIRMED** |
| 13 | First audit implied UserController admin endpoints unprotected | False — `UserController` has `@PreAuthorize("hasRole('ADMIN')")` on `/users`, `/users/{id}`, `/users/count`, `PATCH /users/{id}/status` (lines 73-220) | **INCORRECT in audit-1 working notes — corrected here** (the finding never reached the final docs; recorded for honesty) |

## 2. NEW FINDINGS (missed by the first audit)

| # | Finding | Severity | Evidence | Confidence |
|---|---|---|---|---|
| N1 | **Registration accepts any role**: `req.getRoleName()` looked up directly in `roles` table — no whitelist. `POST /api/v1/auth/register` is `permitAll` → anyone can self-register as ADMIN/CVA/COMPANY | **Critical** | `AuthServiceImpl:59-63` | HIGH |
| N2 | **Withdrawal admin actions unauthenticated-as-admin**: `PATCH /api/v1/withdrawal/admin/{id}/process/{accept}` and `GET /api/v1/withdrawal/admin` have no `@PreAuthorize` and don't match `/api/admin/**` → any authenticated user approves/rejects withdrawals | **Critical** | `WithdrawalController:87-104,116-131`; no checks in `WithdrawalServiceImpl.processWithdrawal` | HIGH |
| N3 | **Withdrawal reject-refund is replayable**: rejection branch refunds `wallet.setBalance(+amount)` without checking the withdrawal's current status → repeated `process(false)` calls mint balance indefinitely; refund also writes **no ledger record** | **Critical** | `WithdrawalServiceImpl:125-130` | HIGH |
| N4 | **JWT signing secret hardcoded and committed** (`JwtConstant.SECRET_KEY`, 70 chars) — repo is public → anyone can forge valid tokens for any email/role | **Critical** | `config/JwtConstant.java:4`; `JwtProvider.java:25,41` | HIGH |
| N5 | **Production secrets committed in `application-prod.properties`** (DB password, mail password, AWS secretAccessKey, Stripe key, PayPal secret — all non-empty) | **Critical** | file read (values masked in docs) | HIGH |
| N6 | **Double-settlement race on the same order**: `completeOrder` checks `SUCCESS` *before* acquiring the listing lock and never locks the Order row → two concurrent calls with the same orderId can both proceed to full settlement (double debit/credit/issuance) | High | `OrderServiceImpl:182-193` (status check precedes lock; no `findByIdWithLock` for Order) | MEDIUM-HIGH (needs runtime confirmation, but code order is definitive) |
| N7 | **Any COMPANY can complete another company's order**: `completeOrder` has no caller-vs-buyer check (only role COMPANY) → third party can force-settle someone's PENDING order | High | `OrderServiceImpl:176-183`; `OrderController:32,50` | HIGH |
| N8 | **KYC admin/CVA profile creation without role checks**: any authenticated user can create `Admin`/`Cva` profile records for themselves | High | `KycServiceImpl:320-346,235-258` (only "already exists" checks); endpoints `KycController:201,268+` | HIGH |
| N9 | **CVA dashboard has no role checks**: `/api/cva/dashboard/**` (cards, companies, reports, credits) — authenticated-only, any role | Medium-High | `CvaDashboardController` — zero `@PreAuthorize`/ownership (grep) | HIGH |
| N10 | **`/v1/ai/chat` duplicate route bypasses rate limiting** (`WebConfig` interceptor covers `/api/**` only); endpoint still role-guarded by `@PreAuthorize` but unlimited Gemini cost for COMPANY accounts | Medium | `ChatAiController:15`; `WebConfig:20-24`; `AppConfig:75-81` | HIGH |
| N11 | **Platform fee is phantom**: `platformFee` stored (`trading_fee=0.05`) but never deducted anywhere — buyer debit = 100% seller credit; profit sharing pays owners from separate wallet debit at market price × pct. Fee invariant unverifiable by design | Medium (intent UNKNOWN) | `OrderServiceImpl:100-112,328-361`; `ProfitSharingServiceImpl:139-146` | MEDIUM |
| N12 | **Withdrawal debit orchestrated in controller** (entity fetch + transaction creation in `WithdrawalController:53-68`) — layering violation on a money path; also approval path never re-checks balance (check commented out) | Medium | `WithdrawalController:53-68`; `WithdrawalServiceImpl:96-99` | HIGH |
| N13 | **No DB indexes beyond PK/FK in reference schema**: `wallet_transaction(wallet_id, created_at)` (queried per wallet view), `marketplace_listings(status, expires_at)`, `orders(company_id)`, `carbon_credits(company_id)` all unindexed | Medium | `backend/database/core_ccm.sql` (CREATE TABLE blocks; no secondary indexes) | MEDIUM (prod schema UNKNOWN) |
| N14 | **Three CSV stacks and two PDF stacks** in pom (jackson-dataformat-csv ×2 + commons-csv; POI; flying-saucer + openhtmltopdf) | Low | `pom.xml:152-278` | HIGH |
| N15 | **Docker build skips tests** (`-DskipTests -Dmaven.test.skip=true`), runs as root, no healthcheck/JVM flags | Medium | `backend/Market_carbon/Dockerfile` | HIGH |
| N16 | **docker-compose is not a working reference deployment**: nginx conf has no `/api` proxy, and FE build arg uses CRA-style `REACT_APP_API_BASE_URL` (project is Vite/VITE_*) pointing at `http://be:8082` (container-internal hostname unusable from browsers) | Medium | `docker-compose.yml` fe service; `frontend/nginx.conf` | HIGH |
| N17 | **`CarbonCredit.currentPrice` is `double`** — floating-point money field on the entity | Medium | `CarbonCredit.java:76-77` | HIGH |
| N18 | **`users.by-email` lookup without role restriction** returns User entity to any authenticated user (enumeration + PII; passwordHash is `@JsonIgnore` but profile data exposed) | Low-Medium | `UserController:55-68`; `User.java:28-29` | HIGH |

## 3. AUDIT CORRECTIONS (things the first audit overstated or got subtly wrong)

| # | Correction |
|---|---|
| C1 | Fractional-quantity handling: audit-1 claimed silent truncation + money loss. Actually `intValueExact()` throws → loud failure + rollback. Real issues: acceptance of fractional quantity at creation, confusing failure, and the S2 rollback bug. Severity: Medium (was Medium-High) |
| C2 | Package-by-feature migration (audit-1 Phase 4): demoted from a roadmap phase to a deferred, optional move. None of the P0/P1 problems require changing the package axis; hardening package-by-layer with ArchUnit achieves the enforceable wins at ~zero migration risk (full analysis in 28) |
| C3 | `utils/Tuong` "personal package" was flagged as debt; second read confirms it is just an envelope variant — the actual problem is envelope duplication, not the name (name is cosmetic) |
| C4 | Audit-1's risk register assumed deposit replay was blocked only by status; in fact `processPaymentOrder` DOES gate crediting on PENDING→SUCCEEDED (replay of the same call returns false). The unverified-confirmation + ownership holes remain Critical; "credit twice via simple replay" is NOT one of them (a concurrent race remains possible since status check and wallet credit are separate transactions) |
| C5 | Audit-1 listed SSE emitters as a possible leak concern — `SseServiceImpl` properly removes emitters on completion/timeout/error (lines 55-57). Cleared |

## 4. Aggregate Effect on Priorities

The first audit's P0 set (deposit verification, transferFunds, secrets) is confirmed and now **joined** by five new blockers: role-whitelist registration (N1), withdrawal admin escalation + replayable refund (N2/N3), committed JWT secret (N4), and committed prod credentials (N5). Correctness work must precede all structural work (28/29 reflect this).
