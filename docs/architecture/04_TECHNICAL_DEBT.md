# 04 — Technical Debt Audit

> Severity: **C** critical / **H** high / **M** medium / **L** low. Every finding carries file:line evidence.

## Architecture

| ID | Finding | Severity | Evidence |
|---|---|---|---|
| A1 | Implicit boundaries; cross-domain access unrestricted | M | `OrderServiceImpl:38-43` injects `WalletTransactionService` + `CreditIssuanceService`; nothing forbids controller→repository |
| A2 | Two parallel AI stacks (google-genai vs Vertex WebClient) | M | `config/AiConfig.java` vs `config/AiVertexConfig.java` + `VertexWebClientConfig.java`; `GeminiAiService` 527 LOC interface file + `GeminiAiScoringService` 1105 |
| A3 | Presentation leaking into domain: Jackson annotations on entities | M | `Wallet.java:3-4,35,40`; `CarbonCredit.java:4-5,76` (`@JsonProperty("current_price")`) |
| A4 | Session/Thymeleaf island inside JWT REST app | L-M | `VNPaymentController:12-49` (`@Controller` returning views) |
| A5 | Shared-code dumping grounds: `utils/Tuong` (personal-named envelope), `helper`, `common` overlap | L | `utils/Tuong/*`; `helper/notification/*`; `common/` |

## Code Quality

| ID | Finding | Severity | Evidence |
|---|---|---|---|
| C1 | ~310-line method `listCreditsForSale` | H | `MarketplaceServiceImpl.java:60-371` |
| C2 | Copy-pasted `currentUser()`/`currentCompany()` in ≥6 services | M | `OrderServiceImpl:52-67`, `WalletServiceImpl:48-56`, `WalletTransactionServiceImpl:32-40`, `PaymentServiceImpl:63-71`, `MarketplaceServiceImpl:43-58`, `MyCreditServiceImpl:64+` |
| C3 | `System.out.println` in 10 places incl. security/payment paths | M | `JwtTokenValidator:90` (emails), `PaymentServiceImpl:165` (Stripe session), `AppConfig:116`, `SearchRepository:80`, `AiConfig:77`, `S3Config:27`, `VertexWebClientConfig:75`, `FileUploadTestController:27-33` |
| C4 | Dead schema field with misleading idempotency comments | M | `MarketPlaceListing.idempotencyKey` — zero usages repo-wide (grep) |
| C5 | Commented-out code blocks in controllers | L | `WalletController:58-77,158-173`; `CarbonCreditServiceImpl:15` |
| C6 | Mixed-language step comments encode legacy flow | L | `// B4.1` style across `OrderServiceImpl`, `WalletServiceImpl` |
| C7 | Fee math duplicated/implied: `sellerPayout = totalPrice` while `platformFee` stored but never applied at settlement | M | `OrderServiceImpl:100-112`; fee only surfaces via profit sharing — single source of truth missing |

## Spring

| ID | Finding | Severity | Evidence |
|---|---|---|---|
| S1 | Mixed `@Transactional` semantics: 16 files jakarta vs 7 Spring | H | grep counts; jakarta lacks `readOnly`/`isolation`/`NESTED` (Spring reference, Baeldung). Money-path files affected: `WalletServiceImpl:19`, `OrderServiceImpl:15`, `WalletTransactionServiceImpl:12`, `PaymentServiceImpl:23` |
| S2 | Order ERROR status written in the transaction that rolls back | H | `OrderServiceImpl:319-324` — save discarded on rollback; orders stay PENDING |
| S3 | Self-invocation workaround via `ApplicationContext` self-proxy | M | `ProfitSharingServiceImpl:63-74` (`getSelf()`) |
| S4 | `@Value` config scattered incl. `${trading_fee}` magic string | L-M | `OrderServiceImpl:45-46`; `PaymentServiceImpl:45-61`; `GeminiAiScoringService.getDoubleProperty/getIntProperty:1080-1099` |
| S5 | `@Transactional` read methods without `readOnly` (pre-branch) | L | `WalletServiceImpl.findWalletById:127-136` etc. |

## Database

| ID | Finding | Severity | Evidence |
|---|---|---|---|
| D1 | `ddl-auto=update` in all profiles; no migration tool | H | `application.properties` (`SPRING_JPA_HIBERNATE_DDL_AUTO:update`); hand dump `backend/database/core_ccm.sql` drifts from entities (Status: partially UNKNOWN — needs schema diff) |
| D2 | EAGER-by-default `@OneToOne` relations (root cause of N+1) | M | `Wallet.java:25-36` (`user`, `carbonCredit`, `company`); `CreditBatch.certificate`, `Company.wallet` per OPTIMIZATION_LOG Phase 1 |
| D3 | Unbounded in-memory processing of transaction history | M-H | `WalletTransactionServiceImpl.getTransactions:130-214` loads full history then merges in memory; no pagination |
| D4 | Business logic embedded in query fetch graphs (drifting responsibility) | L | new `*WithDetails` repositories (acceptable; documented as boundary watch-item) |

## Concurrency

| ID | Finding | Severity | Evidence |
|---|---|---|---|
| N1 | Buyer/seller wallets not locked during settlement → lost-update double-spend window | H | `OrderServiceImpl:214-227` (no-lock loads); debit in `WalletTransactionServiceImpl.createTransaction:67-100` reads balance without lock; existing `WalletRepository.findByUserIdWithPessimisticWrite` unused on this path |
| N2 | Deposit endpoint not idempotent per user (status flip + credit in separate steps) | H | `WalletController:89-95` → `processPaymentOrder` then `addBalanceToWallet` |
| N3 | Lock-order discipline not documented (deadlock risk when combining listing/credit/wallet locks) | M | `completeOrder` locks listing→credit, later reads wallets; `transferFunds` locks by wallet id order implicitly — no stated rule |

## Performance

| ID | Finding | Severity | Evidence |
|---|---|---|---|
| P1 | Historical N+1 (being fixed; root EAGER mappings remain for new queries) | M | 12_PERFORMANCE_AUDIT; `Wallet.java:25-36` |
| P2 | Blocking Gemini/Vertex calls on request threads | M | `GeminiAiScoringService` WebClient usage inside verification flow |
| P3 | `CompanyPayoutQueryServiceImpl.matchesSearch` filters in memory after full aggregation | L-M | `CompanyPayoutQueryServiceImpl:493-499` |
| P4 | Wallet endpoint loads all transactions each view | M | same as D3 |

## Security

Full detail in [13_SECURITY_AUDIT](13_SECURITY_AUDIT.md). Summary: client-trusted deposit confirmation (**C**), committed secrets (**C**), `anyRequest().permitAll()` (**H**), 500 message leak (**M**), JWT in OAuth2 redirect URL (**M**), dead CORS wildcard entries (**L**), demo accounts on public deployment (**H**).

## Reliability

| ID | Finding | Severity | Evidence |
|---|---|---|---|
| R1 | No payment gateway verification for Stripe/PayPal; VNPay return never credits wallet | C | `PaymentServiceImpl:125-126` (`paymentConfirmed = true`); `VNPayService.orderReturn:110-118` (status-only) |
| R2 | No webhook/callback retry handling anywhere | H | no webhook controllers exist |
| R3 | External HTTP (Stripe/PayPal/Gemini) without explicit timeouts/retries/circuit breaking (AI timeouts exist; payment SDK calls unbounded) | M | `PaymentServiceImpl:143-228` |
| R4 | Exception swallow-and-continue patterns | L-M | `WalletTransactionServiceImpl:88-91` (balance untouched, silent fallthrough); `processFinancialTransactions` wraps into generic AppException (`OrderServiceImpl:357-360`) |

## Observability

No Actuator/metrics/tracing/MDC; trace header echoed but never logged; `System.out.println` in auth path; p6spy SQL logs written into repo `logs/`. Detail in [15_OBSERVABILITY_AUDIT](15_OBSERVABILITY_AUDIT.md).

## Testing

26 unit tests / 367 files; zero coverage on wallet balance math, payment confirmation, VNPay, order settlement, profit sharing, withdrawal. No integration/architecture tests, no CI to run them. Detail in [14_TESTING_AUDIT](14_TESTING_AUDIT.md).

## Root Causes (why it looks like this)

1. **Demo-first integrations**: payment flows were built from gateway quickstarts (redirect-and-hope) and verification was stubbed (`paymentConfirmed` TODO comment).
2. **Layer-first packages** gave no natural home for cross-cutting helpers → copy-paste (`currentUser`) and dumping grounds (`utils/Tuong`).
3. **No CI/test gate** → regressions invisible → safety-net never grew → big methods stayed big.
4. **Academic timeline** → template leftovers (artifactId, Thymeleaf island, test controller) and repo hygiene debt.
5. **Schema-by-Hibernate** → no migration culture → `core_ccm.sql` manual dump drift.
