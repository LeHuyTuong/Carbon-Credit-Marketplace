# 12 — Performance Audit

> Principle: no performance claim without measurement. This audit is lucky — the repository contains its own measured baseline (`OPTIMIZATION_LOG.md`, `CV_METRICS.md`, 2026-08-04: k6 + p6spy, dataset 2,000 credits / 2,000 listings / 10 companies).

## Measurement Setup (existing, reusable)

| Parameter | Value |
|---|---|
| Load tool | k6 (10/50/100 VUs, no sleep, per-endpoint groups) |
| SQL counting | p6spy per-request logs (grep statement count) |
| Dataset | 2,000 credits, 2,000 listings, 10 companies/wallets (seed_large.py) |
| Environment | macOS + Colima + MySQL 8 docker, local profile |

## Completed Optimization (current uncommitted branch) — Before/After

### `GET /api/v1/marketplace`

| Metric | Before | After | Delta | VUs |
|---|---|---|---|---|
| Queries/request | 3,802 | 2 | **1,901×** | 1 |
| Avg response | 4.91 s | 226 ms | 21.7× | 10 |
| Throughput | 7.22 req/s | 44.16 req/s | 6.1× | 10 |
| Avg response | — | 145 ms / 2.73 s | — | 50 / 100 |
| Success | 100% | 100% | — | all |

Root cause (p6spy-confirmed): no JOIN FETCH + Hibernate 6 eagerly initializing inverse `@OneToOne(mappedBy)` (`CreditBatch.certificate` ~2,000 queries, `Company.wallet` ~1,801 queries).

### `GET /api/v1/wallet`

| Metric | Before | After | Delta |
|---|---|---|---|
| Queries/request | ~3,800+ | 3 | ~1,267× |
| Avg response (10 VUs) | ~8 s+ (est.) | 30.89 ms | ~250× |
| Throughput (10 VUs) | <2 req/s | 319.4 req/s | ~160× |
| Success | ~95% | 100% | — |

(50 VUs: 137 ms / 362.96 req/s · 100 VUs: 215.83 ms / 460.91 req/s.)

### Order flow

Extra queries per order: ~8-10 → ~3 (p6spy-verified; not load-tested — pessimistic locks limit useful concurrency).

**Honest note**: the wallet "before" numbers were partly estimated (baseline too slow to complete a 10-VU run — recorded as such in CV_METRICS); marketplace numbers are fully measured.

## Remaining Performance Debt (with proposed measurement)

| ID | Item | Baseline | Target | Measurement method | Expected change | Status |
|---|---|---|---|---|---|---|
| P-D3 | `getTransactions()` loads full history per wallet view, merges in memory (`WalletTransactionServiceImpl:130-214`) | UNKNOWN (grows with data) | paginated (page size 20-50) | k6 on `/wallet/transactions` with 10k-transaction seed | flat latency as history grows; query count 1-2/page | not measured — must measure before optimizing (Rule G) |
| P2 | Blocking Gemini/Vertex calls on request threads | UNKNOWN | async + polling/SSE | k6 on AI-scoring endpoint with mocked slow AI (add latency) | free request threads; no thread-pool exhaustion | not measured |
| P3 | `CompanyPayoutQueryServiceImpl` in-memory search filter after full aggregation (`:493-499`) | UNKNOWN | DB-side filtering or pre-aggregation | p6spy on payout list with search term | query count ↓ on search | not measured |
| P1-root | EAGER `@OneToOne` defaults remain for any *new* query path (fetch-joins patched per-endpoint, not globally) | n/a | `@OneToOne(fetch=LAZY)` + optional bytecode enhancement, or keep per-query fetch joins | p6spy on each new endpoint | prevents future N+1 recurrence | architectural watch-item |
| P-R10 | Phase-1 adds Stripe/PayPal verify round-trip in deposit path | n/a | <500 ms budget or async 202 pattern | k6 after Phase 1 | acceptable UX | pending Phase 1 |

## Index Situation

Status: **UNKNOWN** — no `EXPLAIN` plans or index review performed for the fetch-join queries (multi-join on `carbon_credits × credit_batches × companies × wallets`). The 2-query results above were measured on a 2k-row dataset; at production scale the join width matters. What is needed: `EXPLAIN` on the three `*WithDetails` queries under the seed dataset; add composite indexes if scans appear. Scheduled as part of Phase 3 (schema work) — measure first, index second.

## Benchmark Protocol for Future Phases (adopted)

```text
Before phase:  k6 @ 10/50/100 VUs + p6spy query counts on affected endpoints
After phase:   same script, same seed (seed_large.py), same runtime
Record:        queries/request · avg/p95 · throughput · success rate
```
Store results in `docs/architecture/` addenda or revive `OPTIMIZATION_LOG.md` as the running log.

## Version Discrepancy Note

`OPTIMIZATION_LOG.md` header says "Java 21"; `pom.xml` pins Java 17. POM is authoritative for builds — measurements were presumably taken on the developer JVM. Flagged so future benchmarks record the actual runtime.
