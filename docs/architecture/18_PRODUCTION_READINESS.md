# 18 — Production Readiness

> Scoring: 0 absent · 1 weak · 2 partial · 3 acceptable · 4 strong · 5 production-grade.
> Per the second-audit rules: **critical failures are not averaged away** — one critical money/security hole keeps the verdict at NOT PRODUCTION-READY regardless of the mean.

## Category Scores

| Category | Score | Justification (evidence) |
|---|---|---|
| Security | 1 | Good primitives (BCrypt, JWT filter, HMAC for VNPay) vs 5 critical holes: N1 role-at-register, N2/N3 withdrawal escalation+replay, N4 JWT secret committed, SC1 unverified deposits (`13_SECURITY_AUDIT`) |
| Authorization | 1 | Role checks exist on ~40% of sensitive endpoints; ownership checks on ~30%; matrix in 19 |
| Authentication | 2 | JWT + OAuth2 works; hardcoded secret (N4) undermines the whole model; OAuth2 token in URL (SC7) |
| Payments | 1 | No verification, no webhooks, no reconciliation; VNPay signature OK but return never credits (`20`) |
| Financial correctness | 1 | Ledger bug (5.2), replayable refund (N3), phantom fee (N11), double-settlement race (N6) (`21`) |
| Concurrency | 2 | Locks on listing/credit; wallets + order rows unlocked; no lock-order policy (`23`) |
| Transactions | 1 | Mixed annotations; rollback-semantics bug S2; refund-without-ledger; controller-level tx orchestration |
| Database | 1 | `ddl-auto=update`; dump drift; no secondary indexes; no migrations (`22`) |
| Data integrity | 2 | FKs present in dump; balance clamping masks drift instead of failing; no CHECK constraints |
| Reliability | 1 | No retries/timeouts on payment SDK calls; no webhook replay handling; partial-failure gap between status flip and credit |
| Observability | 1 | No metrics/health/trace logging; println in auth path (`15`) |
| Testing | 1 | 26 unit tests; zero money-path coverage; no IT/concurrency/architecture tests (`14`) |
| CI/CD | 0 | No CI; Docker build skips tests |
| Deployment | 1 | Compose non-functional as reference (N16); root container; no healthcheck (`25`) |
| Secrets | 0 | Prod credentials + JWT secret + SMTP password committed to a public repo |
| Backups | 0 | No evidence of any backup (26) — UNKNOWN at best, absent by default |
| Disaster recovery | 0 | No restore procedure/RPO/RTO (26) |
| Performance | 3 | Measured N+1 fixes (1,901×/1,267× query reductions); unbounded history endpoint remains |
| Dependency hygiene | 2 | Deprecated PayPal SDK, old jjwt, duplicate deps, 3 CSV/2 PDF stacks (`24`) |
| API contracts | 2 | Mostly DTO; 3 envelopes; raw entities leak; error codes inconsistent with HTTP status |
| Configuration | 2 | Env-driven in base profile; committed prod values; business constants via scattered `@Value` |
| Operational readiness | 1 | Cannot answer "which payment failed for which user" today |

**Mean ≈ 1.3 / 5.** Mean is irrelevant here: 9 critical findings force the verdict below.

## Production Readiness Matrix

| Area | Severity | Current State | Evidence | Required Before Production | Can Defer |
|---|---|---|---|---|---|
| Unverified deposit crediting | **BLOCKER** | `paymentConfirmed=true` | `PaymentServiceImpl:126` | Server-side verify + ownership + single-tx credit | No |
| Role escalation at registration | **BLOCKER** | any role self-assignable | `AuthServiceImpl:59-63` | Whitelist {EV_OWNER, COMPANY} (+intent doc for CVA path) | No |
| Withdrawal admin endpoints open | **BLOCKER** | no role check | `WithdrawalController:87+` | `@PreAuthorize(ADMIN)` + status-guarded refund | No |
| Withdrawal refund replay | **BLOCKER** | refund without status check/ledger | `WithdrawalServiceImpl:125-130` | Status check under lock + ledger record | No |
| JWT secret committed | **BLOCKER** | static 70-char secret | `JwtConstant:4` | Env-var secret + rotation (invalidates tokens) | No |
| Prod credentials committed | **BLOCKER** | Stripe/PayPal/AWS/DB/mail in git | `application-prod.properties` | Rotate ALL + purge + env injection | No |
| Ledger corruption (transferFunds) | **BLOCKER** | wrong before/after fields | `WalletServiceImpl:379-380` | Fix + reconciliation of existing rows | No |
| Double-settlement race | HIGH | status check outside lock | `OrderServiceImpl:182-193` | Lock order row / status flip under listing lock | No (with P0) |
| Settlement wallets unlocked | HIGH | no-lock reads | `OrderServiceImpl:214-227` | Lock or atomic UPDATE | No (with P0) |
| Order ERROR lost on rollback | HIGH | same-tx save | `OrderServiceImpl:319-324` | REQUIRES_NEW status write | Short deferral |
| Complete-order caller check | HIGH | any COMPANY completes any order | `OrderServiceImpl:176-183` | buyer-ownership check | No |
| KYC admin/cva creation open | HIGH | existence-check only | `KycServiceImpl:320,235` | role gate | No |
| CVA dashboard unroleed | HIGH | any authenticated role | `CvaDashboardController` | `@PreAuthorize(CVA/ADMIN)` | Brief |
| `anyRequest` permitAll + test controller | HIGH | default-open | `AppConfig:81` | authenticated + delete controller | No |
| No CI / tests skipped in image | HIGH | no workflows; -DskipTests | `.github` absent; Dockerfile | CI gate with tests | No |
| ddl-auto=update | HIGH | no migrations | application*.properties | Flyway baseline | After P0 |
| Observability (trace/metrics/health) | MEDIUM | absent | 15 | MDC + Actuator minimum | Partially |
| Pagination of transaction history | MEDIUM | unbounded | `WalletTransactionServiceImpl:130` | Paginate | Yes |
| Dependency upgrades | MEDIUM | deprecated/old SDKs | 24 | Planned upgrades | Yes |
| Envelope unification | MEDIUM | 3 shapes | 04 | Single envelope | Yes |
| Backups/DR | **BLOCKER (operational)** | no evidence | 26 | Backup job + restore test before real money | No |
| Package re-organization | LOW (now) | package-by-layer | 28 | Deferred by choice | Yes |

## Verdict

**NOT PRODUCTION-READY.**

The system is a public deployment handling simulated (or potentially real) funds with: client-trusted payment confirmation, self-service admin registration, unguarded withdrawal approvals with a replayable refund, a committed JWT signing secret and production API keys, corrupted ledger records, and no backups, CI, or monitoring. Every one of these is independently disqualifying for real money. The path to *conditional* readiness is the P0 batch in 29 — estimated days, not weeks, because the fixes are small and the codebase's existing patterns (locks, status machines) already show where they belong.
