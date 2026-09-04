# 03 — What Is Already Good

> Rule: never refactor strength for aesthetics. Each entry lists evidence and an explicit preservation contract.

## G1 — Pessimistic locking on the trade path

- **Component**: listing + credit locking in order settlement.
- **Evidence**: `MarketplaceListingRepository.findByIdWithPessimisticLock` / `findByIdWithPessimisticLockAndDetails` (`@Lock(PESSIMISTIC_WRITE)`), `CarbonCreditRepository.findByIdWithPessimisticLock`, `PaymentOrderRepository.findByIdWithLock`; used in `OrderServiceImpl.completeOrder:191-200` and `PaymentServiceImpl.processPaymentOrder:116`. Re-validation of quantity/balance happens *after* lock acquisition (`OrderServiceImpl:205-222`) — correct pattern.
- **Why it is good**: serializes concurrent buyers on the same listing — the hardest concurrency problem in a marketplace — at the DB level.
- **Why it matters**: prevents overselling credits.
- **Preserve**: lock-first-then-recheck ordering; lock-acquiring repository methods.
- **Classification**: **KEEP**

## G2 — Banking-style transfer with balance audit trail

- **Component**: `WalletServiceImpl.transferFunds` (lines 308-404).
- **Evidence**: dual `entityManager.find(Wallet.class, id, PESSIMISTIC_WRITE)`; funds check on locked read; `WalletTransaction` rows carry `balanceBefore`/`balanceAfter`.
- **Why it is good**: the exact shape needed for auditable money movement and dispute resolution.
- **Preserve**: locking order and audit fields. (Contains one copy-paste bug — fix, do not redesign; see 04/13.)
- **Classification**: **KEEP (bug fix)**

## G3 — VNPay HMAC-SHA512 signature verification

- **Evidence**: `VNPayService.orderReturn:94-123` verifies `vnp_SecureHash` over sorted fields before mutating order status.
- **Preserve**: verify-then-mutate ordering.
- **Classification**: **KEEP**

## G4 — Centralized exception handling with field-level messages

- **Evidence**: `GlobalExceptionHandler` (206 LOC) covering validation, security, type mismatch, malformed JSON, integrity violations; shared `buildErrorResponse`.
- **Preserve**: central handling; field-specific validation messages. (Fix 500-leak + envelope split — 04.)
- **Classification**: **IMPROVE**

## G5 — Security & platform hygiene above the average demo

- **Evidence**: Bucket4j rate limiting (`RateLimitConfig`, `RateLimitInterceptor`), BCrypt, JWT filter, `@EnableMethodSecurity`, OpenAPI/Swagger, OAuth2 authorization-request stored in cookies, and `DelegatingSecurityContextAsyncTaskExecutor` (`ConcurrencyConfig`) — the security-context propagation for `@Async` is a detail many production teams miss.
- **Classification**: **KEEP**

## G6 — DTO separation and validation

- **Evidence**: 85 request/response DTOs; `@Valid` at controllers (e.g. `PaymentController:34`); entities mostly absent from API contracts (exceptions: `PaymentOrder`, `EVOwner` returns — 04).
- **Classification**: **KEEP (extend to leaking endpoints)**

## G7 — Evidence-driven performance work already in flight (current branch)

- **Evidence**: uncommitted diff adds fetch-join repository methods (`findByCompanyIdWithDetails`, `findByStatusAndExpiresAtAfterWithDetails`, `OrderRepository.findByIdWithDetails`, lock+fetch variants), `@Transactional(readOnly=true)` read paths, p6spy integration; measured with k6 (3,802→2 queries on marketplace; see 12_PERFORMANCE_AUDIT).
- **Why it is good**: measure → fix → re-measure discipline; standard Hibernate 6 `@OneToOne(mappedBy)` N+1 workaround.
- **Classification**: **KEEP (finish the @Transactional migration repo-wide)**

## G8 — Scheduler and seeding patterns

- **Evidence**: `CreditExpiryScheduler` bulk `markExpiredCredits()` (no row scan); `DataInitializer` existence-checked idempotent seeding.
- **Classification**: **KEEP (gate demo accounts behind a profile)**

## G9 — Realistic multi-gateway payment surface

- **Evidence**: three gateways integrated (VNPay fully, Stripe/PayPal session creation), `PaymentOrder` state machine (PENDING/SUCCEEDED/FAILED), `findByIdWithLock` idempotency base.
- **Why it matters**: the *shape* needed for the fix is already present; correcting §P0-1 is a completion of intent, not a redesign.
- **Classification**: **IMPROVE**

## G10 — Existing unit-test beachhead

- **Evidence**: 26 Mockito tests across 4 services (Marketplace 8, CreditIssuance 8, EmissionReport 5, ProjectApplication 5), including the listing-creation path — the exact tests that will guard the Phase-5 refactor of `listCreditsForSale`.
- **Classification**: **KEEP (extend — see 14_TESTING_AUDIT)**

## DO NOT BREAK (contract for all future phases)

1. The lock-first settlement skeleton of `completeOrder` (locks on listing + source credit).
2. `transferFunds` design (dual locks + before/after audit fields) — only its credit-side field bug.
3. VNPay HMAC verification flow.
4. The fetch-join repository methods from the current N+1 branch.
5. Single-deployable monolith (no microservices/multi-module split — ADR-001).
6. The existing 26 tests must stay green in every phase.
7. Service-interface-per-feature style — do not add interfaces without a second implementation or a test boundary that needs it (Rule G of the roadmap).
