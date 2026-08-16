# 26 — Backup & Disaster Recovery Audit

> Rule: no "probably fine". Without evidence, status is UNKNOWN. Operational evidence lives outside the repo (VPS, provider consoles) — none was available to this audit.

## Assessment

| Capability | Status | Evidence / Gap |
|---|---|---|
| Database backup | **UNKNOWN — no evidence in repo** | No cron/script/service in repo; compose has only `db_data` volume; a VPS-side cron *may* exist (operator must confirm) |
| Backup retention | UNKNOWN | — |
| Restore procedure | **Absent** (no runbook anywhere in repo/docs) | — |
| Restore test (fire-drill) | Absent / UNKNOWN | — |
| RPO (max acceptable loss) | **Undefined** | — |
| RTO (max acceptable downtime) | **Undefined** | — |
| S3 backup/versioning | UNKNOWN (bucket config not in repo) | — |
| Uploaded files durability | UNKNOWN | KYC docs/certificates live in S3; versioning/lifecycle unknown |
| Secrets recovery | **Broken by design**: all secrets in git (some already leaked); rotation procedure absent | N4/N5 |
| Migration rollback | Impossible today (no migrations — `ddl-auto=update` has no down path) | 22 |
| Data-repair process | Absent — and *needed now*: `transferFunds` ledger corruption (I9) requires a one-time repair once the bug is fixed; withdrawal refunds lack ledger rows | 21 |
| Instance-level recovery | Partial: docker-compose re-creates containers; **nothing re-creates data** except the volume | compose |

## Consequences of Current State (if operated with real money)

- Single `docker volume rm` / disk failure / ransomware event = **total, unrecoverable loss** of wallets, ledger, orders (unless an undocumented external backup exists).
- The known ledger corruption (I9) is currently **unrepairable in a verified way** because there is no reconcilable baseline (reconciliation queries absent, 21).
- Secret rotation cannot be performed safely without a documented redeploy procedure (prod profile is in git).

## Minimum DR Baseline (must exist before "conditionally production-ready")

1. **Daily** `mysqldump` (or Percona XtraBackup) cron on the VPS → offsite storage (S3 bucket with versioning + lifecycle), 30-day retention.
2. Restore runbook: documented steps + one executed fire-drill on a staging DB, recorded date/result.
3. Defined RPO/RTO (suggest RPO 24h cold-start acceptable only for demo; real money ⇒ RPO ≤ 1h via binlog or hourly dumps, RTO ≤ 4h).
4. S3: enable versioning + lifecycle on the documents bucket; document bucket layout.
5. Secrets: rotation runbook per secret class (DB, SMTP, AWS, Stripe, PayPal, JWT).
6. Post-P0-B: run reconciliation queries (21) as a daily job; alert on drift.

All of the above are **operator actions + small scripts**; none block code fixes, but all block the "real money" gate.
