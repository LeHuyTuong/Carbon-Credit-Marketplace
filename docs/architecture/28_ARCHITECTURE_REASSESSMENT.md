# 28 — Architecture Reassessment (challenging package-by-feature)

## The Mandated Hypothesis Test

> *Can all meaningful problems identified in the audits be solved while keeping package-by-layer?*

Walking every first/second-audit finding:

| Problem | Requires package migration? | Solvable in package-by-layer? |
|---|---|---|
| SC1 unverified deposits | No | Yes — service/port refactor is orthogonal to packages |
| N1 role-at-register, N2/N3 withdrawal escalation | No | Yes — annotations + service checks |
| Ledger bug, double-settlement race, wallet locks | No | Yes — method/transaction level |
| Secrets, CI, tests, backups | No | Yes |
| Envelope/entity leaks, @Transactional mix | No | Yes |
| Flyway/schema | No | Yes |
| Cross-domain coupling (Order→WalletTx/CreditIssuance; SecurityContextHolder in 6 services) | **No** — enforceable via ArchUnit on the *existing* packages | Yes — rules like "service.impl..(Order) must not access repository.WalletRepository" are nameable today |
| `impl` dumping ground / scattered feature code (merge friction, discoverability) | **This is the only cluster that package-by-layer cannot fix by rules** — but it is a *developer-velocity* concern, not correctness | Partially: conventions + ArchUnit reduce it; the scatter itself remains |

**Conclusion: YES — every correctness, security, reliability, testing, and observability problem is solvable without changing the package axis.** The only things package-by-feature buys are merge-conflict reduction and feature discoverability: real, but unproven pains for this team (4 people, feature-owned areas already de facto), and they come with a real migration cost (large diffs during the riskiest months of the project).

## Options Compared

| Criterion | A: keep layer as-is | B: layer + hardening rules | C: package-by-feature | D: hybrid (feature for NEW code only) |
|---|---|---|---|---|
| Migration cost | 0 | ~0 (rules as tests) | High (whole-repo move) | Low-medium (drift) |
| Merge conflict reduction | none | partial (conventions) | high | medium |
| Discoverability | low | low-medium | high | medium |
| Dependency enforcement | none | **ArchUnit on current names** | ArchUnit on feature names | ArchUnit (two shapes) |
| Testability | unchanged | improved (rules) | unchanged | unchanged |
| Team size fit (4) | fits | fits | acceptable | fits |
| Production risk now | 0 | ~0 | nonzero during P0/P1 window | low |
| Future growth | needs eventual move | rules travel with any future move | ready | halfway house |

## Revised Decision

**Option B now; package-by-feature demoted to a deferred, optional move (revisit trigger below).** This *reverses* the first audit's Phase 4 priority — recorded as AUDIT CORRECTION C2. Rationale:

1. The second audit found 9 blockers; none is architectural. Spending the team's scarce risk-budget on class moves while the money path is broken inverts priorities (Principle: production safety first).
2. ArchUnit rules on the existing package names capture the enforceable benefits (dependency direction, no cross-domain repository access, no SecurityContextHolder in services, no jakarta @Transactional) — provable, revertible, ~zero migration risk.
3. A future package-by-feature move, if ever made, is *easier* on top of enforced layer rules (classes already obey boundaries; moves stay mechanical).

**Revisit trigger for package-by-feature (any two):** team grows past ~6; per-feature change sets regularly touch 4+ top-level packages (measure over a quarter); onboarding time measurably hurt by scatter. Until then: no.

## What survives from the first audit's target design

- **Ports/adapters for payments and AI: keep** (ADR-002; driven by SC1/N-issues, not by aesthetics). They live as `payment/gateway`-style *sub-packages inside the current layer layout* if needed — the port is a dependency direction, not a directory scheme.
- **ADR-003/004/005/006 (transactions, Flyway, envelope, CI+ArchUnit): keep** — all axis-independent.
- **ADR-001 (modular monolith package-by-feature): REVERSED → deferred** (this document).
- **ADR-007/008: keep.**

## Layered-Hardening Rule Set (Option B, ArchUnit-enforceable)

```text
controllers..  may not access repositories.. or mutate entities
service..      may not use SecurityContextHolder (allowlist: shared CurrentUserResolver)
service.impl.OrderServiceImpl may not access WalletRepository (cross-domain data via Wallet*Service)
service.impl.* may not import jakarta.transaction.Transactional
model..        may not import Spring (except jakarta.persistence) or infrastructure SDKs
all            may not import utils.Tuong.. after envelope unification
**/admin/**   endpoints must carry hasRole('ADMIN')  (manual list + naming convention)
```
Rules start with today's violations whitelisted as TODOs that shrink per phase — enforcing everything on day one would fail the build on 50 existing sites and get deleted. (Deliberate: rules that block legitimate use get removed; each rule above maps to a finding class, not taste.)
