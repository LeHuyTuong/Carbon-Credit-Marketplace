# 10 — Refactoring Roadmap

> Principles: one concern per phase; every phase compiles, tests green, deployable, revertible. Small change → compile → test → verify → commit → next. Rule F: no concurrency change without tests. Rule G: no optimization without measurement.

## Phase 0 — Hygiene & Secrets

- **Objective**: stop active leaks; remove noise.
- **Scope**: repo-level files + one controller + one handler.
- **Files**: `docker-compose.yml`, `.gitignore`, root junk (`daq`, `et --hard f55ba43`, `git log --graph --oneline --decorate --all`, `how --stat 86a9b8f`, `t --rebase`, `e -i HEAD~3`, `git`, `img_1.png`, `img_2.png`, stray `package-lock.json`, `uploads/`), `controller/FileUploadTestController.java`, `exception/GlobalExceptionHandler.java` (500 handler), `backend/Market_carbon/logs/`, `src/main/resources/seed_large.py` (→ scripts/).
- **Dependencies**: none.
- **Expected outcome**: no secrets in repo; no test endpoints; generic 500 body.
- **Risk**: Low. Junk deletions verified unused by grep.
- **Testing strategy**: compile + existing 26 tests; manual smoke of one error path.
- **Benchmark strategy**: n/a.
- **Deployment considerations**: rotate Gmail app password **before** merging; redeploy compose with env-var credentials.
- **Rollback strategy**: `git revert` per commit.
- **Acceptance criteria**: `git log -p` shows no credential strings; `/api/test/**` 404s; 500 body contains no exception text.

## Phase 1 — Money-Path Correctness (highest value)

- **Objective**: close P0/P1 bugs SC1 (deposit), S2 (ERROR rollback), N1 (wallet double-spend), N2 (idempotency), C7-adjacent fractional bug (`intValue`).
- **Scope**: payment confirmation flow + order settlement + transferFunds.
- **Files/modules**: `PaymentServiceImpl`, `WalletController`, `WalletServiceImpl`, `WalletTransactionServiceImpl`, `OrderServiceImpl`, new `PaymentGateway` port + 3 adapters (Stripe `Session.retrieve`, PayPal verify, VNPay reuse HMAC), new `DepositService`.
- **Dependencies**: Phase 0 (secrets rotated so new env plumbing exists).
- **Expected outcome**: deposits credited only on server-verified payments owned by the caller, exactly once; settlement rolls back atomically while persisting ERROR out-of-band; both settlement wallets locked; fractional quantities rejected or fully honored in BigDecimal.
- **Risk**: Medium — live flows. Mitigated by new tests + feature flag `payment.verify.enabled` during soak.
- **Testing strategy (new tests, written first)**: `DepositServiceTest` (verify ok/fail, ownership, idempotent double-call), `WalletServiceImplTest.transferFunds` (assert **both** sides' balanceBefore/After), `OrderSettlementTest` (insufficient funds, rollback persists ERROR via REQUIRES_NEW, concurrent completion with mocked locks, fractional rejection).
- **Benchmark strategy**: settlement p6spy query count before/after (expect no regression from locking).
- **Deployment considerations**: coordinate with frontend — success page switches to polling `GET /payment/{id}` status instead of calling deposit endpoint; deploy BE first (flag off), then FE, then enable flag.
- **Rollback strategy**: disable feature flag; revert phase commit.
- **Acceptance criteria**: replaying a deposit call twice credits once; cross-user `order_id` rejected 403; failed settlement leaves order ERROR and balances untouched; all new tests green in CI.

## Phase 2 — Consistency Pass

- **Objective**: one transaction annotation, one envelope, DTO-only boundary, mapper decision.
- **Scope**: 16 jakarta-`@Transactional` files; `PaymentController`/`KycController` returns; `mapper/*`; CSV handler.
- **Expected outcome**: `org.springframework...@Transactional` everywhere (`readOnly` on reads); single `ApiResponse` envelope; MapStruct enabled for 3 hottest mapping sites (wallet summary, listing response, payout rows) or dependency removed.
- **Risk**: Low-Medium (response shape changes on entity-leaking endpoints — FE coordination).
- **Testing strategy**: full suite + contract snapshots of changed endpoints.
- **Rollback**: revert.
- **Acceptance criteria**: `grep -r "jakarta.transaction.Transactional" src/main` → 0; no JPA entity in any controller return signature.

## Phase 3 — Database Under Control

- **Objective**: replace `ddl-auto=update` with Flyway.
- **Scope**: new `db/migration/` + `pom.xml` + profiles.
- **Files**: `V1__baseline.sql` (generated from prod schema diff vs `core_ccm.sql`), `application*.properties` → `validate`.
- **Dependencies**: Phase 1 (schema stable before freezing).
- **Expected outcome**: versioned, reviewable schema changes.
- **Risk**: Medium (prod drift). Mitigation: `baseline-on-migrate=true`, staging run, schema diff first.
- **Testing strategy**: boot against migrated schema in CI (MySQL service container).
- **Rollback**: flyway repair/revert + `validate→update` flag as emergency.
- **Acceptance criteria**: app boots with `ddl-auto=validate` against migrated DB; `flyway info` clean.

## Phase 4 — Package-by-Feature Reorganization

- **Objective**: target structure of 09 with zero behavior change.
- **Scope**: mechanical class moves, one feature per PR (order: `wallet` → `payment` → `marketplace` → `credit` → `profitsharing` → `emission` → `ai` → rest).
- **Expected outcome**: feature packages with api/service/repository/model; `utils/Tuong` dissolved; `AppConfig` split (SecurityConfig/CorsConfig/AsyncConfig).
- **Risk**: Medium (large diffs, no semantic change). Mitigation: compile+tests after each PR; no method-body edits in the same PR.
- **Testing strategy**: suite green per PR; smoke endpoints of moved feature.
- **Rollback**: revert feature PR.
- **Acceptance criteria**: package tree matches 09 for migrated features; imports reference only sanctioned cross-feature services.

## Phase 5 — Targeted Decompositions & Ports

- **Objective**: kill the specific god-methods/hacks; isolate infra.
- **Scope**: `MarketplaceServiceImpl.listCreditsForSale` step extraction (guarded by existing 8 tests); `ProfitSharingServiceImpl.getSelf()` → `TransactionTemplate`; `GeminiAiScoringService` → `DataQualityStatistics` + `ScoringPromptBuilder` + `ScoringProperties`; `MyCreditServiceImpl` → `RetirementService`; `WalletServiceImpl` → `WalletSummaryAssembler` (removes write-in-read side-effect at `:179-180`); consolidate AI stacks behind `AiClient`.
- **Risk**: Low-Medium; each extraction independently shippable.
- **Testing strategy**: new unit tests for extracted pure components; existing tests stay green.
- **Acceptance criteria**: `listCreditsForSale` ≤40 lines; no `ApplicationContext` self-injection; statistics unit-tested without network.

## Phase 6 — Guardrails & CI

- **Objective**: prevent regression to the audited state.
- **Scope**: `.github/workflows/ci.yml` (build+test on PR, later Flyway validate + ArchUnit), `ArchUnitTest` encoding 08's forbidden dependencies + no-jakarta-Transactional + no `System.out` in main.
- **Risk**: Low.
- **Acceptance criteria**: PR without green CI cannot merge; ArchUnit fails on cross-feature repository access in a spike test.

## Explicitly NOT in scope (recorded to resist scope creep)

Microservices, multi-module Maven, full DDD/CQRS/event-sourcing, rewriting services that are merely large-but-cohesive (`EmissionReportServiceImpl`, `KycServiceImpl`, `CreditIssuanceServiceImpl`), frontend redesign.
