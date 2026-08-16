# 11 — Risk Register

> Likelihood/Impact: H/M/L. Owner = area, not person.

| ID | Risk | Likelihood | Impact | Mitigation | Detection | Rollback | Owner/Area |
|---|---|---|---|---|---|---|---|
| R1 | Deposit fix breaks legitimate deposits (FE still calls old endpoint) | M | H | FE coordination; feature flag `payment.verify.enabled`; deploy order BE(flag off)→FE→enable | deposit success-rate metric/logs (Phase 0 logging) | disable flag | payment+FE |
| R2 | Wallet locking change introduces deadlock (listing→credit→wallets lock order) | M | H | single documented lock order (listing → credit → wallets by id); lock-timeout configured; concurrency test | deadlock errors in logs; test in CI | revert phase commit | marketplace/wallet |
| R3 | REQUIRES_NEW error-marking opens nested-tx pitfalls (connection pool exhaustion under failure storm) | L-M | M | dedicated small `OrderStatusService`; pool sizing check; only ERROR path uses it | pool metrics once Actuator added (Phase 6/long-term) | revert | marketplace |
| R4 | Flyway baseline diverges from live prod schema | M | H | schema diff first; `baseline-on-migrate`; staging rehearsal; backup before cutover | `validate` failure at boot | set `ddl-auto=update` temporarily; repair baseline | database |
| R5 | Package moves break Spring component scanning / SpEL / reflection | L | M | mechanical moves only; full boot smoke per PR; no XML/SpEL on class names found (grep) | boot failure in CI (Phase 6) | revert feature PR | all |
| R6 | Envelope unification breaks frontend parsing | M | M | contract snapshot tests; FE adapter review; keep old fields during transition where cheap | FE error rates; manual smoke | revert | shared+FE |
| R7 | `transferFunds` fix changes displayed history for old (already-corrupt) rows | H (data already wrong) | M | one-time data-repair script optional; fix forward for new rows | reconcile query (sum of credits vs balances) | n/a (data fix, not code) | wallet |
| R8 | Secrets already leaked (Gmail password, DB creds in git history) | H | H | rotate now (out-of-band), history rewrite optional later; `.gitignore` hygiene | credential scans | n/a — rotation is the fix | ops |
| R9 | Concurrency regression from Phase-5 decompositions moving tx boundaries | M | M | Rule F: each moved boundary gets a test; keep `@Transactional` on orchestration entry points | unit tests + settlement smoke | revert extraction PR | services |
| R10 | Performance regression from added verification (Stripe/PayPal round-trip in deposit path) | M | L-M | verify calls are short-lived; async confirmation pattern (return 202, webhook completes) if slow | p6spy/k6 re-run of wallet endpoints | revert to sync | payment |
| R11 | Team bandwidth (academic timeline) stalls roadmap mid-way leaving mixed structure | M | M | phase independence; each phase valuable alone; CI gate from Phase 6 prevents silent decay | audit re-run (final phase) | each phase revertible | team |
| R12 | Test double-drift: new money tests mock too much and pass while real DB behavior differs | M | H | add ≥1 Testcontainers integration test per money flow in Phase 1 or 6 | integration suite | fix tests | testing |

## Standing Stop Conditions (from execution plan)

Halt the current phase and investigate whenever: tests fail unexpectedly; behavior differs from prediction; migration risk escalates; a refactor adds coupling; performance/security worsens; or the target becomes more complex than the original problem.
