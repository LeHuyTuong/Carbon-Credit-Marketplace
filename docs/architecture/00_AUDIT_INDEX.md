# Architecture Audit Index — CarbonX (carbon-credit-marketplace)

> Generated: 2026-08-16 · Branch audited: `refactor/cleancode` (including uncommitted N+1-optimization diff)
> Method: full read of money-path code + structural analysis of all packages + external best-practice research + measured performance baselines
> Audit mode: READ-ONLY — **no application source code was modified**

This is the entry point of the architecture audit. Each document is independently readable.
The multi-file set supersedes the earlier consolidated `docs/ARCHITECTURE_AUDIT.md` (kept for reference; same findings).
**Second audit (17–30) challenges and partially revises the first audit — read `17_SECOND_AUDIT.md` before acting on any first-audit recommendation.**

## Document Map

| # | Document | Content | Status |
|---|----------|---------|--------|
| 01 | [REPOSITORY_MAP](01_REPOSITORY_MAP.md) | Tech stack, modules, entry points, deps, deployment, tree | Complete |
| 02 | [CURRENT_ARCHITECTURE](02_CURRENT_ARCHITECTURE.md) | Actual architecture classification + flows + diagrams | Complete |
| 03 | [WHAT_IS_GOOD](03_WHAT_IS_GOOD.md) | Strengths to preserve, DO-NOT-BREAK list | Complete |
| 04 | [TECHNICAL_DEBT](04_TECHNICAL_DEBT.md) | Debt by category, evidence-based | Complete |
| 05 | [LARGE_FILES_AND_CLASSES](05_LARGE_FILES_AND_CLASSES.md) | God-class analysis, split recommendations | Complete |
| 06 | [BEST_PRACTICE_RESEARCH](06_BEST_PRACTICE_RESEARCH.md) | External research + reference repositories | Complete |
| 07 | [ARCHITECTURE_COMPARISON](07_ARCHITECTURE_COMPARISON.md) | Current vs reference practice, gap table | Complete |
| 08 | [TARGET_ARCHITECTURE](08_TARGET_ARCHITECTURE.md) | Alternatives compared, decision, trade-offs | Complete |
| 09 | [PACKAGE_AND_MODULE_DESIGN](09_PACKAGE_AND_MODULE_DESIGN.md) | Target package tree + dependency rules | Complete |
| 10 | [REFACTORING_ROADMAP](10_REFACTORING_ROADMAP.md) | 7 production-safe phases with acceptance criteria | Complete |
| 11 | [RISK_REGISTER](11_RISK_REGISTER.md) | Risk entries with mitigation/detection/rollback | Complete |
| 12 | [PERFORMANCE_AUDIT](12_PERFORMANCE_AUDIT.md) | Measured baselines (k6 + p6spy), remaining work | Complete |
| 13 | [SECURITY_AUDIT](13_SECURITY_AUDIT.md) | Security findings with severity + evidence | Complete |
| 14 | [TESTING_AUDIT](14_TESTING_AUDIT.md) | Criticality vs coverage, test strategy per phase | Complete |
| 15 | [OBSERVABILITY_AUDIT](15_OBSERVABILITY_AUDIT.md) | Signal-by-signal gaps and targets | Complete |
| 16 | [ARCHITECTURE_DECISIONS](16_ARCHITECTURE_DECISIONS.md) | ADR-001..ADR-008 | Complete — **re-assessed in 28** |

## Second Audit (adversarial) — documents 17–30

| # | Document | Content | Status |
|---|----------|---------|--------|
| 17 | [SECOND_AUDIT](17_SECOND_AUDIT.md) | Audit-of-the-audit: verdicts on every P0/P1, new findings N1-N18, corrections C1-C5 | Complete |
| 18 | [PRODUCTION_READINESS](18_PRODUCTION_READINESS.md) | 21-category scoring + BLOCKER matrix + verdict | Complete |
| 19 | [AUTHORIZATION_AUDIT](19_AUTHORIZATION_AUDIT.md) | Full endpoint×role×ownership matrix | Complete |
| 20 | [PAYMENT_STATE_MACHINE_AUDIT](20_PAYMENT_STATE_MACHINE_AUDIT.md) | Actual vs target payment states, mandated Q&A | Complete |
| 21 | [MONEY_INVARIANTS_AUDIT](21_MONEY_INVARIANTS_AUDIT.md) | 12 invariants, violations, reconciliation queries | Complete |
| 22 | [DATABASE_SCHEMA_AUDIT](22_DATABASE_SCHEMA_AUDIT.md) | Dump vs entity drift, missing indexes/constraints | Complete |
| 23 | [CONCURRENCY_TRANSACTION_AUDIT](23_CONCURRENCY_TRANSACTION_AUDIT.md) | Flow-by-flow tx maps, lock analysis | Complete |
| 24 | [DEPENDENCY_SUPPLY_CHAIN_AUDIT](24_DEPENDENCY_SUPPLY_CHAIN_AUDIT.md) | pom findings, scanner gap | Complete |
| 25 | [DEPLOYMENT_RUNTIME_AUDIT](25_DEPLOYMENT_RUNTIME_AUDIT.md) | Dockerfile/compose/nginx survival scenarios | Complete |
| 26 | [BACKUP_RECOVERY_AUDIT](26_BACKUP_RECOVERY_AUDIT.md) | UNKNOWN-honest DR assessment + baseline | Complete |
| 27 | [LARGE_CLASS_DEEP_AUDIT](27_LARGE_CLASS_DEEP_AUDIT.md) | Method-level verdicts for 12 classes | Complete |
| 28 | [ARCHITECTURE_REASSESSMENT](28_ARCHITECTURE_REASSESSMENT.md) | Package-by-feature challenged → **deferred**; layer-hardening rules | Complete |
| 29 | [REVISED_ROADMAP](29_REVISED_ROADMAP.md) | P0-A/P0-B/P1/P2/P3/P4 rebuilt around blockers | Complete |
| 30 | [OPEN_QUESTIONS](30_OPEN_QUESTIONS.md) | Business-intent Q1-Q7 + technical UNKNOWNs Q8-Q17 | Complete |
| — | [P0_A_IMPLEMENTATION](P0_A_IMPLEMENTATION.md) | **Implementation phase P0-A** — 10 blockers fixed, 26 regression tests, scope/diff classification | Complete |
| — | [P0_A_SECRET_ROTATION](P0_A_SECRET_ROTATION.md) | Rotation checklist (code remediation ≠ remediation) | Pending external action |

## Reading Order

- **"What is the state of the system?"** → 01, 02, 03
- **"What is wrong and why?"** → 04, 05, 12, 13, 14, 15
- **"What should it become and how do we get there safely?"** → 06, 07, 08, 09, 10, 11, 16

## Top Findings (TL;DR — updated by the second audit)

**Second-audit verdict: NOT PRODUCTION-READY** (see 18). Nine blockers, several newly discovered:

1. **N1** Self-service admin: registration accepts any role including ADMIN (`AuthServiceImpl:59-63`).
2. **N2/N3** Withdrawal admin endpoints lack role checks AND the rejection refund is replayable → infinite mint (`WithdrawalServiceImpl:125-130`).
3. **N4** JWT signing secret hardcoded and committed — tokens forgeable for any account (public repo).
4. **N5** Production credentials (Stripe/PayPal/AWS/DB/SMTP) committed in `application-prod.properties`.
5. First-audit P0s re-confirmed: unverified deposit crediting, `transferFunds` ledger corruption, buyer-wallet double-spend, order ERROR lost to rollback.
6. **N6** double-settlement race (idempotency check outside the lock); **N7** any company can complete another's order.
7. No CI, no backups (UNKNOWN at best), `ddl-auto=update`, zero money-path tests.

**Architecture verdict (28): KEEP package-by-layer, hardened with ArchUnit rules — package-by-feature DEFERRED** behind explicit triggers; ports/adapters for payments/AI retained.

## Status of Phases (per execution plan)

| Phase | Status |
|-------|--------|
| 0–12 First audit (docs 01-16) | ✅ Complete |
| Second audit (docs 17-30) | ✅ Complete |
| 13 Implementation | ⛔ Not started — begin with **P0-A security batch** (29) after roadmap approval |
| 14–16 Testing / Verification / Re-audit | ⛔ Not started |
