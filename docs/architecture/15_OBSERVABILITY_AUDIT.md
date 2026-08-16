# 15 — Observability Audit

## Signal-by-Signal Assessment

| Signal | Current State | Gap | Target State | Operational Value |
|---|---|---|---|---|
| Structured logging | SLF4J `@Slf4j` in services; plain-text logback pattern; 10 `System.out.println` sites (incl. auth/payment) | println bypasses log levels/files; no JSON option | logger-only, leveled; optional JSON for ingestion | grep-able history, level control |
| Correlation ID | `X-Request-Trace` header accepted and echoed into response envelope (every controller) — but **never logged nor propagated to async/SSE** | trace exists visually, not operationally | MDC filter: put trace at request start, include in every log line, propagate via `DelegatingSecurityContextAsyncTaskExecutor` context or explicit param | follow one request across layers/threads |
| Metrics | **None** (no Actuator dependency) | cannot see latency/error rates/lock waits | Actuator + Micrometer: http server requests, Hikari pool, executor queue depth, custom counters (deposits verified/rejected, settlements failed) | detect R1/R2/R10 risks live |
| Health checks | None | orchestrator restarts blind | Actuator `/health` (db, redis) + compose healthcheck | fast failure detection |
| Tracing | None | n/a | optional later (Micrometer Tracing) — defer until traffic justifies | cross-service spans (not needed for monolith now) |
| Error tracking | 500s logged with stack (GlobalExceptionHandler `log.error`) — good; but client sees raw message (SC5) | leak vs visibility conflated | generic client body + trace id; full detail server-side | users report trace id; ops find it in logs |
| Alerting | None | silent failures (e.g., stuck PENDING orders from S2) | later: alert on settlement-failure counter, deposit-verify-failure rate | proactive incident response |
| SQL observability | p6spy file logging (logs/p6spy.log), hibernate SQL DEBUG in logback | always-on file writes to repo-local dir; DEBUG SQL noisy in prod | keep p6spy for profiling sessions only; move logs out of repo; document k6 protocol (12) | the existing measurement discipline — preserve it |
| Audit trail (domain) | `WalletTransaction.balanceBefore/After` (good design); emission audit logs mentioned in README | transferFunds bug corrupts the trail (SC-adjacent) | fix + reconciliation query (sum credits = balances) | financial forensics |
| Sensitive data in logs | emails printed per request (JwtTokenValidator:90), Stripe session (PaymentServiceImpl:165), OAuth emails (AppConfig:116) | PII/payment objects in stdout | mask/remove; debug level | compliance + safety |

## Quick Wins (folded into Roadmap Phase 0/6)

1. Replace println with loggers (10 sites) — 1 hour.
2. MDC filter for `X-Request-Trace` (header already exists — pure upside) — half a day.
3. Add `spring-boot-starter-actuator` + health endpoint — 1 hour.
4. Move `logs/` out of repo; gitignore.

## Deliberately Deferred

Distributed tracing, log aggregation (ELK/Loki), dashboards, alerting rules — valuable only once CI + metrics exist and the deployment has an operator; revisit after Phase 6 (avoids tool churn per Principle: recommend tools that solve identified problems).
