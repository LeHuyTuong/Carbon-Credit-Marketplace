# 07 — Architecture Comparison

## Gap Table (Current vs Reference Practice)

| Area | Current Repo | Reference Practice | Gap | Recommendation | Evidence |
|---|---|---|---|---|---|
| Package structure | Package-by-layer; `impl/` dumping ground; `utils/Tuong` | Package-by-feature modules (Modulith R2, Keycloak R3) | High | Incremental feature packages (Roadmap Phase 4) | `01` inventory; `05` |
| Module boundaries | None enforced | Test-verified boundaries (Modulith `verify()`) | High | ArchUnit rules (Phase 6) | `02` dependency flow |
| Dependency direction | Mostly downward; `SecurityContextHolder` in services; cross-domain free | Auth resolved at edge; cross-module via public service API only | Medium | `CurrentUserResolver` + sanctioned-call list | `OrderServiceImpl:52-60` ×6 services |
| Service responsibilities | Transaction-script; giant methods; mixed infra | Use-case services + pure assemblers (JHipster R4) | Medium | Method extraction where tested (`05`) | `MarketplaceServiceImpl:60-371` |
| Domain model | Anemic entities + Jackson on entities | Entities free of presentation concerns | Medium | Move annotations to DTOs during Phase 4 moves | `Wallet.java:35,40`; `CarbonCredit.java:76` |
| Infrastructure boundaries | SDK calls inside services/transactions | Ports + adapters for volatile infra (payments/AI) | High | `PaymentGateway`, `AiClient` ports (Phase 5) | `PaymentServiceImpl:143-228` |
| Configuration | `@Value` scattered; manual property getters | `@ConfigurationProperties` typed objects | Medium | Consolidate hotspots first (`trading_fee`, AI thresholds) | `OrderServiceImpl:45`; `GeminiAiScoringService:1080-1099` |
| Transactions | Mixed annotations; rollback-semantics bug; no readOnly policy | One annotation; policy documented; REQUIRES_NEW for out-of-band writes | High | Phase 1/2 fixes + S2 | `04` §Spring |
| Error handling | Central but leaking; 3 envelopes | One envelope; generic 500 + trace id | Medium | Phase 2 | `GlobalExceptionHandler:118-124` |
| API contract | DTOs mostly; raw entities leak in places | DTO-only at boundary | Medium | Fix 2 controllers | `PaymentController:103-106`; `KycController:74` |
| Testing | 26 unit tests, no money-path tests, no CI | Test-per-change + IT + CI gate (JHipster) | **Critical** | Phase 1 tests + GitHub Actions | `14` |
| Schema management | `ddl-auto=update` + manual dump | Flyway/Liquibase from day one | High | Phase 3 baseline | `application.properties`; `backend/database/core_ccm.sql` |
| Security | Good primitives; client-trusted confirmation; committed secrets | Least privilege; server-verified; secret mgmt | **Critical** | Phase 0/1 | `13` |
| Observability | println + p6spy; no actuator/MDC | Actuator + structured logs + correlation | High | Phase 0/6 | `15` |
| Performance | N+1 fixed (measured 1,901×); unbounded history endpoint | Measured budgets per endpoint | Medium | D3 fix + budgets | `12` |
| Concurrency | Locks on listing/credit; wallet debit unlocked | Every balance mutation locked or atomic | High | N1 fix | `04` §Concurrency |
| Reliability | Redirect-and-hope payments; no webhooks/retries | Verified callbacks + retry + idempotency | **Critical** | Phase 1 | F2 research |

## Verdict

The repository sits closest to **Petclinic-style package-by-layer Transaction-Script monolith** (R1) with production instincts borrowed from practice (locks, rate limits, HMAC). It is *behind* the boring JHipster baseline (R4) on migrations/CI/tests, and *ahead* of typical student projects on concurrency awareness. The gap to the recommended target (R2-style modular monolith) is structural but mechanical — no paradigm shift required.
