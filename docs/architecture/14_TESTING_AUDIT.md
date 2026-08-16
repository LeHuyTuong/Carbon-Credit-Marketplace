# 14 — Testing Audit

## Current Inventory

| Test class | Tests | Style | Quality notes |
|---|---|---|---|
| `MarketplaceServiceImplTest` | 8 | Mockito unit | covers listing creation path — will guard Phase-5 method extraction |
| `CreditIssuanceServiceImplTest` | 8 | Mockito unit | issuance rules |
| `EmissionReportServiceImplTest` | 5 | Mockito unit | report workflow |
| `ProjectApplicationServiceImplTest` | 5 | Mockito unit | application flow |
| **Total** | **26** | | for 367 production files (~7% file coverage) |

Test classes sit in the root package `com.carbonx.marketcarbon` (not mirrored per-package). No integration tests, no Testcontainers, no ArchUnit, no CI to execute anything.

## Business Criticality × Coverage Map

| Flow | Criticality | Coverage | Verdict |
|---|---|---|---|
| Deposit confirmation (payment verify → credit) | **Critical** | **None** | must be built in Phase 1 *before* the fix lands |
| Wallet transfer (both ledger sides) | **Critical** | **None** (and the untested side is the buggy one) | Phase 1 |
| Order settlement (locks, rollback, ERROR persistence) | **Critical** | None | Phase 1 |
| Wallet balance math (`createTransaction` debit/credit rules) | Critical | None | Phase 1 |
| Listing creation/validation | High | 8 tests | extend for extraction |
| Credit issuance/serials | High | 8 tests | keep green |
| Profit sharing payouts | High | None | Phase 5-6 |
| Withdrawal approval flow | High | None | Phase 1-2 |
| VNPay signature verify | High | None (pure logic — easy unit win) | Phase 1 |
| Emission verification workflow | Medium | 5 tests | adequate for now |
| KYC CRUD | Medium | None | low risk (CRUD) |
| AI scoring statistics | Medium | None (currently untestable without AI client) | Phase 5 enables it |
| Schedulers | Medium | None | small unit win |

## Identified Gaps by Type

- **Critical-path**: all money paths untested (table above).
- **Integration**: zero — no test verifies JPQL fetch-graphs, pessimistic locking, or Flyway migrations against a real MySQL.
- **Architecture**: zero — nothing prevents boundary violations (Phase 6 ArchUnit).
- **Regression**: no CI → the 26 existing tests run only on dev machines.
- **Excessive mocking risk**: existing tests are pure-Mockito; for Phase-1 concurrency fixes, mocks alone would not catch lost-update bugs (Risk R12) → at least one Testcontainers test per money flow required.
- **Flakiness**: none observed (small suite).

## Test Strategy Per Roadmap Phase

| Phase | Tests to add | Level |
|---|---|---|
| 0 | none (deletions only) — keep 26 green | — |
| 1 | DepositServiceTest (verify/ownership/idempotency); WalletServiceImplTest.transferFunds (both sides); OrderSettlementTest (insufficient funds, ERROR-out-of-band, fractional rejection, concurrent completion); VNPayServiceTest (HMAC tamper cases); **1 Testcontainers IT per money flow** | unit + first ITs |
| 2 | envelope contract snapshots; mapper tests (if MapStruct enabled) | unit |
| 3 | migration IT (boot + validate on MySQL container) | integration |
| 4 | smoke per moved feature (controller→service wiring) | integration-lite |
| 5 | pure-component tests: DataQualityStatistics, ListingValidator, payout math | unit |
| 6 | ArchUnit suite (08 rules) + GitHub Actions runs everything | architecture |

## Definition of Done (all phases)

Compile → unit tests → integration tests → ArchUnit (from Phase 6) → diff review. No phase completes with a red or skipped test.
