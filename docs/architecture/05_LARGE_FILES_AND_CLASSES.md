# 05 — Large Files and God-Class Analysis

> LOC is context, not verdict (a cohesive 800-line class can be fine; a 300-line 7-responsibility class is not). Public methods and dependency counts were taken from the source.

## Inventory

| File | LOC | Public methods | Injected deps | Responsibilities | Cohesion | Coupling | Why it grew | Risk | Action | Class |
|---|---|---|---|---|---|---|---|---|---|---|
| `service/impl/GeminiAiScoringService.java` | 1105 | 1 (+40 private) | 5 | orchestration, prompt building ×3, DQ statistics (quantile/mean/stddev), JSON parsing, config getters, note synthesis | Low-Med (all scoring-related) | Med (WebClient, repos, Jackson) | prompt + heuristics accreted in one class | untestable statistics without AI client | EXTRACT `DataQualityStatistics` + `ScoringPromptBuilder` + `ScoringProperties` | **EXTRACT** |
| `service/impl/CompanyPayoutQueryServiceImpl.java` | 780 | 4 (+15 private) | 9 | formula doc endpoint, owner aggregation ×2, report assembly, pagination+search, price/pct resolution | Medium (read-side payout) | High (9 repos/services) | read-model assembly for complex payout views | medium — mostly reads | extract aggregation builder; keep as query service | **KEEP/EXTRACT-PARTS** |
| `service/impl/MyCreditServiceImpl.java` | 725 | 8 (+7 private) | 12 | listing/queries, expiry marking, batch views, retirement, certificate PDF, email, SSE, storage | Low (queries + retirement + notify + render) | High (12 deps) | feature = "everything about my credits" | medium | EXTRACT `RetirementService`; move notify to events | **EXTRACT** |
| `service/impl/CreditIssuanceServiceImpl.java` | 694 | ~6 | ~8 | issuance rules, serial numbering, persistence, notification | Medium-High (issuance pipeline) | Medium | genuine domain complexity | low-medium | keep; move notification side-effects out | **KEEP** |
| `service/impl/MarketplaceServiceImpl.java` | 658 | 6 (+7 private) | 5 | create listing (**310-line method**), update, cancel, credit-status math, response building | Low at method level | Medium | `listCreditsForSale` accumulated validation+resolution+mapping | **high** — the method is the marketplace | EXTRACT steps within method (validate/resolve/build) | **RESTRUCTURE (method)** |
| `service/impl/ProfitSharingServiceImpl.java` | 574 | ~5 | 12 | contribution calc, payout calc, transfers, distribution persistence, async email | Medium | High + `ApplicationContext` self-proxy hack (`:63-74`) | async + tx + notify in one class | medium | replace getSelf() with TransactionTemplate/separate bean; extract email | **RESTRUCTURE** |
| `service/GeminiAiService.java` | 527 | (interface file, large) | — | interface + (to verify) inline constants/impl | Status: UNKNOWN (file not fully read) | — | possible impl-in-interface drift | low | verify, then split | **VERIFY→SPLIT** |
| `service/impl/EmissionReportServiceImpl.java` | 527 | ~7 | ~9 | report submission, status transitions, verification workflow, file handling | High (workflow) | Medium | real workflow complexity | low | keep; add state-machine tests | **KEEP** |
| `service/impl/KycServiceImpl.java` | 427 | ~12 | ~8 | user/company/cva/admin KYC CRUD | Medium (multi-profile CRUD hub) | Medium | 4 profiles in one service | low | acceptable; optional split per profile | **KEEP** |
| `service/impl/WalletServiceImpl.java` | 405 | 6 | 7 | provisioning, read-model assembly (chain walking), deposit application, transferFunds | Low (CRUD + assembler + transfer + deposit) | Medium | wallet = 4 sub-responsibilities | **high — contains C-bug** | FIX bug; EXTRACT `WalletSummaryAssembler`, `DepositService` | **FIX→EXTRACT** |
| `service/impl/OrderServiceImpl.java` | 363 | 5 (+2 private) | 8 | create/cancel/read orders, settlement orchestration, financial record assembly | Medium (use-case cluster) | High (3 domains) | settlement spans 3 aggregates | **high — contains H-bugs** | FIX bugs; keep; later split command/settlement | **FIX→(later) SPLIT** |
| `controller/KycController.java` | 334 | 14 | 1 | KYC endpoints hub | High (thin delegation) | Low | 4 profiles × CRUD endpoints | low | DTO-ify the raw-entity return | **KEEP** |
| `config/DataInitializer.java` | 323 | 1 bean | 7 | roles, demo users, sample companies/projects/credits/listings seeding | Medium (seeding only) | Medium | sample data accretion | medium (credentials!) | profile-gate demo seeding | **RESTRUCTURE** |
| `exception/GlobalExceptionHandler.java` | 206 | 14 handlers | 0 | error mapping | High | Low | breadth of exception types | medium (500 leak) | fix leak; unify envelope | **IMPROVE** |

Not god-classes despite size: `EmissionReportServiceImpl`, `KycServiceImpl`, `CreditIssuanceServiceImpl` — cohesive workflow/CRUD code; splitting them now would be aesthetics, not engineering.

## Current → Target Responsibility Maps

### MarketplaceServiceImpl (method-level)

```text
Current: listCreditsForSale (310 lines)
 ├─ current user/company resolution
 ├─ request validation (quantity, expiry, price)
 ├─ owned-credit resolution + chain fallbacks
 ├─ listing entity creation + credit status math
 └─ response assembly

Target:
 ListingValidator          (pure, unit-tested)
 OwnedCreditResolver       (repository-backed)
 ListingFactory            (entity math: listedAmount/status)
 ListingResponseAssembler  (pure mapping)
 listCreditsForSale        (≤40 lines orchestration)
```

### GeminiAiScoringService

```text
Current:
 ├─ suggestScore orchestration
 ├─ buildProjectContext/buildDataContext/buildAnomalyContext
 ├─ computeDQMetrics + quantile/mean/stddev
 ├─ tryParseJson/extractText/safe
 ├─ buildRichNotes
 └─ 8 config getters (getDoubleProperty/getIntProperty)

Target:
 AiScoringService          (orchestration only)
 ScoringPromptBuilder      (pure functions)
 DataQualityStatistics     (pure, no AI client needed to test)
 ScoringProperties         (@ConfigurationProperties)
```

### WalletServiceImpl

```text
Current:
 ├─ generateWallet (provisioning)
 ├─ getUserWallet/addBalanceToWallet/findWallet* (read + deposit)
 ├─ resolveCarbonCreditSummaries + resolveRootCredit/resolveEffectiveBatch (read-model, with write side-effect at :179-180)
 └─ transferFunds (auditable transfer, bug at :379-380)

Target:
 WalletService             (provisioning + transfer — bug fixed)
 WalletSummaryAssembler    (pure read-model, no writes)
 DepositService            (verified crediting only — new, closes P0-1)
```
