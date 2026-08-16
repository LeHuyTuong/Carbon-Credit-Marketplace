# 25 — Deployment & Runtime Audit

## Artifacts Reviewed

`backend/Market_carbon/Dockerfile`, `frontend/Dockerfile` (not read line-by-line — standard static build assumed, UNKNOWN details), `frontend/nginx.conf`, `docker-compose.yml`, `application{,-local,-prod}.properties`, `logback-spring.xml`.

## Backend Dockerfile

| Aspect | State | Evidence | Risk |
|---|---|---|---|
| Multi-stage build | Yes (maven build → jre runtime) with cache mounts | Dockerfile | Good |
| Tests during build | **Skipped** (`-DskipTests -Dmaven.test.skip=true`) | RUN line | Tests never gate any artifact |
| Container user | **root** (no USER directive) | Runtime stage | Container escape blast radius |
| JVM settings | None (default heap; no -XX flags, no container-aware tuning) | ENTRYPOINT | OOM/underuse risk under load |
| Healthcheck | None (no HEALTHCHECK, no Actuator) | — | Orchestration blind |
| Graceful shutdown | Not configured (server.shutdown=graceful absent) | properties | In-flight settlement killed on redeploy |
| Logs | stdout (fine) + p6spy file into container FS | logback | Disk growth; `logs/` also in repo |

## docker-compose (dev/topology reference)

| Aspect | State | Evidence | Risk |
|---|---|---|---|
| Secrets | **Plaintext in file** (MySQL root, Gmail app password) | compose env block | Committed credential (SC2) |
| FE→API wiring | **Broken as written**: nginx conf has no `/api` proxy; FE build arg uses `REACT_APP_API_BASE_URL` (CRA-style, project is Vite) = `http://be:8082/api` (container hostname, unreachable from browsers) | `frontend/nginx.conf` + compose fe block | Compose is not a runnable reference (N16) |
| Redis service | **Absent** from compose though backend config expects Redis | compose services list | OTP flow fails in compose-local unless Redis external |
| Resource limits | None | — | Noisy-neighbor risk on shared VPS |
| Restart policy | db/be `unless-stopped`; **fe missing** | compose | FE stays down after host reboot |
| Healthcheck | db only (mysqladmin ping) | compose | be/fe failures undetected |
| Volumes | db_data only | compose | No log volume, no backup volume |

## Environment / Profiles

| Profile | State | Issues |
|---|---|---|
| base (`application.properties`) | env-var driven, sane defaults | — |
| local | untracked, p6spy + hikari tuning + `ddl-auto` — dev-oriented | packaged `seed_large.py`, `spy.properties` in resources (jar bloat/info) |
| **prod** | **committed with live values** (DB, mail, AWS key, Stripe key, PayPal secret) | N5 — critical |

## Survival Scenarios

| Scenario | Expected behavior today | Basis |
|---|---|---|
| DB restart | Hikari re-establishes connections; in-flight settlements rolled back (partial user impact); no health signal | JDBC pool defaults |
| Redis restart | OTP send/verify errors surface as 500s | single Redis usage path |
| App restart (deploy) | **In-flight settlement/payment credit can be cut mid-transaction** — DB tx protects consistency, but multi-tx flows (F3/F4) can strand PENDING/SUCCEEDED-without-credit states | no graceful shutdown |
| Container restart | compose restarts be/db; fe stays down (no policy) | compose |
| Stripe/PayPal outage | Link creation 500s; **no timeout/retry configured on SDK calls** — threads can hang on socket defaults | `PaymentServiceImpl` |
| Gemini/Vertex outage | AI scoring/chat 500s (Vertex has timeout config); EmissionAi blocking call holds request thread | AI configs |
| Network blip (Cloudflare) | Frontend static cached; API fails fast | topology assumption |
| High traffic | Rate limiter (20/min default) protects `/api/**` — **not `/v1/ai`**; unbounded `getTransactions` is the memory/lativity hotspot; pool exhaustion in settlement storms plausible | 12/23 |
| Memory pressure | No JVM limits/tuning; OOM kill → restart, no alert | Dockerfile |
| Disk full | p6spy/`logs/` growth unbounded in container + repo | logback |

## Required Minimums Before Real Traffic

1. Env-injected secrets everywhere; remove committed prod profile (rotate first).
2. `USER` non-root + `-XX:MaxRAMPercentage` + `server.shutdown=graceful` + Actuator health/readiness + compose healthchecks + fe restart policy.
3. Fix compose reference deployment (nginx `/api` proxy + Vite env var) or delete it to stop implying it works.
4. Redis service in compose (or remove Redis dependency).
5. Outbound HTTP timeouts for Stripe/PayPal/Gemini clients.
