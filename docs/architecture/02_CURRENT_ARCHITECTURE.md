# 02 — Current Architecture

## Classification

**Single-module, package-by-layer, service-centric layered architecture with Transaction-Script services and implicit boundaries.**

- *Package-by-layer*: top-level packages are technical (`controller`, `service`, `repository`, `model`, `dto`).
- *Service-centric / Transaction-Script*: business logic lives as procedural use-case methods in `service/impl/*` classes; entities are anemic data holders (no behavior beyond getters/setters; the only computed member is `CarbonCredit.getIssuedYear()`).
- *Implicit boundaries*: nothing prevents controller→repository, cross-domain repository access, or `SecurityContextHolder` usage in services — only convention.
- Two feature-flavored sub-packages (`service/analysis`, `service/credit`) show the codebase already drifting toward feature orientation.

## Request Flow

```text
Client (React SPA / VNPay redirect)
  → Cloudflare → nginx (fe container) → /api proxied to :8082
  → JwtTokenValidator (filter; parses Bearer token, loads user)
  → RateLimitInterceptor (Bucket4j, 20 req/min default)
  → Controller (28; envelope: TuongCommonResponse | CommonResponse | raw entity)
  → ServiceImpl (@Transactional: 16 files jakarta.*, 7 files Spring's)
  → Repository (Spring Data JPA) → MySQL
```

## Data Flow (buy flow, traced end-to-end)

1. `POST /orders` → `OrderServiceImpl.createOrder` — validate quantity/price/self-buy, snapshot `unitPrice/platformFee(=trading_fee=0.05)/sellerPayout(=totalPrice)`, persist PENDING.
2. Frontend confirmation → `PUT /orders/{id}/complete` → `completeOrder` in one transaction:
   - lock listing (`findByIdWithPessimisticLockAndDetails`), lock source credit (`findByIdWithPessimisticLock`)
   - load buyer/seller wallets **without locks** (`findByCompanyIdWithDetails`)
   - mutate seller credit amounts; mark TRADED if exhausted
   - `creditIssuanceService.issueTradeCredit(...)` mints buyer-side credit
   - update wallet carbon-credit balances; update listing quantity/soldQuantity/status
   - order → SUCCESS; `processFinancialTransactions` creates buyer-debit + seller-credit `WalletTransaction` rows (balance mutation happens inside `WalletTransactionServiceImpl.createTransaction`)
3. Later: `@Async` profit sharing → `WalletServiceImpl.transferFunds` (dual PESSIMISTIC_WRITE locks, writes 2 ledger rows — one with a copy-paste bug, see 04).

## Dependency Flow

```text
controller ──▶ service interface ──▶ service/impl ──▶ repository ──▶ model
                   │                        │
                   │                        ├──▶ other domains' services (cross-cutting)
                   │                        ├──▶ Stripe / PayPal / VNPay / S3 / Gemini / Vertex (SDK calls in tx scope)
                   │                        ├──▶ SecurityContextHolder (current user)
                   │                        └──▶ SseService / EmailService (@Async)
```

## Business Logic Flow

Domain rules are embedded in service methods (e.g., quantity-vs-listed accounting in `OrderServiceImpl:230-303`, credit chain resolution in `WalletServiceImpl.resolveRootCredit:278-295`, payout formulas in `ProfitSharingServiceImpl`/`CompanyPayoutQueryServiceImpl`). No domain events; side-effects (email, SSE) are called inline or via `@Async`.

## Infrastructure Flow

Config classes wire: JWT/OAuth2 (`AppConfig`), thread pool (`ConcurrencyConfig`), rate limit, S3, VNPay props, two AI clients, WebSocket/SSE. Infrastructure and business code meet inside `service/impl` — there is no adapter layer.

## Cross-cutting Concerns

| Concern | Implementation | Location |
|---|---|---|
| AuthN | JWT filter + OAuth2 island | `config/AppConfig.java`, `config/JwtTokenValidator.java` |
| AuthZ | Endpoint matchers + (rare) `@PreAuthorize` | `AppConfig:71-82`; commented-out example `WalletController:143` |
| Current user | `SecurityContextHolder` + DB lookup, copy-pasted in ≥6 services | e.g. `OrderServiceImpl:52-60` |
| Transactions | Mixed annotations, no policy doc | see 04 §Spring |
| Errors | `GlobalExceptionHandler` (206 LOC), 3 envelope shapes | `exception/GlobalExceptionHandler.java` |
| Response envelope | `TuongCommonResponse` / `CommonResponse` / ad-hoc Map | `utils/Tuong/`, `utils/CommonResponse.java` |
| Async | one executor `profitSharingTaskExecutor` (10/20/500, security-context-propagating) | `config/ConcurrencyConfig.java` |
| Scheduling | 2 `@Scheduled` jobs | `scheduler/` |
| Seeding | `DataInitializer` ApplicationRunner (323 LOC) | `config/DataInitializer.java` |

## Architecture Diagram (as-is)

```text
┌────────────────────────────── Market_carbon (single deployable) ──────────────────────────────┐
│                                                                                              │
│  controller/* ────────────▶ service/*(+impl) ────────────▶ repository/* ────────▶ MySQL       │
│      │  (28)                     │ (33 impl)                    │ (26)      ddl-auto=update   │
│      │                           │                                                              │
│      │                           ├──▶ Stripe / PayPal / VNPay        (HTTP, some in tx)        │
│      │                           ├──▶ Gemini API / Vertex AI        (blocking WebClient)      │
│      │                           ├──▶ AWS S3                                                    │
│      │                           ├──▶ Redis (OTP only)                                         │
│      │                           ├──▶ SSE emitters + @Async email/payouts                      │
│      │                           └──▶ SecurityContextHolder (current-user coupling)            │
│                                                                                              │
│  Cross-cutting: GlobalExceptionHandler · JwtTokenValidator · RateLimitInterceptor ·           │
│                 ConcurrencyConfig · DataInitializer · 2 response envelopes                    │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

## Honest Caveats (explicit uncertainty)

- Package-level cycle analysis was done by inspection only (no jdeps/class-cycle tooling run): no blocking cycles found; `service ↔ service/impl` self-reference workaround exists in `ProfitSharingServiceImpl.getSelf()` (lines 63-74). Status: LOW-CONFIDENCE complete.
- The OAuth2 + Thymeleaf islands (session-based flows inside a JWT API) are architecturally separate mini-flows; they were traced but not exhaustively audited.
