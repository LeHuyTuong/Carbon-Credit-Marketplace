# 29 — Revised Roadmap

> Rebuilt from the second-audit risk picture. Old Phase-4 (package migration) is **removed** (28). Priorities: P0 production blockers → P1 correctness/security/reliability → P2 operational/maintainability → P3 architecture improvement → P4 nice-to-have.

## Phase P0-A — Security Blockers Batch (small, surgical, high-value)

- **Objective**: eliminate every authentication/authorization/secret blocker that requires no behavioral redesign.
- **Why now**: all are remotely exploitable today on a public deployment; each fix is < 20 lines.
- **Evidence**: N1 (`AuthServiceImpl:59-63`), N2/N3 (`WithdrawalController:87+`, `WithdrawalServiceImpl:125-130`), N4 (`JwtConstant:4`), N5 (`application-prod.properties`, compose), N8 (`KycServiceImpl:235,320`), N9 (CvaDashboard), N10 (`/v1/ai` route), SC3 (AppConfig:81 + test controller), SC5 (500 leak).
- **Scope/Files**: AuthServiceImpl (role whitelist {EV_OWNER, COMPANY}); WithdrawalController/ServiceImpl (ADMIN `@PreAuthorize`; refund only if status==PENDING, under lock, with ledger row); JwtConstant→env `JWT_SECRET`; rotate ALL committed secrets (DB/SMTP/AWS/Stripe/PayPal) and purge via env injection; KycController/ServiceImpl (role gates on cva/admin create + list endpoints); CvaDashboardController (role gate); ChatAiController (drop `/v1/ai` mapping); delete FileUploadTestController; GlobalExceptionHandler (generic 500); `anyRequest().authenticated()`.
- **Risk**: Low-Medium. JWT secret rotation invalidates all tokens (users re-login — communicate). Withdrawal fix changes replay behavior (intended).
- **Dependencies**: none. **Tests**: authz unit tests for each gated endpoint (MockMvc with 4 roles × endpoints); withdrawal replay test.
- **Performance check**: n/a. **Security check**: the point of the phase; re-run matrix in 19 after.
- **Rollback**: per-commit revert; keep secret rotation (never roll back a rotation).
- **Acceptance**: register with role=ADMIN → 400; non-admin calls withdrawal admin → 403; 3× replayed rejection → single refund; forged token with old secret → 401; no secret strings in `git grep` of tracked files.

## Phase P0-B — Money Correctness Batch

- **Objective**: make deposit/settlement/transfer/withdrawal satisfy invariants I1-I5, I9.
- **Why now**: financial correctness is the product.
- **Evidence**: 20/21/23 (SC1, SC-2, N6, N1-concurrency, S2).
- **Scope**: `PaymentGateway` port + Stripe `Session.retrieve` / PayPal verify / VNPay-reuse adapters; ownership check; **single transaction** = lock payment order → verify → SUCCEEDED → credit wallet → ledger row (with `wallet_transaction.payment_order_id` FK, unique); `completeOrder` — move SUCCESS check under listing lock / lock order row; lock both wallets (by id) or atomic conditional UPDATE; ERROR status via `REQUIRES_NEW`; buyer-caller check on complete; reject fractional quantity at `createOrder` (`quantity.stripTrailingZeros().scale() <= 0`); fix `transferFunds` credit-side fields + one-time repair script for existing rows.
- **Risk**: Medium — live flows; concurrency changes (Rule F ⇒ tests first).
- **Dependencies**: P0-A (env plumbing); FE coordination (deposit confirmation becomes status-polling; feature flag `payment.verify.enabled`).
- **Tests** (written first, expected to fail before fix): 23's concurrency suite (double-complete, concurrent deposits, buyer double-spend), deposit ownership/idempotency, transferFunds both-sides assertions, fractional rejection, ERROR persistence.
- **Performance check**: settlement p6spy before/after (locks add negligible query count; watch p95 under k6).
- **Security check**: replay suite (tampered `paymentId`, cross-user `order_id`, stale webhook-like re-post).
- **Rollback**: feature flag for verification path; code revert otherwise.
- **Acceptance**: all new tests green; reconciliation queries (21) return empty on a fresh seeded DB after running the full money-flow scenario.

## Phase P1 — Safety Net (CI + tests + reconciliation)

- **Objective**: never regress silently again.
- **Why now**: P0 fixes must be protected; CI also runs the authz suite.
- **Scope**: GitHub Actions (build + unit + Testcontainers MySQL IT); Dependabot on; daily reconciliation queries as job + alert; Actuator health + MDC trace filter; graceful shutdown + non-root Dockerfile; fix compose reference (nginx `/api` proxy, Vite env) or remove.
- **Tests**: the CI itself; smoke E2E of money path against seed data.
- **Rollback**: n/a. **Acceptance**: red PR blocked; reconciliation alert fires in a drill.

## Phase P2 — Consistency & Schema

- **Objective**: one transaction annotation, one envelope, Flyway, pagination.
- **Scope**: jakarta→Spring sweep + `readOnly`; envelope unification (FE-coordinated); MapStruct decide (ADR-007); Flyway baseline from **live** prod dump (22 procedure) + `validate` + indexes (six from 22) + CHECK/UNIQUE constraints (I1/I3 backstops); paginate `getTransactions`; `PaymentOrderRequest.amount` → BigDecimal; `currentPrice` → BigDecimal (migration).
- **Risk**: Medium (schema + API shape). **Rollback**: Flyway repair / feature flag on envelope.
- **Acceptance**: `git grep jakarta.transaction.Transactional src/main` = 0; boot with `validate`; envelope snapshot tests.

## Phase P3 — Targeted Structure (only where tests exist)

- **Scope**: decompositions from 27 in this order: PaymentServiceImpl (gateway adapters — may partly land in P0-B), WalletServiceImpl (DepositService + SummaryAssembler), MarketplaceServiceImpl (listCreditsForSale steps), ProfitSharingServiceImpl (distribution restructure + TransactionTemplate), MyCreditServiceImpl (RetirementService), GeminiAiScoringService (statistics/prompt/config), AI stacks → one `AiClient` port.
- **Acceptance**: existing + new tests green; no `ApplicationContext` self-injection; `listCreditsForSale` ≤ 40 lines.

## Phase P4 — Nice-to-have

Dependency upgrades (jjwt 0.12, PayPal SDK replacement completion, Spring Boot patch line), CSV/PDF stack consolidation, Redis caching where measured, dashboard/alerts beyond minimum, optional package-by-feature **only if** 28's revisit triggers fire.

## Removed / Deferred vs First-Audit Roadmap

| Old item | Disposition |
|---|---|
| Phase 4 package-by-feature reorg | **Removed** (28; deferred behind explicit triggers) |
| `utils/Tuong` dissolution | Kept but folded into P2 envelope work |
| ArchUnit suite | Kept — moved earlier conceptually (rules grow from P1 onward on current package names) |
| Everything else | Re-sequenced around the 9 blockers; money tests now precede money fixes (Rule F) |
