# Architecture & Technical Debt Audit

> Repository: carbon-credit-marketplace (CarbonX) — branch `refactor/cleancode`
> Date: 2026-08-16
> Status: COMPLETED (audit phase — no source code was modified)

---

# Audit Progress

## Completed

- Repository discovery (structure, README, pom, configs, docker, git history)
- Full read of money-path code: Order, Wallet, WalletTransaction, Payment, VNPay, Marketplace services + controllers + repositories
- Read of models, exception handling, security config, schedulers, DataInitializer, tests
- Structural skim of all god-class candidates (GeminiAiScoring, CompanyPayoutQuery, MyCredit, ProfitSharing, EmissionReport)
- External best-practice research (Spring transactions, payment webhooks, modular monolith)
- This document (all 21 sections)

## In Progress

(none)

## Remaining

Implementation phases (see Section 15) — to be done only after this audit is reviewed.

## Next Step

Approve roadmap; start with Phase 0 (cleanup) + Phase 1 (critical money-path fixes).

---

# 1. Executive Summary

## Current State

CarbonX is a full-stack carbon-credit trading platform: a Spring Boot 3.3.3 / Java 17 monolith (~367 Java files, ~26K LOC in `backend/Market_carbon`), a React 18 + Vite frontend (~184 source files), MySQL 8, Redis (OTP only), and integrations with VNPay, Stripe, PayPal, AWS S3, and Google Gemini/Vertex AI. It is deployed publicly at `carbonx.io.vn` (per README) and has 762 commits of active history from a 4-person student team.

The backend is a classic **package-by-layer** layered architecture (`controller → service/impl → repository → model`) with DTO request/response separation. It works, it is feature-rich, and it shows real engineering awareness in places (pessimistic locking on trades, balance audit trails, rate limiting, signature verification for VNPay).

## Overall Assessment

**Architecture: acceptable for the product stage, but the money path has correctness and security defects that override any aesthetic refactoring.** The single most important finding is that wallet deposit confirmation trusts the client (`paymentConfirmed = true` hardcoded, no gateway verification, no ownership check) — any authenticated user can mint free wallet balance. Combined with a copy-paste bug that corrupts transaction history, a rollback pattern that loses order error states, an unlocked buyer wallet, and committed credentials, the top priority is **fixing correctness/security bugs the current architecture already knows how to express**, not changing the architecture.

## Biggest Strengths

- Concurrency-aware trade path: pessimistic locks on listing + credit, banking-style `transferFunds` with dual wallet locks and balance-before/after audit trail
- VNPay return-URL HMAC-SHA512 signature verification
- Coherent global exception handling with a consistent envelope for most errors
- Rate limiting (Bucket4j interceptor), JWT + OAuth2 login, BCrypt, method security enabled
- Clean DTO separation (85 request/response DTOs), `@Valid` used at controllers
- The current uncommitted N+1-optimization work (fetch-join repositories, `readOnly` transactions, migration to Spring `@Transactional`) is technically sound and in the right direction
- 26 unit tests (Mockito style) exist for 4 services, including the marketplace path

## Biggest Risks

1. **CRITICAL — free-money endpoint**: `POST /api/v1/wallet/deposit` credits the wallet based on a hardcoded success flag (evidence in §5.1)
2. **CRITICAL — corrupted financial records**: `transferFunds` writes the *source* wallet's balances into the *destination* wallet's transaction row (§5.2)
3. **CRITICAL — secrets committed**: real Gmail SMTP app password in `docker-compose.yml`; demo credentials hardcoded in `DataInitializer` (§6 Security Debt)
4. **HIGH — no payment gateway verification for Stripe/PayPal and no webhooks**; VNPay return handler never credits the wallet at all (§5.3)
5. **HIGH — schema via `ddl-auto=update`**, no migrations, no CI pipeline, 26 tests for 367 files, zero tests on wallet/payment/profit-sharing (§6 Testing Debt)

---

# 2. Repository Overview

## Technology Stack

| Area | Technology | Evidence |
|---|---|---|
| Language / framework | Java 17, Spring Boot 3.3.3 (dependencyManagement import, no parent POM) | `backend/Market_carbon/pom.xml:16-37` |
| Security | Spring Security, JWT via jjwt 0.11.5, Google OAuth2 client | `pom.xml:80-95,197-200`; `config/AppConfig.java`, `config/JwtProvider.java` |
| Persistence | Spring Data JPA / Hibernate, MySQL 8 | `pom.xml:50-53`; `application.properties` |
| Cache / store | Redis (used only by `OtpServiceImpl`), AWS S3 SDK 2.25.48 | grep: RedisTemplate only in `OtpServiceImpl` |
| Payments | VNPay (custom HMAC), Stripe java 24.9.0, PayPal `rest-api-sdk` 1.14.0 (legacy SDK) | `pom.xml:182-195`; `service/VNPayService.java` |
| AI | Google `google-genai` 1.24.0 **and** a parallel Vertex AI WebClient stack | `pom.xml:226-248`; `config/AiConfig.java`, `config/VertexWebClientConfig.java` |
| PDF / docs | PDFBox, flying-saucer, openhtmltopdf, POI, commons-csv | `pom.xml:152-278` |
| Mapping | MapStruct 1.5.5 configured in annotation processors, **both mapper `@Mapper` annotations are commented out** — dead dependency | `mapper/UserMapper.java:10`, `mapper/Admin_UserMapper.java:8` |
| Observability | logback-spring.xml (p6spy file appender), SLF4J `@Slf4j`; **no Actuator** | `resources/logback-spring.xml` |
| Frontend | React 18, Vite, MUI + React-Bootstrap + Redux Toolkit, axios, formik/yup | `frontend/package.json` |
| Build/ops | docker-compose (db + be + fe), nginx in frontend image; **no CI workflows** (no `.github/workflows`) | `docker-compose.yml` |

## Application Type

Modular-feature monolith (single deployable) + SPA frontend. Multi-role: Admin, EV Owner, Company, CVA (verification authority). Publicly deployed.

## Main Modules (logical domains found in code)

| Domain | Controllers | Services | Notes |
|---|---|---|---|
| Auth / User / KYC | `AuthController`, `UserController`, `KycController` (334 LOC) | `AuthServiceImpl` 184, `KycServiceImpl` 427, `UserServiceImplementation` 266 | OTP in Redis, OAuth2 Google |
| Wallet & transactions | `WalletController`, `WithdrawalController` | `WalletServiceImpl` 405, `WalletTransactionServiceImpl` 279, `WithdrawalServiceImpl` 169 | balance + carbon-credit balance |
| Payments | `PaymentController`, `VNPaymentController`, `PaymentDetailsController` | `PaymentServiceImpl` 254, `VNPayService` 125 | VNPay/Stripe/PayPal |
| Credit lifecycle | `CarbonCreditController`, `CreditIssuanceController`, `MyCreditController` | `CreditIssuanceServiceImpl` 694, `MyCreditServiceImpl` 725, `CarbonCreditServiceImpl` 251 | issuance, inventory, retirement, certificates |
| Marketplace & orders | `MarketplaceController`, `OrderController` | `MarketplaceServiceImpl` 658, `OrderServiceImpl` 363 | listing CRUD + buy flow |
| Profit sharing | `ProfitSharingController`, `CompanyPayoutController` | `ProfitSharingServiceImpl` 574, `CompanyPayoutQueryServiceImpl` 780 | EV-owner payouts, async executor |
| Emissions / CVA | `EmissionReportController` 257, `ReportController`, `ReportAnalysisController`, `CvaDashboardController` | `EmissionReportServiceImpl` 527, `ReportAnalysisService` 342, analysis/rules (10 files) | verification workflow |
| AI | `ChatAiController`, `EmissionAiController` | `GeminiAiScoringService` 1105, `GeminiAiService` 527, `VertexGeminiService` | 2 AI stacks |
| Projects / vehicles | `ProjectController`, `ProjectApplicationController`, `VehicleController` | `ProjectServiceImpl` 175, `ProjectApplicationServiceImpl` 304, `VehicleServiceImpl` 204, `admin/VehicleControlServiceImpl` 212 | |
| Notifications | `NotificationController` | `SseService`, `helper/notification/*` (SSE + async email) | WebSocket config exists too |
| Admin / dashboard | `AdminController`, `ApiController` | `DashboardCardService`, admin services | |

## Entry Points

- HTTP API `/api/v1/**` via 28 controllers (`controller/`)
- OAuth2 login flow (`/oauth2/...`) via `AppConfig` security chain
- `@Scheduled` jobs: `CreditExpiryScheduler` (daily 02:00, bulk `markExpiredCredits()`), `UserCleanupScheduler`
- `DataInitializer` (`ApplicationRunner`) seeds roles + demo accounts + sample data at every startup
- SSE endpoint(s) via `NotificationController` + `SseService`

## External Dependencies

VNPay sandbox/prod gateway, Stripe API, PayPal API, AWS S3, Gmail SMTP, Google OAuth2, Google Gemini API + Vertex AI.

## Infrastructure

docker-compose: MySQL 8 (root/12345), backend (profile `local`), nginx-fronted frontend. Logs to `logs/` (p6spy + app). No metrics, no health endpoint, no CI.

---

# 3. Current Architecture

## Architecture Style

**Single-module package-by-layer layered architecture** (a.k.a. "Spring Boot default"). Boundaries are implicit: there is no enforced rule preventing a controller from calling a repository or a service from another domain — only convention.

## Actual Dependency Flow

```text
HTTP /api/v1/**
  ↓
JwtTokenValidator (filter) → RateLimitInterceptor
  ↓
Controller (28)  ── uses TuongCommonRequest/Response or CommonResponse envelope
  ↓                (some controllers return raw entities: PaymentOrder, EVOwner)
Service interface (≈40) → ServiceImpl (33)
  ↓ @Transactional (16 files jakarta.*, 7 files Spring's)
  ├── Repository (26, Spring Data JPA)
  │     └── MySQL (ddl-auto=update, no migrations)
  ├── Other services cross-domain (Order → CreditIssuance, WalletTransaction)
  ├── External SDKs inside services: Stripe, PayPal, S3, Gemini, WebClient(Vertex)
  ├── SseService (notifications) + EmailService (@Async)
  └── SecurityContextHolder (currentUser() helper duplicated in ~6 services)
```

## Request / Data Flow (buy flow, traced)

`OrderController` → `OrderServiceImpl.createOrder` (validate quantity/price, persist PENDING order) → frontend confirms → `OrderServiceImpl.completeOrder` (single `@Transactional`):
lock listing (`findByIdWithPessimisticLockAndDetails`) → lock source credit → load wallets (no lock) → mutate credit amounts → `creditIssuanceService.issueTradeCredit` → update wallet carbon balances → update listing → order SUCCESS → `processFinancialTransactions` → `WalletTransactionServiceImpl.createTransaction` ×2 (buyer debit, seller credit — balance mutation happens here, without wallet lock).

Profit sharing runs later via `@Async("profitSharingTaskExecutor")` → `WalletServiceImpl.transferFunds` (dual pessimistic locks, writes 2 transaction rows — with the copy-paste bug, see §5.2).

## Current Package Structure

```text
com.carbonx.marketcarbon
├── controller/ (28) + controller/advice
├── service/ (40 interfaces) ├── impl/ (33) ├── admin/ ├── analysis/ (+rules/10) ├── credit/ (+formula/)
├── repository/ (26)
├── model/ (25 entities + BaseEntity)
├── dto/ (85: request/44, response/41, analysis, dashboard)
├── config/ (25: security chain, JWT, OAuth2, AI×2, S3, VNPay, WebSocket, rate limit, DataInitializer)
├── common/ (enums, validators, annotations)
├── exception/ (13: GlobalExceptionHandler + hierarchy)
├── scheduler/ (4)   ├── helper/notification/ (4)   ├── certificate/ (4)
├── spec/ (1 JPA specification)
├── mapper/ (2, disabled)
└── utils/ (6) + utils/Tuong/ (3 response-envelope classes named after a team member)
```

## Architecture Diagram

```text
┌────────────┐   ┌─────────────────────────── Market_carbon (single Spring Boot app) ──────────────────────────┐
│  React SPA │──▶│ Controller layer ─ Service layer ─ Repository layer ─ MySQL                                        │
└────────────┘   │      │      │            │    │                                                        │
                 │      │      │            │    ├──▶ Stripe / PayPal / VNPay (HTTP, some inside @Transactional)│
                 │      │      │            │    ├──▶ Gemini / Vertex AI (blocking WebClient)                   │
                 │      │      │            │    ├──▶ AWS S3                                                            │
                 │      │      │            │    └──▶ Redis (OTP only)                                                │
                 │      │      │            ├──▶ @Async executor (email, profit sharing) ──▶ DB                    │
                 │      │      │            └──▶ SSE emitters                                                        │
                 │      │      └── SecurityContextHolder.getContext() (current user, in ~6 services)              │
                 └────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

# 4. What Is Already Good

## 4.1 Pessimistic locking on the trade path

### Why It Is Good
Concurrent buys of the same listing are serialized at the DB level — the hardest concurrency problem in this domain is already approached correctly.

### Evidence
`MarketplaceListingRepository.findByIdWithPessimisticLock` + `...AndDetails` (`@Lock(PESSIMISTIC_WRITE)`), `CarbonCreditRepository.findByIdWithPessimisticLock`, `PaymentOrderRepository.findByIdWithLock`; used in `OrderServiceImpl.completeOrder:191-200`, `PaymentServiceImpl.processPaymentOrder:116`.

### What Must Be Preserved
The lock-first pattern in `completeOrder`; the re-check of quantity/balance **after** taking the lock (`OrderServiceImpl:205-222`).

### Classification
KEEP

## 4.2 Banking-style transfer with balance audit trail

### Why It Is Good
`WalletServiceImpl.transferFunds` locks both wallets `SELECT ... FOR UPDATE`, checks funds on the locked read, and writes `balanceBefore`/`balanceAfter` on `WalletTransaction` — the right shape for money movement and dispute auditing.

### Evidence
`WalletServiceImpl:308-404` (dual `entityManager.find(..., PESSIMISTIC_WRITE)` at 328/334; audit fields at 359-385).

### What Must Be Preserved
The locking order and audit fields. (One bug to fix inside it — §5.2.)

### Classification
KEEP (with bug fix)

## 4.3 VNPay signature verification

### Why It Is Good
Return URL is verified with HMAC-SHA512 over sorted fields before any status update — resists tampered callbacks.

### Evidence
`VNPayService.orderReturn:94-123` (`VNPayConfig.hashAllFields` compare at 108-110).

### What Must Be Preserved
The verify-then-mutate order.

### Classification
KEEP

## 4.4 Global exception handling with a coherent envelope

### Why It Is Good
One `@ControllerAdvice` covers validation, security, type mismatch, malformed JSON, integrity violations; messages are field-specific; most handlers share `buildErrorResponse`.

### Evidence
`exception/GlobalExceptionHandler.java` (206 LOC, handlers at 44-203).

### What Must Be Preserved
Central handling, field-level validation messages. (Fix the 500 leak + envelope split — §5.10, §6.)

### Classification
IMPROVE

## 4.5 Security & platform hygiene beyond the minimum

### Why It Is Good
Rate limiting (Bucket4j `RateLimitInterceptor` + config), JWT filter, BCrypt, `@EnableMethodSecurity`, OpenAPI/Swagger, security-context-propagating async executor (`DelegatingSecurityContextAsyncTaskExecutor` in `ConcurrencyConfig`).

### Evidence
`config/RateLimitConfig.java`, `config/RateLimitInterceptor.java`, `config/AppConfig.java:26`, `config/ConcurrencyConfig.java`.

### What Must Be Preserved
All of the above; the security-context propagation for `@Async` is an often-missed detail.

### Classification
KEEP

## 4.6 DTO separation and validation

### Why It Is Good
85 dedicated request/response DTOs; controllers use `@Valid` (e.g. `PaymentController:34`); entities are not the API contract (with exceptions noted in §6).

### Evidence
`dto/request` (44), `dto/response` (41).

### What Must Be Preserved
The boundary; extend it to the leaking endpoints.

### Classification
KEEP

## 4.7 The current N+1-optimization branch work

### Why It Is Good
`findByCompanyIdWithDetails`, `findByStatusAndExpiresAtAfterWithDetails` etc. are evidence-based fixes (p6spy counting), plus `@Transactional(readOnly = true)` on read paths and the swap to Spring's `@Transactional` where touched — all standard, low-risk performance practice.

### Evidence
`git diff` on `CarbonCreditRepository`, `MarketplaceListingRepository`, `OrderRepository`, `WalletRepository`, `MarketplaceServiceImpl`, `OrderServiceImpl`, `WalletServiceImpl`; `pom.xml` p6spy 3.9.1.

### What Must Be Preserved
The fetch-join repository methods and the p6spy measurement habit.

### Classification
KEEP (finish the `@Transactional` migration repo-wide)

## 4.8 Scheduler and seeding patterns

### Why It Is Good
`CreditExpiryScheduler` does a single bulk update (`markExpiredCredits()`), no row-by-row scanning; `DataInitializer` checks existence before inserting (idempotent re-runs).

### Evidence
`scheduler/CreditExpiryScheduler.java:17-26`; `config/DataInitializer.java:66-80`.

### What Must Be Preserved
Bulk-update scheduling; idempotent seeding.

### Classification
KEEP

---

# 5. Architecture Problems

## 5.1 Deposit confirmation trusts the client (free money)

### Severity
Critical

### Impact
Any authenticated user can credit their wallet with arbitrary amounts; funds used for real trades on a deployed system.

### Evidence
- `PaymentServiceImpl.processPaymentOrder:125-138`: `boolean paymentConfirmed = true; // hoặc verifyPaymentFromGateway(paymentId)` — `paymentId` is accepted but **never used**; PENDING→SUCCEEDED with zero verification.
- `WalletController.addMoneyToWallet:80-106` (`POST /api/v1/wallet/deposit?order_id&payment_id`): calls `processPaymentOrder(orderId, paymentId)` **without checking that the payment order belongs to the current user**, then `addBalanceToWallet(order.getAmount())` credits the *caller's* wallet.
- A user can create a PENDING order of any amount via `POST /api/v1/payment` (no payment needed) then call `/wallet/deposit` → infinite balance. Alternatively race/poll other users' `order_id`s to steal their deposit.

### Root Cause
Client-driven "success page" flow instead of server-verified webhooks/callbacks; verification was stubbed and never finished.

### Risk
Direct financial loss; full loss of trust in wallet balances.

### Recommendation
(1) Verify server-side: Stripe `checkout.session.retrieve`/webhook with signature, PayPal `payment.get`/webhook verification, VNPay already has signature flow. (2) Enforce `order.getUser().getId().equals(currentUser().getId())`. (3) Credit the wallet exactly once inside the same transaction that flips SUCCEEDED (the `findByIdWithLock` + status check is already an idempotency base). Sources: [Stripe webhooks](https://docs.stripe.com/webhooks), [PayPal webhook guide](https://hookdeck.com/webhooks/platforms/guide-to-paypal-webhooks-features-and-best-practices), [webhook idempotency practices](https://www.stigg.io/blog-posts/best-practices-i-wish-we-knew-when-integrating-stripe-webhooks).

## 5.2 `transferFunds` writes wrong balances into the destination transaction row

### Severity
Critical (data corruption of financial records)

### Impact
Every profit-sharing credit row shows the *sender's* balanceBefore/After; user-visible history and any future reconciliation/audit is wrong.

### Evidence
`WalletServiceImpl:373-385`: `toTransaction` builder sets `.balanceBefore(fromBefore)` and `.balanceAfter(fromAfter)` — copy-paste from the debit record; should be `toBefore`/`toAfter`.

### Root Cause
Copy-paste in a long manual builder block; no test covering the credit side.

### Risk
Wrong statements shown to EV owners; reconciliation impossible.

### Recommendation
Fix fields; add unit test asserting each side's before/after; consider a small `WalletLedger` helper so both records are built from one place.

## 5.3 Payment architecture has no completion path for VNPay and none verified for Stripe/PayPal

### Severity
High

### Impact
VNPay: `orderReturn` only flips `PaymentOrder` status (`VNPayService:110-118`) — the wallet is never credited by that flow (users must notice and use the unverified deposit endpoint → §5.1). Stripe/PayPal: success redirect → frontend → `/wallet/deposit` with an unverified `paymentId`. No webhooks exist anywhere.

### Root Cause
Gateway integration modeled on demos (redirect-and-hope) instead of [webhook + server-side verification](https://docs.stripe.com/webhooks).

### Risk
Lost deposits, replayed deposits, untraceable state.

### Recommendation
One `PaymentGateway` port per provider with `verifyPayment(orderRef): VerifiedPayment`; webhook endpoints (`/api/v1/payments/{provider}/webhook`) with signature verification and idempotent processing; the redirect page only *reads* status, never triggers crediting.

## 5.4 `.anyRequest().permitAll()` and a test controller in the security chain

### Severity
High

### Impact
Default-open posture: any endpoint not matched by earlier rules is public (e.g. `/api/test/**` upload controller — wait, `/api/**` requires auth, so `/api/test/upload1e` is authenticated; but any future non-`/api` endpoint is open by default). Swagger is intentionally public.

### Evidence
`config/AppConfig.java:75-81` (`requestMatchers("/api/**").authenticated()` … `.anyRequest().permitAll()`); `controller/FileUploadTestController.java` (`/api/test/upload1e`, `System.out.println` logging, S3 upload with no ownership/size validation).

### Root Cause
Permissive default chosen for convenience.

### Risk
A new mapping added outside `/api/**` ships public by accident.

### Recommendation
`.anyRequest().authenticated()`; delete `FileUploadTestController`.

## 5.5 Buyer wallet not locked during order completion (double-spend)

### Severity
High

### Impact
Two concurrent `completeOrder`s for the same buyer both pass the balance check (`OrderServiceImpl:218`) and both debit via `WalletTransactionServiceImpl.createTransaction:67-100`, which reads `wallet.getBalance()` without a lock → lost update / negative-effective balance.

### Evidence
`OrderServiceImpl:214-227` uses `findByCompanyIdWithDetails` (no lock); `WalletTransactionServiceImpl:47-52` re-reads via `findById` (no lock). `WalletRepository` already contains `findByUserIdWithPessimisticWrite` — unused on this path.

### Root Cause
Locks were added to listing/credit but the wallet debit path was left optimistic.

### Risk
Overdraft / inconsistent balances under concurrency.

### Recommendation
Lock both wallets (ordered by id to avoid deadlock) or perform the debit as `UPDATE wallet SET balance = balance - :amt WHERE id = :id AND balance >= :amt` and check affected rows. Keep everything inside the existing transaction.

## 5.6 Order ERROR status is written inside the transaction that rolls back

### Severity
High

### Impact
When completion fails, the intent is to persist `OrderStatus.ERROR`, but the save happens in the same `@Transactional` scope as the exception (`OrderServiceImpl:319-324`) — the rollback discards it; the order stays PENDING forever.

### Evidence
`OrderServiceImpl:319-324` (`catch → order.setOrderStatus(ERROR); orderRepository.save(order); throw e;`).

### Root Cause
Misunderstanding of rollback semantics.

### Recommendation
Persist the ERROR transition in a separate `REQUIRES_NEW` transaction (small `OrderStatusService`), or mark the tx rollback-only and update status out-of-band.

## 5.7 Fractional-quantity truncation in the buy flow

### Severity
Medium-High

### Impact
Buyer pays `totalPrice = unitPrice × quantity` (full fractional quantity) but the wallet carbon balance is credited `quantityToBuy.intValue()` (`OrderServiceImpl:272`) — fractional credits are paid for and lost. The seller side subtracts the full fractional amount (`:290`), so the books don't balance.

### Evidence
`OrderServiceImpl:271-292`.

### Root Cause
Integer assumption ("mỗi credit = 1 unit") conflicting with `BigDecimal(18,4)` columns.

### Recommendation
Decide the invariant: either quantities are integral (validate at creation) or all arithmetic stays BigDecimal. Remove `intValue()`.

## 5.8 IDOR / ownership gaps on reads

### Severity
Medium

### Impact
Authenticated users can read other companies' order details.

### Evidence
`OrderServiceImpl.getOrderById:126-138` — no check `order.getCompany().getId().equals(currentCompany(...))` (compare `cancelOrder:158-174` which does check). `WalletController.countAllTransactions:142-156` has `@PreAuthorize("hasRole('ADMIN')")` commented out.

### Root Cause
Ownership checks applied inconsistently, method security underused.

### Recommendation
Ownership check in `getOrderById`; re-enable `@PreAuthorize`; add ArchUnit/test rule that read endpoints on company-scoped resources verify ownership.

## 5.9 Mixed transaction annotation semantics

### Severity
Medium

### Impact
16 files use `jakarta.transaction.Transactional` (no `readOnly`, no `isolation`, no `NESTED`, different rollback rules) vs 7 using Spring's — including wallet/order/payment services. Behavior differences bite exactly in money code.

### Evidence
grep: `WalletServiceImpl:19`, `OrderServiceImpl:15`, `WalletTransactionServiceImpl:12`, `PaymentServiceImpl:23`, `CreditIssuanceServiceImpl:21`, `AuthServiceImpl:21`, schedulers, etc. Differences per [Spring reference](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) and [Baeldung comparison](https://www.baeldung.com/spring-vs-jta-transactional).

### Root Cause
Incremental migration started (this branch) but not finished.

### Risk
Subtle rollback/readOnly differences between services.

### Recommendation
Finish the migration to `org.springframework.transaction.annotation.Transactional` everywhere (mechanical, one commit), add a checkstyle/ArchUnit rule banning the jakarta import.

## 5.10 Cross-cutting inconsistencies (envelope, mapping, entity leakage)

### Severity
Medium

### Impact
API consumers face two envelopes (`CommonResponse` vs `TuongCommonResponse` + ad-hoc `Map` in the CSV handler, `GlobalExceptionHandler:190-203`); some endpoints return raw JPA entities (`PaymentController.getAllPaymentsByUser` → `List<PaymentOrder>`; `KycController.getUserKyc` → `EVOwner`); MapStruct is configured but both mappers are commented out, so all mapping is hand-written builders (duplication across the big services); `GlobalExceptionHandler.handleOther:118-124` returns raw `ex.getMessage()` on 500 (information disclosure).

### Root Cause
Grew by accretion; no API style guide.

### Recommendation
Pick one envelope, map entities → DTO at the controller boundary, decide MapStruct (enable) or delete it, make the 500 handler return a generic message + logged trace id.

## 5.11 God method and god classes beyond cohesive size

### Severity
Medium

### Impact
Reviewability and regression risk concentrated in few files.

### Evidence
- `MarketplaceServiceImpl.listCreditsForSale` spans lines 60–371 (~310 lines): validation, credit resolution, expiry checks, listing creation, credit-status updates, response building.
- `GeminiAiScoringService` (1105 LOC): prompt building + statistics (quantile/mean/stddev, 605-643) + JSON parsing (377-417) + config getters (1013-1093) + note synthesis (644+).
- `MyCreditServiceImpl` (725 LOC, 12 injected deps): queries + retirement + PDF + email + SSE + storage.
- `ProfitSharingServiceImpl` (574 LOC, 12 deps) with self-proxy hack `getSelf()` via `ApplicationContext` (lines 63-74) to defeat self-invocation.
- Duplication: `currentUser()`/`currentCompany()` helpers copy-pasted in ≥6 services.

### Root Cause
Layer-first packages give no natural home for domain helpers; testing gap removes pressure to decompose.

### Recommendation
Targeted extractions with technical justification (see §8/§15 Phase 5) — not a blanket split by LOC.

---

# 6. Technical Debt

## Architecture Debt

- Implicit boundaries: any layer may call any other; e.g. repositories gained business-ish fetch graphs while services still hand-assemble domain logic. No module concept for 10+ clearly separable domains.
- Two parallel AI stacks (`AiConfig`+`GeminiAiService` vs `AiVertexConfig`+`VertexWebClientConfig`+`VertexGeminiService`) with overlapping responsibility.
- `utils/Tuong/` — response envelope named after a person; `common/` vs `utils/` overlap.
- Payment/UI coupling: `VNPaymentController` is a Thymeleaf `@Controller` inside a REST API app (`VNPaymentController:12-49`).

## Code Debt

- `System.out.println` at 10 sites incl. JWT validator (emails), Stripe session object, OAuth2 success (`JwtTokenValidator:90`, `PaymentServiceImpl:165`, `AppConfig:116`, `SearchRepository:80`, `AiConfig:77`, `S3Config:27`, `VertexWebClientConfig:75`, `FileUploadTestController`).
- Commented-out code blocks kept in controllers (`WalletController:58-77,158-173`).
- Dead schema/logic: `MarketPlaceListing.idempotencyKey` column with elaborate comments but **zero usage** (grep across repo).
- Manual builder mapping duplicated per service; MapStruct present but disabled.
- Mixed language comments (VN/EN) with step comments (`// B4.1`) that encode legacy teaching-style flow.
- `sellerPayout = totalPrice` while `platformFee = tradingFee` is stored (`OrderServiceImpl:100-112`) — payout math not actually reduced by fee here; fee application lives implicitly in profit sharing; single source of truth missing.

## Dependency Debt

- `pom.xml` duplicates: `jackson-dataformat-csv` (lines 163-167 and 176-180), `spring-boot-starter-oauth2-client` (197-200 and 256-259).
- Legacy/deprecated: PayPal `rest-api-sdk` 1.14.0 (deprecated SDK), jjwt 0.11.5 (current 0.12.x line), `org.json` alongside Jackson.
- Identity debt: `groupId com.example`, `artifactId spring-rest-auth-vehicles-kyc` — a template name unrelated to the product.

## Testing Debt

- 4 test classes / 26 `@Test` methods for 367 production files. Covered: Marketplace (8), CreditIssuance (8), EmissionReport (5), ProjectApplication (5).
- **Zero tests**: Wallet, WalletTransaction (balance math!), Payment, VNPay, Order.completeOrder, ProfitSharing, Withdrawal — the money paths.
- No integration tests (no Testcontainers/H2), no ArchUnit, no CI to run anything.

## Performance Debt

- Historical N+1 from EAGER `@OneToOne` on `Wallet` (`Wallet.java:25-36`) — being fixed on this branch via fetch-join repositories; root cause (EAGER default) remains for future queries.
- `getTransactions()` loads **all** wallet transactions then merges in memory (`WalletTransactionServiceImpl:130-214`) — unbounded as history grows; no pagination.
- Blocking Gemini/Vertex calls on request threads (AI scoring inside verification flow).
- In-memory filtering patterns (`CompanyPayoutQueryServiceImpl.matchesSearch:493` after full aggregation loads).

## Security Debt

- **Committed secrets**: real Gmail SMTP app password in `docker-compose.yml` (be service env: `SPRING_MAIL_PASSWORD`), MySQL root password 12345; `application-local.properties` (untracked but in worktree) holds live local credentials; `spy.properties` present. → rotate all, move to env/secret store, add to `.gitignore`.
- Demo accounts with fixed passwords seeded at startup (`DataInitializer:43-58`) on a public deployment; README publishes them.
- 500 handler leaks exception messages (§5.10).
- JWT in OAuth2 redirect URL query string (`AppConfig:117`) — token leakage via history/referrer.
- CORS: wildcard-ish entries passed to `setAllowedOrigins` (only `setAllowedOriginPatterns` supports patterns) — `http://192.168.*.*:*` etc. never match (dead config) (`AppConfig:129-147`); `httpBasic` enabled unnecessarily (`:88`).
- `ddl-auto=update` in production (schema drift, no history).

## Observability Debt

- No Actuator/metrics/health; no MDC/correlation IDs (trace header accepted but only echoed into the response envelope); `System.out.println` instead of loggers; p6spy SQL logging to `logs/` committed as a directory.

## Maintainability Debt

- 310-line method (§5.11); 12-dependency services; copy-pasted auth helpers; junk files committed at repo root: `daq`, `et --hard f55ba43`, `git log --graph --oneline --decorate --all`, `how --stat 86a9b8f`, `t --rebase`, `e -i HEAD~3`, `git` (empty), `img_1.png`, `img_2.png`, `uploads/`, `package-lock.json` (root, stray), `01-card-issuing.md`, `CV_METRICS.md`, `OPTIMIZATION_LOG.md`, `profolio.code-workspace` — repo hygiene noise.
- `logs/` and `seed_large.py` inside `src/main/resources` (packaged into the jar).

---

# 7. Large Files / Large Classes

| File | LOC | Responsibility Count | Problem | Recommended Action | Priority |
| ---- | --- | -------------------- | ------- | ------------------ | -------- |
| service/impl/GeminiAiScoringService.java | 1105 | ≥5 (prompt build, stats, JSON parse, config, notes) | Mixed stats/parsing/config | Split into `ScoringPromptBuilder` + `DataQualityStatistics` + config props | Medium |
| service/impl/CompanyPayoutQueryServiceImpl.java | 780 | 4 (formula doc, owner aggregation, pagination/search, summary) | Query-side god service, read-model assembly | Keep; extract owner-aggregation builder; consider read-model DTO assembly | Low-Med |
| service/impl/MyCreditServiceImpl.java | 725 | 6 (list/query, expiry, retirement, certificate PDF, email/SSE notify, storage) | 12 deps; write+notify+render mixed | Extract `RetirementService` + move notifications to events | Medium |
| service/impl/CreditIssuanceServiceImpl.java | 694 | 4 (issuance rules, serial numbering, persistence, notification) | Core domain logic, cohesive but big | Keep; extract notification/email side-effects | Low-Med |
| service/impl/MarketplaceServiceImpl.java | 658 | 5 (create/update/cancel listing, credit status math, response building) | **~310-line method** `listCreditsForSale` | Extract validation + credit-resolution + response-mapper steps | High |
| service/impl/ProfitSharingServiceImpl.java | 574 | 5 (contribution calc, payout calc, transfer, persistence, async email) | `getSelf()` proxy hack; 12 deps | Remove hack via `TransactionTemplate` or separate bean; extract email | Medium |
| service/GeminiAiService.java | 527 | (interface file at 527 LOC — likely contains impl or constants) | Interface/impl smell | Verify and split | Medium |
| service/impl/EmissionReportServiceImpl.java | 527 | 4 | Report workflow + status transitions | Keep; add state-transition tests | Low |
| service/impl/KycServiceImpl.java | 427 | 3 | Multi-profile KYC | Keep | Low |
| service/impl/WalletServiceImpl.java | 405 | 4 (wallet CRUD, deposit mapping, summaries, transfer) | **Contains critical bug §5.2**; summary side-effect writes | Fix bug first; extract `WalletSummaryAssembler` | High |
| service/impl/OrderServiceImpl.java | 363 | 3 | **Contains §5.5/5.6/5.7 bugs** | Fix bugs first; keep structure | High |
| controller/KycController.java | 334 | endpoint hub | Returns raw entity in one place | DTO-ify; otherwise fine | Low |
| config/DataInitializer.java | 323 | seeding | Hardcoded demo creds | Move to profile-gated seeder | Medium |
| exception/GlobalExceptionHandler.java | 206 | error mapping | 500 leak, envelope mix | Fix §5.10 | High |

Note per audit rules: files are ranked by *responsibility problems + contained bugs*, not raw LOC. `EmissionReportServiceImpl` (527) is cohesive workflow code — lower priority than smaller-but-buggier `OrderServiceImpl`.

---

# 8. Responsibility Analysis

## OrderServiceImpl (363 LOC)

### Current Responsibilities
1. Current-user/company resolution (52-67)
2. Order creation + pricing/fee snapshot (69-123)
3. Order reads (no ownership check — §5.8) (125-155)
4. Cancellation with ownership check (157-174)
5. **Settlement orchestration**: locks, wallet checks, credit mutation, issuance call, listing update, financial transactions, error marking (176-325)
6. Financial transaction record assembly (328-361)

### Responsibility Problems
Settlement is a use-case spanning 3 aggregates + 2 other services inside one method; error-marking breaks rollback semantics (§5.6).

### Proposed Separation

```text
Current                          Target (justified by bugs, not aesthetics)
OrderServiceImpl (all above)  →  OrderCommandService        (create/cancel/read — thin)
                                OrderSettlementService     (completeOrder only)
                                shared: CurrentUserResolver (one bean, ends 6× copy-paste)
```
No further splitting until settlement is correct + tested.

## WalletServiceImpl (405 LOC)

### Current Responsibilities
1. Wallet provisioning (59-70)
2. Read model assembly: wallet + transactions + per-credit summaries with chain-walking `resolveRootCredit`/`resolveEffectiveBatch` (152-295)
3. Deposit application (`addBalanceToWallet`) (89-124)
4. Funds transfer with dual locks (308-404)

### Responsibility Problems
Summary assembly performs a **write** side-effect (`wallet.setCompany; save` at 179-180) inside a read path; transfer + summaries + provisioning will keep growing apart.

### Proposed Separation

```text
Current                       Target
WalletServiceImpl          →  WalletService            (provisioning, transfer — fix §5.2)
                              WalletSummaryAssembler   (pure read model, no writes)
                              DepositService           (verified crediting only — §5.1)
```

## GeminiAiScoringService (1105 LOC)

### Current Responsibilities
1. Orchestration `suggestScore` (50-237)
2. Context/prompt building (239-490)
3. Data-quality statistics: quantile/mean/stddev/DQ metrics (491-643)
4. Response JSON parsing/safety (343-425)
5. Config plumbing via property getters (1013-1093)

### Proposed Separation

```text
Current                       Target
GeminiAiScoringService     →  AiScoringService        (orchestration only)
                              ScoringPromptBuilder    (pure functions)
                              DataQualityStatistics   (pure, unit-testable without AI)
                              ScoringProperties       (@ConfigurationProperties — kills 8 getters)
```
Justification: statistics and prompt text are unit-testable today only by instantiating a 1105-LOC class.

---

# 9. Package / Module Analysis

## Current Structure
Package-by-layer (see §3 tree). Technical naming (`impl`, `utils`, `helper`, `spec`) rather than domain naming; two domain-flavored exceptions (`service/analysis`, `service/credit`) prove the team already trends feature-ward.

## Problems
- Feature logic scattered: "buy a credit" spans `controller/OrderController`, `service/OrderService(+impl)`, `repository/OrderRepository`, `model/Order`, DTOs in 3 packages — 5 places per feature, high merge-friction for a 4-person team.
- `impl` package with 33 classes is a dumping ground; some "interfaces" exist only for the impl (e.g. `PutResult`, `CreditQuery` are not services).
- `utils/Tuong`, `helper/notification`, `certificate`, `common` overlap as "shared stuff" locations.

## Dependency Violations
None mechanically enforced or measured (no ArchUnit). By inspection: services reach across domains freely (`OrderServiceImpl` → `CreditIssuanceService`, `WalletTransactionService`; `PaymentServiceImpl` → `SseService`); controllers stay on services (good); repositories contain fetch-graph knowledge driven by service read-models (acceptable, but drifting).

## Circular Dependencies
Package-level cycles: service ↔ repository across domains are acyclic today (JPA repos don't call services). Bean-level cycle risk exists via `ProfitSharingServiceImpl.getSelf()` self-reference (workaround, not a cycle). Status: no blocking cycle found — UNKNOWN whether javac-level cycles exist between `service` and `service/impl` subpackages (interfaces live beside impls in the same package tree, so risk is low).

## Boundary Violations
- Security context read from service layer everywhere (`SecurityContextHolder` in 6+ services) — couples business logic to the web thread.
- External SDK calls (Stripe/PayPal/Gemini/S3) inside `@Transactional` service methods (e.g. `PaymentServiceImpl` Stripe session creation is not transactional, but `processPaymentOrder` path and others mix concerns).
- Presentation annotations on entities (`CarbonCredit.currentPrice` `@JsonProperty`, `Wallet` Jackson annotations) — serialization concerns inside the domain model.

---

# 10. External Best Practices Research

## Research Methodology
Web research performed 2026-08-16 on: Spring `@Transactional` semantics, payment webhook/idempotency practices, modular monolith & package-by-feature organization. Prioritized official docs (Spring, Stripe) and practitioner guides; cross-checked ≥2 sources per claim. Well-known reference repositories cited from established knowledge (marked as such).

## Reference 1 — Spring Framework (official): `@Transactional` semantics

### Source
https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html

### Relevant Finding
Spring supports both its own and `jakarta.transaction.Transactional`; the Jakarta variant lacks `readOnly`, `isolation`, `timeout`, `NESTED` and has different rollback-rule expressiveness; proxy self-invocation does not start transactions.

### Application To This Repository
Directly explains §5.9 (16 vs 7 file split) and validates the current branch's migration; self-invocation explains the `getSelf()` hack in `ProfitSharingServiceImpl`.

### Not Applicable
JTA/XA scenarios (not used here).

## Reference 2 — Stripe (official) + webhook practice guides

### Sources
https://docs.stripe.com/webhooks ; https://hookdeck.com/webhooks/platforms/guide-to-paypal-webhooks-features-and-best-practices ; https://www.stigg.io/blog-posts/best-practices-i-wish-we-knew-when-integrating-stripe-webhooks ; https://medium.com/@sohail_saifii/handling-payment-webhooks-reliably-idempotency-retries-validation-69b762720bf5

### Relevant Finding
Verify signatures server-side; treat webhooks as the source of truth; make handlers idempotent (event id / status check under lock); return fast, process in-transaction.

### Application To This Repository
Blueprint for redesigning §5.1/§5.3: verify → lock payment order → flip status + credit wallet in one transaction.

## Reference 3 — Spring Modulith (official Spring project)

### Sources
https://spring.io/projects/spring-modulith (project page); practitioner write-ups: https://www.codecentric.de/en/knowledge-hub/blog/modularization-the-easy-way-spring-modulith-with-kotlin-and-hexagonal-architecture ; https://notes.softwarearchitect.id/p/combining-modular-monolith-and-hexagonal

### Relevant Finding
Modular monolith with package-by-feature modules, verified boundaries (tests assert allowed package dependencies), event-driven decoupling inside one deployable.

### Application To This Repository
Target model for §13/§14 — without multi-module Maven overhead.

### Not Applicable
Full hexagonal ports/adapters per module (overkill for this codebase size).

## Reference 4 — Canonical Spring examples & large codebases (established-knowledge references, not fetched today)

- `spring-projects/spring-petclinic` — the canonical package-by-layer layout; shows our current structure is the framework's default, not an anomaly.
- `keycloak/keycloak` — large modular monolith: Maven modules per feature (`server-spi`, per-feature providers) with enforced SPI boundaries; relevant for how domains like "wallet" vs "payments" can be moduleized later.
- JHipster-generated apps — convention: DTO + service + MapStruct mapper per entity, Assembler classes, `@Transactional(readOnly=true)` on reads, Flyway migrations from day one, CI gates — a pragmatic checklist contrast for §11.

## Reference 5 — Comparative architecture guides

### Sources
https://www.ensolvers.com/post/comparative-guide-to-application-architectures-with-spring-boot-multi-module-hexagonal-and-microservices ; https://foojay.io/today/clean-and-modular-java-a-hexagonal-architecture-approach/

### Relevant Finding
For small/medium teams: single-module modular monolith beats multi-module and microservices; start with module boundaries + tests, split modules only when build/team pain appears.

### Application To This Repository
Reject microservices/multi-module for a 4-person academic team; adopt package-by-feature inside the single module (§12).

---

# 11. Best Practice Comparison

| Area | Current | External Practice | Gap | Recommendation |
| ---- | ------- | ----------------- | --- | -------------- |
| Package Structure | Package-by-layer + impl dumping ground | Package-by-feature modules (Spring Modulith, Keycloak) | High | Migrate incrementally (Phase 4) |
| Layer Boundary | Convention only, unenforced | ArchUnit/Modulith verification in CI | High | Add ArchUnit after reorg |
| Dependency Direction | Mostly correct (ctrl→svc→repo); SecurityContextHolder in services | Auth resolved at web layer, passed as parameter | Medium | `CurrentUserResolver` bean + parameter passing |
| Business Logic | In services (Transaction Script); anemic entities | Rich domain optional; acceptable either way | Low | Keep services; extract pure calculators where tested (pricing, payouts) |
| Infrastructure | SDK calls inside services/transactions | Adapters behind ports (PaymentGateway, AiClient) | High | Phase 5 |
| Configuration | Properties via `@Value` scattered (incl. `@Value("${trading_fee}")` in services, hardcoded in local properties); `getDoubleProperty` manual reads in AI service | `@ConfigurationProperties` typed objects | Medium | Consolidate (e.g. `TradingProperties`) |
| Error Handling | Central handler; 500 leaks message; 3 envelope shapes | One envelope; generic 500 + trace id | Medium | §5.10 fixes |
| Testing | 26 unit tests; no money-path tests; no CI | JHipster baseline: per-service tests + IT + CI gates | Critical | Phase 1 tests + GitHub Actions |
| Observability | println + p6spy; no actuator/MDC | Actuator, structured logs, MDC trace ids | High | Phase 0/6 |
| Security | Good primitives; permitAll default, secrets committed, client-trusted deposits | Least privilege, secret mgmt, verified callbacks | Critical | §5.1/§5.4 + rotation |
| Concurrency | Locks on listing/credit; wallet debit unlocked | Lock or atomic-update every balance mutation | High | §5.5 |
| Module Boundaries | None (single layer packages) | Feature modules w/ verified edges | High | Phase 4 |

**Conclusion**: the repository is a **package-by-layer Transaction-Script service-centric monolith** (Petclinic-style), with genuine production instincts (locks, rate limits, signatures) and demo-grade corners (payment confirmation, secrets, migrations). It is *not* an incoherent architecture — it is an unfinished one.

---

# 12. Architecture Decision

## Recommended Architecture

**Single-module Modular Monolith with package-by-feature, keeping layered internals per module (Controller → Service → Repository), plus explicit ports for external systems (payments, AI, storage).**

## Why This Architecture

- Domain has ≥10 separable features (§2 table) that map cleanly to packages — matches Spring Modulith guidance and Keycloak/JHipster practice without build complexity.
- Team of 4 + academic timeline → one Maven module, IDE-refactorable moves, no release-train overhead (Ensolvers comparative guide).
- The existing code is *already* layer-clean inside features; the migration is mechanical (move classes), not semantic (rewrite logic).
- Ports/adapters only where external systems already exist (payments, AI, S3) — justified by the §5.1/§5.3 fix, not by pattern appetite.

## Why Not Other Alternatives

### Alternative 1 — Keep package-by-layer as-is
Rejected: merge conflicts and scattered features are the daily tax on a 4-person team; nothing enforces the boundaries that are only conventional today. But no urgency — this changes in Phase 4, after correctness.

### Alternative 2 — Multi-module Maven / microservices
Rejected: operational and build overhead with no team scale to justify it; deployment is a single VPS docker-compose.

### Alternative 3 — Full Clean/Hexagonal (domain/application/infrastructure per module)
Rejected: 367 files don't need 15 layers; the audit found the value concentrated in *fixing flows*, and over-abstraction directly conflicts with Rule F/G/H (no abstraction for "clean"). Hexagonal reserved for the payment adapters only, where the §5.1 fix forces an interface anyway.

---

# 13. Target Architecture

## Architecture Overview

```text
com.carbonx.marketcarbon
├── shared/                    (envelope, error codes, pagination, CurrentUserResolver)
├── config/                    (Spring wiring per concern; profile-gated seeders)
├── auth/                      (login, JWT, OAuth2, OTP)
├── user/                      (users, roles, KYC profiles: evowner/company/cva/admin)
├── wallet/                    (wallets, transactions, ledger, deposit application)
├── payment/                   (PaymentOrder + gateway adapters: vnpay/ stripe/ paypal/ + webhook API)
├── credit/                    (batches, credits, issuance, serials, retirement, certificates)
├── marketplace/               (listings, orders, settlement, pricing)
├── profitsharing/             (contributions, distributions, payouts)
├── emission/                  (reports, verification/CVA, analysis rules)
├── ai/                        (scoring, chat; AiClient port + vertex/gemini adapters)
├── project/  ├── vehicle/  ├── notification/ (SSE+email)  └── filestorage/ (S3/local)
```
Each feature package: `api/` (controller+DTOs) · `service/` · `repository/` · `model/` — only the folders it needs.

## Dependency Direction

```text
api → service → repository → model
service → shared (allowed)
cross-feature: only service → other feature's service interface (never repository/model directly)
payment gateways / ai / storage: service → port interface → adapter (infrastructure)
```

## Architectural Rules

### Allowed Dependencies
- Any package → `shared`, `config`
- `X.api → X.service`, `X.service → X.repository`
- `X.service → Y.service` (interface only) for the 4 sanctioned cross-calls: marketplace→credit (issuance), marketplace→wallet (settlement), profitsharing→wallet (transfer), emission→ai (scoring)

### Forbidden Dependencies
- `X.api → Y.repository` or `X.api → model of Y` (cross-feature entity leakage)
- Any → `utils.Tuong` (envelope unified in `shared`)
- `service → SecurityContextHolder` (use `CurrentUserResolver`)
- Repository layers containing business branching beyond query/fetch-graph definitions

---

# 14. Target Package Structure

```text
com.carbonx.marketcarbon
├── shared
│   ├── api          (CommonResponse/envelope, PageResponse)
│   ├── security     (CurrentUserResolver, annotations)
│   └── error        (ErrorCode, AppException hierarchy)
├── config           (AppConfig→split: SecurityConfig, CorsConfig, AsyncConfig, WebConfig, OpenApiConfig)
├── auth             (api/AuthController, service/, model/ none — reuses user)
├── user
│   ├── api          (UserController, KycController, dto/)
│   ├── service
│   ├── repository
│   └── model        (User, Role, Company, EVOwner, Cva, Admin)
├── wallet
│   ├── api          (WalletController, WithdrawalController)
│   ├── service      (WalletService, WalletTransactionService, DepositService, WalletSummaryAssembler)
│   ├── repository
│   └── model        (Wallet, WalletTransaction, Withdrawal)
├── payment
│   ├── api          (PaymentController, webhook controllers)
│   ├── service      (PaymentOrderService)
│   ├── gateway      (PaymentGateway port; vnpay/, stripe/, paypal/ adapters)
│   ├── repository
│   └── model        (PaymentOrder, PaymentDetails)
├── credit           (… model: CreditBatch, CarbonCredit, CreditCertificate, CreditSerialCounter)
├── marketplace      (… model: MarketPlaceListing, Order; service: ListingService, OrderService, OrderSettlementService)
├── profitsharing    (… model: ProfitDistribution(+Detail))
├── emission         (… model: EmissionReport(+Detail); analysis rules)
├── ai               (service: scoring+chat; client port; adapters gemini/, vertex/)
├── project | vehicle | notification | filestorage
└── CarbonXApplication.java
```

### Package: `payment`
* Purpose: own PaymentOrder lifecycle; expose `PaymentGateway` port.
* Responsibilities: create order, receive webhook/return, verify signature server-side, publish "payment verified" to wallet.
* Allowed: → wallet.service (DepositService), shared. Forbidden: Stripe/PayPal/VNPay SDK types outside `gateway/*` adapters.

### Package: `wallet`
* Purpose: single owner of balance mutations.
* Responsibilities: wallets, ledger writes (before/after), transfers, deposits application, summaries.
* Allowed: → shared, payment.service (to read verified orders). Forbidden: any SDK import; SecurityContextHolder.

### Package: `marketplace`
* Purpose: listings + orders + settlement orchestration.
* Allowed: → credit.service (issuance), wallet.service (settlement), shared. Forbidden: touching Wallet/CarbonCredit repositories directly (cross-feature repository access).

(Corresponding rules for remaining packages follow the same pattern; full matrix to be encoded as ArchUnit tests in Phase 6.)

---

# 15. Refactoring Strategy

## Principles
Behavior-preserving unless the behavior is itself the bug (Phases 0–1 are bug fixes by definition). One concern per phase; every phase compiles, runs the test suite, and is deployable. No new abstraction without a failing test or a bug it prevents.

## Migration Strategy
Fix correctness first (cheap, tests prove it), then consistency, then structure (mechanical moves), then boundaries (new code patterns), then guardrails. Rollback = git revert per phase; each phase is a separate PR.

## Phase 0 — Hygiene & secrets (1 day)

### Goal
Stop active leaks; remove noise.
### Files
`.gitignore`, `docker-compose.yml`, repo-root junk files, `controller/FileUploadTestController.java`, `GlobalExceptionHandler.java` (500 handler), `src/main/resources/` (remove `logs/`, `seed_large.py`).
### Changes
Rotate Gmail app password + any deployed secrets (out-of-band); parametrize compose credentials; delete junk files (`daq`, `et --hard f55ba43`, `git log --graph...`, `t --rebase`, `e -i HEAD~3`, `git`, stray `package-lock.json`, `img_*.png` → docs/); delete FileUploadTestController; 500 handler returns generic message; remove root `logs/` from git.
### Risk
Low (deletions of unused/dead files verified by grep).
### Tests
Compile + existing 26 tests.
### Rollback
git revert.

## Phase 1 — Money-path correctness (highest value)

### Goal
Close §5.1, §5.2, §5.5, §5.6, §5.7 with tests.
### Files
`PaymentServiceImpl`, `WalletController`, `WalletServiceImpl` (transferFunds + deposit), `OrderServiceImpl`, `WalletTransactionServiceImpl`, new `DepositService` + `PaymentGateway` interface (minimal, only `verify`), Stripe/PayPal/VNPay adapters (Stripe `Session.retrieve`, PayPal payment verify, VNPay reuse of existing HMAC).
### Changes
Server-side verification + ownership check + single-transaction crediting; fix `toTransaction` balances; lock/order wallets (by id) or atomic conditional UPDATE; ERROR status via `REQUIRES_NEW`; remove `intValue()` truncation (validate integral quantities at order creation).
### Risk
Medium — touches live flows; mitigated by new unit tests per bug (target: wallet debit/credit, transfer both sides, deposit idempotency×2, concurrent completion simulation with mocked repo locks).
### Tests
New: `WalletServiceImplTest` (transfer both sides), `DepositServiceTest` (verification/idempotency/ownership), `OrderSettlementTest` (insufficient funds, rollback leaves ERROR, fractional rejection).
### Rollback
Feature-flag `payment.verify.enabled` if emergency (default on after soak).

## Phase 2 — Consistency pass

### Goal
One transaction annotation, one envelope, mapper decision.
### Files
16 jakarta-`@Transactional` files; controllers returning entities (`PaymentController`, `KycController`); `mapper/*`; `GlobalExceptionHandler` CSV handler.
### Changes
Swap imports to Spring `@Transactional` (add `readOnly=true` where the current branch started it); unify on `TuongCommonResponse` (or rename to `ApiResponse` in shared) — mechanical wrapper change; enable MapStruct for the 3 heaviest mapping sites (wallet summary, listing response, payout rows) or remove the dependency.
### Risk
Low-Medium (serialization shape changes for entity-leaking endpoints — coordinate FE).
### Tests
Full suite + manual API smoke of `/wallet`, `/payment`.

## Phase 3 — Database under control

### Goal
Repeal `ddl-auto=update`.
### Files
new `src/main/resources/db/migration/V*__*.sql`, `pom.xml` (Flyway), `application*.properties`.
### Changes
Flyway baseline from current prod schema (`baseline-on-migrate`), then `ddl-auto=validate`.
### Risk
Medium (prod schema drift) — do a schema diff first; run on a staging DB.
### Tests
App boots against migrated schema; entity metadata validates.

## Phase 4 — Package-by-feature reorganization

### Goal
Structure of §14 without logic changes.
### Files
~all (mechanical moves only — imports, package lines; no method bodies).
### Changes
Move classes feature-by-feature (one PR per feature: wallet → payment → marketplace → …), starting with the 4 sanctioned cross-call clusters; delete `utils/Tuong` after envelope unification; split `AppConfig` into SecurityConfig/CorsConfig/AsyncConfig.
### Risk
Medium (large diffs, zero semantic change) — keep PRs per-feature, rely on compile+tests.
### Tests
Compile + suite after each feature PR.

## Phase 5 — Targeted decompositions

### Goal
Kill the specific god-method/hacks.
### Files
`MarketplaceServiceImpl.listCreditsForSale` (extract validate/resolve/build steps — pure refactor, covered by the 8 existing tests); `ProfitSharingServiceImpl` (replace `getSelf()` with `TransactionTemplate` or new bean); `GeminiAiScoringService` (extract `DataQualityStatistics` + `ScoringProperties`); `MyCreditServiceImpl` (extract retirement + notifications); `WalletServiceImpl` (extract `WalletSummaryAssembler`, remove write-in-read side effect).
### Risk
Low-Medium; each extraction independently shippable.
### Tests
New unit tests for extracted pure components (statistics, pricing, payout math).

## Phase 6 — Guardrails & CI

### Goal
Prevent regression to the audited state.
### Changes
GitHub Actions: build + test + (later) Flyway migrate-check on H2/MySQL container; ArchUnit rules encoding §13 forbidden dependencies + "no jakarta.transaction import" + "no System.out"; optional Checkstyle guard for package cycles.
### Risk
Low.
### Tests
The guardrails themselves are tests.

---

# 16. Refactoring Priority

| Item | Impact | Risk | Effort | Priority | Reason |
| ---- | ------ | ---- | ------ | -------- | ------ |
| Fix deposit verification (§5.1) | Critical | Med | M | P0 | Free money on deployed system |
| Fix transferFunds balances (§5.2) | Critical | Low | S | P0 | Financial record corruption, trivial fix |
| Secrets rotation + junk removal (Phase 0) | Critical | Low | S | P0 | Active leak in repo |
| Wallet locking in settlement (§5.5) | High | Med | M | P1 | Double-spend |
| ERROR-status rollback fix (§5.6) | High | Low | S | P1 | Orders stuck PENDING |
| Money-path tests (Phase 1) | High | Low | M | P1 | Enables everything after |
| Finish @Transactional migration (§5.9) | Med | Low | S | P2 | Consistency in money code |
| anyRequest→authenticated (§5.4) | Med | Low | S | P2 | Default-open posture |
| Envelope/entity-leak cleanup (§5.10) | Med | Low | M | P2 | API contract sanity |
| Flyway (Phase 3) | High | Med | M | P2 | Schema is unmigratable today |
| Package-by-feature (Phase 4) | Med | Med | L | P3 | Team velocity, boundaries |
| God-method/class splits (Phase 5) | Med | Low-Med | M | P3 | Reviewability |
| ArchUnit + CI (Phase 6) | High | Low | S-M | P3 | Locks in all previous phases |
| Mapper decision (MapStruct) | Low | Low | S | P4 | Dead dependency |
| AI stack consolidation | Low-Med | Low | M | P4 | Two Gemini stacks |

---

# 17. Quick Wins

## Fix `transferFunds` credit-side balances

### Why
Two-line fix (§5.2); restores trustworthy history.
### Estimated Effort
30 min + test.
### Risk
None.

## Rotate the Gmail app password & scrub compose secrets

### Why
Committed live credential (§6 Security Debt).
### Effort
1 h (incl. checking deployment env).
### Risk
None (operational).

## Delete FileUploadTestController + flip `anyRequest` to `authenticated`

### Why
Test upload endpoint in prod; default-open chain (§5.4).
### Effort
30 min.
### Risk
Low — verify FE doesn't rely on any non-`/api` public endpoint (grep nginx/FE config).

## Generic 500 message in GlobalExceptionHandler

### Why
Stops exception-detail leakage (§5.10).
### Effort
15 min.
### Risk
None.

## Remove duplicate pom dependencies + rename artifact

### Why
Two literal duplicates; template identity (§6 Dependency Debt).
### Effort
30 min.
### Risk
None.

## Delete repo-root junk files & `resources/logs`

### Why
Repo hygiene; smaller jars.
### Effort
30 min.
### Risk
None (verified unused).

---

# 18. Long-Term Improvements

- Replace PayPal `rest-api-sdk` with Checkout SDK; upgrade jjwt to 0.12 API.
- Consolidate AI stacks behind one `AiClient` port; make scoring async with result polling/SSE.
- Pagination for `getTransactions()` (currently loads full history per wallet view).
- Actuator + Micrometer + MDC trace propagation (the X-Request-Trace header exists — thread it through logs).
- `@ConfigurationProperties` for trading fee, profit-sharing, AI thresholds (replaces scattered `@Value` and `getDoubleProperty`).
- Redis beyond OTP (listing pages, dashboards) — only after measuring with the p6spy setup already in place.
- Seed demo data behind a `demo` profile, disabled in prod deployment.
- Frontend audit (out of scope here): `apiCVA`/`apiAdmin` duplication, token storage strategy — flagged for a separate pass.

---

# 19. Architecture Guardrails

## ArchUnit
Rules to encode (Phase 6): feature packages may not access another feature's `repository`/`model`; `..service..` may not use `SecurityContextHolder`; no class may import `jakarta.transaction.Transactional`; controllers return types only from `..api..`/`shared`.

## Static Analysis
Checkstyle (import bans, file length warning at 700 LOC) + SpotBugs later; not adopted now to avoid tool churn before Phases 0–2.

## Dependency Rules
Documented in §13; ArchUnit is the enforcement mechanism; code review checklist mirrors it.

## CI Checks
GitHub Actions on PR: `mvn -q verify` (unit tests), later + Flyway validate on MySQL service container + ArchUnit suite. Currently **no CI exists at all** — this is the single highest-leverage guardrail.

---

# 20. Final Recommendation

## What Should Be Changed
Execute Phases 0–1 immediately (secrets, free-money deposit, ledger bug, wallet locking, order status semantics) — these are correctness/security fixes the current architecture already supports. Then Phases 2–3 (consistency + Flyway), then the structural work (4–6).

## What Should NOT Be Changed
- The pessimistic-lock settlement skeleton (`completeOrder` shape).
- `transferFunds` design (only its bug).
- VNPay HMAC verification flow.
- The fetch-join repository approach from the current branch.
- Single-deployable monolith (no microservices/multi-module).
- Service-interface-per-feature style (Rule G — do not add more interfaces than the 40 existing).

## First Refactoring Step
Phase 0 + the two-line `transferFunds` fix + ownership check in `/wallet/deposit` as one PR — smallest safe set that stops the bleeding while the full verification fix (Phase 1) is built.

## Expected End State
A single-module package-by-feature modular monolith with verified payment callbacks, locked wallet mutations, Flyway-managed schema, one response envelope, ArchUnit+CI guardrails, and tests concentrated on the money paths — with the existing domain logic and locking discipline preserved.

---

# 21. Evidence & Sources

## Repository evidence
All file:line references above were read directly on branch `refactor/cleancode` (working tree including uncommitted N+1 optimization diff) on 2026-08-16. Key files fully read: `AppConfig`, `OrderServiceImpl`, `WalletServiceImpl`, `WalletTransactionServiceImpl`, `PaymentServiceImpl`, `VNPayService`, `PaymentController`, `VNPaymentController`, `WalletController`, `GlobalExceptionHandler`, `Wallet`, `CarbonCredit`, `MarketPlaceListing`, `Order`, `pom.xml`, `docker-compose.yml`, `logback-spring.xml`, `ConcurrencyConfig`, `CreditExpiryScheduler`, `DataInitializer`, `FileUploadTestController`, method-level skim of the five largest services. Grep evidence: jakarta vs Spring `@Transactional` counts (16/7), `System.out.println` (10 sites), Redis usage (1 file), MapStruct disabled (2 files), idempotencyKey unused, test counts (4 files / 26 tests).

## External sources

| # | Name | URL | What was learned | Where it influenced |
|---|------|-----|------------------|---------------------|
| 1 | Spring Framework Reference — Using @Transactional | https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html | Jakarta vs Spring annotation capabilities; proxy self-invocation | §5.9, §5.11 getSelf(), Phase 2 |
| 2 | Baeldung — Spring vs JTA @Transactional | https://www.baeldung.com/spring-vs-jta-transactional | Concrete attribute differences (readOnly/isolation/NESTED) | §5.9 |
| 3 | Stripe — Webhooks official docs | https://docs.stripe.com/webhooks | Signature verification + idempotent webhook handling | §5.1, §5.3, Phase 1 |
| 4 | Hookdeck — PayPal webhooks guide | https://hookdeck.com/webhooks/platforms/guide-to-paypal-webhooks-features-and-best-practices | PayPal verification API & event-id idempotency | §5.3 |
| 5 | Stigg — Stripe webhook best practices | https://www.stigg.io/blog-posts/best-practices-i-wish-we-knew-when-integrating-stripe-webhooks | Persist events; return 200 fast; process async | Phase 1 design |
| 6 | Medium — Handling payment webhooks reliably | https://medium.com/@sohail_saifii/handling-payment-webhooks-reliably-idempotency-retries-validation-69b762720bf5 | Checklist: verify→idempotency→transaction→monitor | Phase 1 design |
| 7 | Spring Modulith (project + write-ups) | https://spring.io/projects/spring-modulith ; https://www.codecentric.de/en/knowledge-hub/blog/modularization-the-easy-way-spring-modulith-with-kotlin-and-hexagonal-architecture ; https://notes.softwarearchitect.id/p/combining-modular-monolith-and-hexagonal | Package-by-feature modules with verified boundaries in one deployable | §12–§14 |
| 8 | Ensolvers — Comparative architectures with Spring Boot | https://www.ensolvers.com/post/comparative-guide-to-application-architectures-with-spring-boot-multi-module-hexagonal-and-microservices | Modular monolith first; split only on real pain | §12 (why not multi-module) |
| 9 | foojay — Hexagonal approach in modular Java | https://foojay.io/today/clean-and-modular-java-a-hexagonal-architecture-approach/ | Ports/adapters scoped to genuine infra concerns | §13 payment/ai ports |
| 10 | spring-petclinic, Keycloak, JHipster (established references) | https://github.com/spring-projects/spring-petclinic ; https://github.com/keycloak/keycloak ; https://www.jhipster.tech/ | Canonical layer layout; feature-module monolith at scale; testing/CI/Flyware baseline conventions | §10 Ref 4, §11 |

Claims marked UNKNOWN: none outstanding — all Section 5/6 findings carry file:line evidence; javac-level package cycle analysis (§9) is the only area noted as low-confidence.
