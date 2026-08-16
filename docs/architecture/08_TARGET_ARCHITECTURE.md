# 08 — Target Architecture

## Alternatives Considered

| Criterion | (a) Keep package-by-layer | (b) Multi-module Maven | (c) Microservices | (d) Clean/Hexagonal everywhere | (e) **Modular monolith, package-by-feature (chosen)** |
|---|---|---|---|---|---|
| Benefits | zero migration cost | compile-time boundaries | independent scaling/deploy | pure domain, infra swap | feature locality, low merge friction, verified boundaries, zero build overhead |
| Costs | growing merge conflicts, scattered features; no boundary enforcement | build config ×N, release coupling, IDE friction | ops explosion (needs platform team) | 15 layers for 26K LOC; DTO factories everywhere | discipline to keep module rules (enforced by tests) |
| Migration complexity | none | high (pom surgery for every feature) | extreme | high | **low-medium (mechanical class moves)** |
| Operational risk | none during migration | medium | high | medium | **low (behavior-preserving moves)** |
| Team complexity | low | medium | high | medium | **low** |
| Fit for this repo | acceptable today, degrades | poor (4-person academic team) | rejected | over-engineered (Principle 4) | **strong: 10+ clean feature seams already exist (01 table)** |

## Decision

**(e) Single-module modular monolith with package-by-feature, layered internals per feature (api/service/repository/model), plus explicit ports only where infrastructure is genuinely volatile: payment gateways and AI clients.**

### Why chosen
- The domain decomposes naturally (auth, user/kyc, wallet, payment, credit, marketplace, profitsharing, emission, ai, project, vehicle, notification, filestorage) — every current service maps 1:1 to a future package.
- The codebase is already layer-clean *inside* features; only the package axis changes → mechanical, reviewable, revertible.
- Boundaries become testable (ArchUnit) instead of aspirational — directly addresses debt A1.
- Ports appear exactly where the P0 fix forces an interface anyway (`PaymentGateway.verify`) — no speculative abstraction.

### Why alternatives were rejected
- **(a)** keeps the daily tax (5 places to touch per feature, `impl/` dumping ground) and cannot encode the §13 dependency rules.
- **(b)** Maven module graph is team overhead the current size cannot amortize; revisit if the team doubles (R3 lesson).
- **(c)** no scaling evidence needs it; deployment is one VPS docker-compose.
- **(d)** 367 files do not need domain/application/infrastructure × every feature; hexagonal internals only inside `payment` and `ai` adapters, justified by the webhook verification work and AI stack consolidation respectively.

### Trade-offs that remain (explicit)
- Package-private encapsulation is not compiler-enforced across features (single module) — ArchUnit compensates; accepted residual risk.
- `SecurityContextHolder` removal from services is gradual; until Phase 4 completes, the `CurrentUserResolver` coexists with legacy helpers.
- Keeping one deployable means the AI-blocking-call problem must be solved in-process (async + polling), not by deployment isolation.

## Architecture Overview (target)

```text
com.carbonx.marketcarbon
├── shared/          envelope · error codes · pagination · CurrentUserResolver
├── config/          SecurityConfig · CorsConfig · AsyncConfig · OpenApiConfig · (profile-gated seeders)
├── auth/            login · JWT · OAuth2 · OTP
├── user/            users · roles · KYC profiles
├── wallet/          wallets · ledger · deposits · summaries
├── payment/         PaymentOrder + gateway port {vnpay,stripe,paypal} + webhook api
├── credit/          batches · credits · issuance · serials · retirement · certificates
├── marketplace/     listings · orders · settlement · pricing
├── profitsharing/   contributions · distributions · payouts
├── emission/        reports · CVA verification · analysis rules
├── ai/              scoring · chat · AiClient port {gemini,vertex}
├── project/  vehicle/  notification/  filestorage/
└── CarbonXApplication.java
```

## Dependency Direction (target)

```text
X.api ──▶ X.service ──▶ X.repository ──▶ X.model
any ──▶ shared, config
X.service ──▶ Y.service (interface only)   [sanctioned cross-calls: marketplace→credit, marketplace→wallet,
                                            profitsharing→wallet, emission→ai, wallet→payment]
service ──▶ port ──▶ adapter               [payment gateways, ai clients, storage]
```

## Architectural Rules

**Allowed**
- Access to `shared`/`config` from anywhere.
- Intra-feature any-layer (api→service→repository).
- The 5 sanctioned cross-feature service calls listed above.

**Forbidden**
- Cross-feature repository/model access (`marketplace.repository` from `wallet`, etc.).
- `X.api → Y.model` (entity leakage across features).
- `service → SecurityContextHolder` (use `CurrentUserResolver`).
- SDK types (Stripe/PayPal/Gemini/AWS) outside `payment.gateway.*` / `ai.client.*` / `filestorage.*`.
- Import of `jakarta.transaction.Transactional` (Spring's only — ADR-003).
- `utils.Tuong.*` (superseded by `shared` envelope — ADR-005).
