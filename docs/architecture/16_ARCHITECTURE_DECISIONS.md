# 16 — Architecture Decision Records

## ADR-001 — Adopt single-module modular monolith with package-by-feature

- **Context**: 367-file package-by-layer Spring Boot app, 4-person team, one VPS deployment, 10+ separable domains (01).
- **Problem**: features scattered across 5+ technical packages; boundaries unenforced; merge friction.
- **Options considered**: keep package-by-layer · multi-module Maven · microservices · full Clean/Hexagonal · **modular monolith package-by-feature (single module)**.
- **Decision**: modular monolith, package-by-feature, layered internals per feature; ports only for payments/AI/storage.
- **Why**: mechanical migration, test-verified boundaries, zero build overhead; matches Spring Modulith guidance and repo seams (08 comparison table).
- **Trade-offs**: no compiler-enforced privacy across features (ArchUnit compensates); port layers exist in only 3 places (accepted asymmetry).
- **Consequences**: Phase 4 reorg; sanctioned cross-call list becomes law; ArchUnit in Phase 6.
- **Evidence / Sources**: 06 §F3 (Spring Modulith, Ensolvers, codecentric), 07, 08.
- **Status**: Proposed (activate with roadmap approval).

## ADR-002 — Payments become server-verified via gateway port + webhooks

- **Context**: deposit confirmation trusts the client (`PaymentServiceImpl:126`); no webhooks; VNPay return flips status only.
- **Problem**: free-money endpoint (13 SC1) + unverifiable payment state.
- **Options considered**: keep client-driven flow + add ownership check only (rejected: still spoofable) · full payment orchestration service (overkill) · **`PaymentGateway` port per provider with `verifyPayment` + webhook endpoints + single-transaction crediting**.
- **Decision**: the port + webhook model; `DepositService` in wallet module credits exactly once on verified payment.
- **Why**: matches official Stripe/PayPal webhook guidance; the `findByIdWithLock` + status-check idempotency base already exists.
- **Trade-offs**: requires FE change (poll status instead of calling deposit); extra round-trip (R10, measured in Phase 1).
- **Consequences**: Phase 1 scope; feature flag during soak.
- **Evidence / Sources**: 06 §F2 (Stripe docs, PayPal guide, idempotency articles).
- **Status**: Proposed.

## ADR-003 — Standardize on Spring's `@Transactional`

- **Context**: 16 files jakarta vs 7 Spring (grep); money paths on jakarta; `readOnly` unusable there; `getSelf()` hack exists because of proxy semantics.
- **Options considered**: keep jakarta everywhere (loses readOnly/isolation/NESTED) · **migrate all to `org.springframework.transaction.annotation.Transactional`**.
- **Decision**: migrate; ban jakarta import via ArchUnit/Checkstyle.
- **Why**: Spring reference documents the capability gap; the repo migration already began on this branch.
- **Trade-offs**: none meaningful for a non-JTA app.
- **Consequences**: Phase 2 mechanical sweep; rule enforced Phase 6.
- **Evidence / Sources**: 06 §F1 (Spring reference, Baeldung).
- **Status**: Proposed.

## ADR-004 — Introduce Flyway; retire `ddl-auto=update`

- **Context**: schema by Hibernate update; manual `core_ccm.sql` dump drifting; no migration history.
- **Options considered**: keep update+dump · **Flyway with baseline-on-migrate** · Liquibase.
- **Decision**: Flyway; baseline from live prod schema; switch to `validate`.
- **Why**: JHipster baseline practice (06 R4); enables reviewable schema changes and CI validation.
- **Trade-offs**: cutover risk (R4) mitigated by staging rehearsal; XML-free SQL preferred over Liquibase at this size.
- **Consequences**: Phase 3; migration IT in CI.
- **Evidence / Sources**: 06 R4; docker-compose/db state (01).
- **Status**: Proposed.

## ADR-005 — Unify API response envelope

- **Context**: three shapes coexist (`TuongCommonResponse`, `CommonResponse`, ad-hoc Map in CSV handler); entity leaks in 2 controllers.
- **Options considered**: keep dual envelopes · **one `shared.api.ApiResponse`** (FE-coordinated migration).
- **Decision**: single envelope; DTO-only boundary; delete `utils/Tuong` after migration.
- **Why**: contract sanity; simplifies GlobalExceptionHandler; unblocks error-taxonomy work.
- **Trade-offs**: FE adapter changes (R6); transition may temporarily carry old fields.
- **Consequences**: Phase 2.
- **Evidence / Sources**: 04 §Code Quality; 07 gap table.
- **Status**: Proposed.

## ADR-006 — CI + ArchUnit guardrails

- **Context**: no CI; boundaries conventional; regressions invisible.
- **Options considered**: heavyweight SonarQube now (deferred — needs infra) · **GitHub Actions build+test + ArchUnit rule suite** (+Checkstyle import bans).
- **Decision**: Actions CI on PR; ArchUnit encodes 08's forbidden dependencies, jakarta-import ban, no-`System.out`-in-main.
- **Why**: cheapest enforcement of every prior ADR; Spring Modulith's own model is test-based boundary verification (06 R2).
- **Trade-offs**: rules start minimal and grow; CI runtime cost small.
- **Consequences**: Phase 6; PR gate.
- **Evidence / Sources**: 06 R2/R4.
- **Status**: Proposed.

## ADR-007 — MapStruct: enable for 3 hotspot mappers or remove

- **Context**: MapStruct configured in annotation processors but both `@Mapper`s are commented out → dead dependency with real mapping duplication.
- **Options considered**: leave as-is (worst) · **enable for wallet-summary, listing-response, payout-row mapping** · remove entirely.
- **Decision**: enable for the three duplication hotspots; if the team prefers builders after a spike, remove the dependency instead. Decide in Phase 2, not later.
- **Why**: duplication is measurable (same builder blocks repeated across `WalletServiceImpl`/`MarketplaceServiceImpl`/`CompanyPayoutQueryServiceImpl`); keeping a dead processor invites confusion.
- **Trade-offs**: annotation-processor build fragility vs hand-mapping verbosity.
- **Consequences**: Phase 2 decision gate.
- **Evidence / Sources**: `mapper/UserMapper.java:10`; 04 §Code Quality.
- **Status**: Proposed (with explicit decision point).

## ADR-008 — Preserve pessimistic-lock settlement; extend locking to wallets

- **Context**: settlement locks listing+credit but reads wallets unlocked (`OrderServiceImpl:214-227`); debit path mutates without lock (`WalletTransactionServiceImpl:67-100`).
- **Options considered**: optimistic `@Version` on Wallet (invasive: every writer) · **pessimistic locks on both wallets in a documented order** · atomic conditional UPDATE.
- **Decision**: lock both wallets ordered by id (or conditional UPDATE) inside the existing settlement transaction; document the global lock order (listing → credit → wallets-by-id).
- **Why**: builds on the proven G1/G2 patterns already in the codebase; Rule F satisfied by new concurrency tests.
- **Trade-offs**: coarser lock scope slightly reduces concurrent settlement throughput for the same wallet pair (acceptable: correctness first).
- **Consequences**: Phase 1; deadlock-risk register entry R2.
- **Evidence / Sources**: G1/G2 (03); Spring transactions research (06 §F1).
- **Status**: Proposed.
