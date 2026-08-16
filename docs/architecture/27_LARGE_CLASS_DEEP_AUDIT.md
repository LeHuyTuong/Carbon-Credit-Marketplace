# 27 — Large-Class Deep Audit

> Method-level verdicts per class. LOC is context only. Verdict vocabulary: KEEP AS-IS · EXTRACT PURE LOGIC · EXTRACT INFRASTRUCTURE · EXTRACT ORCHESTRATOR · SPLIT SERVICE · MOVE RESPONSIBILITY · REWRITE.

## 1. GeminiAiScoringService (1105 LOC)

- **Public API**: 1 method (`suggestScore`).
- **Method clusters**: orchestration (50-237) · context/prompt builders (239-490) · DQ statistics: quantile/mean/stddev/metrics (491-643) · JSON/text parsing-safety (343-425) · rich-notes synthesis (644-1012) · config getters ×8 (1013-1099).
- **Shared state**: static `JSON_BLOCK` pattern, `MAPPER`; otherwise stateless.
- **Deps**: AiVertexConfig, WebClient, 2 repositories — repositories used only for prompt-logging contexts.
- **Transactions**: none declared (jakarta import only).
- **Side effects**: DB writes via repos (prompt dumps), external Vertex call.
- **Concurrency**: static ObjectMapper (thread-safe), OK.
- **Test coverage**: none.
- **Verdict**: **EXTRACT PURE LOGIC** (DataQualityStatistics + prompt builders are pure and instantly unit-testable) **+ EXTRACT INFRASTRUCTURE** (config getters → `@ConfigurationProperties`).

## 2. CompanyPayoutQueryServiceImpl (780)

- **Clusters**: formula-doc endpoint · owner aggregation ×2 overloads · report assembly · pagination/search · price/pct resolution helpers.
- **Deps**: 9 (repos + DynamicPricingService + properties) — read-only usage.
- **Test coverage**: none.
- **Verdict**: **KEEP AS-IS** (query/read-model service; cohesion acceptable). Optional later: extract `OwnerAggregation` builder if payout views grow. Not worth a phase of its own.

## 3. MyCreditServiceImpl (725, 12 deps)

- **Clusters**: listing/queries · expiry marking · batch views · retirement (write path) · certificate PDF render · email · SSE · storage.
- **Verdict**: **EXTRACT ORCHESTRATOR** → `RetirementService` (write path + notify) separate from read paths. PDF/email/SSE belong behind notification/rendering helpers. Priority: after money fixes (retirement touches inventory).

## 4. CreditIssuanceServiceImpl (694)

- **Clusters**: issuance rules · serial allocation · persistence (loop inserts) · notification. `issueTradeCredit` (567-625) is the settlement-critical piece — `intValueExact()` contract (integral-only) now documented (audit correction C1).
- **Concurrency**: serial allocation atomicity UNKNOWN (I11) — verify `SerialNumberService`.
- **Verdict**: **KEEP AS-IS** + move notification side-effects out when convenient. Do not split during correctness phase.

## 5. MarketplaceServiceImpl (658)

- **The** problem is one method: `listCreditsForSale` (60-371, ~310 lines) — validation, resolution, entity math, response building inline; 8 existing tests cover the behavior.
- **Verdict**: **EXTRACT PURE LOGIC** (steps within the method: Validator / OwnedCreditResolver / ListingFactory / ResponseAssembler) — refactor is test-guarded; do it *after* P0 (it is marketplace-critical code).

## 6. ProfitSharingServiceImpl (574, 12 deps + self-proxy)

- **Clusters**: contribution aggregation · payout computation (scale/rounding discipline is careful — `setScale(2, HALF_UP)` at construction) · per-owner F2 transfers · distribution persistence · async email.
- **Key structural fact**: async entry point reads wallet balance **before** per-owner locked transfers (F5) — partial-failure leaves distribution PROCESSING with no restart path.
- **Verdict**: **MOVE RESPONSIBILITY** (email out) + **RESTRUCTURE** the distribution to compute-payments-then-transfer under one consistent plan; replace `getSelf()` with `TransactionTemplate`. Sequenced after P0-B.

## 7. EmissionReportServiceImpl (527)

- Workflow/state-transitions + file handling; cohesive.
- **Verdict**: **KEEP AS-IS**; add state-transition tests in P1-test wave.

## 8. KycServiceImpl (427)

- 4 profile families (user/company/cva/admin) × create/update/list. Missing role gates on cva/admin creation (N8) — that's an authz fix, not a structural one.
- **Verdict**: **KEEP AS-IS** (+authz fix). Optional per-profile split only if profiles diverge further — not now.

## 9. WalletServiceImpl (405)

- **Clusters**: provisioning · read-model assembly with chain-walking (+ hidden write at `:179-180`) · deposit application · transferFunds.
- Contains SC-2 (ledger bug) and the deposit crediting target of SC-1.
- **Verdict**: **FIX FIRST**, then **EXTRACT ORCHESTRATOR** (`DepositService`) **+ EXTRACT PURE LOGIC** (`WalletSummaryAssembler`, removing the write-in-read).

## 10. OrderServiceImpl (363)

- Use-case cluster; settlement is the heart (N6/N1/S2 live here).
- **Verdict**: **FIX FIRST** (idempotency under lock, wallet locks, ERROR out-of-band, caller check). Later optional **SPLIT** into OrderCommand/OrderSettlement — only if settlement keeps growing.

## 11. PaymentServiceImpl (254)

- **Clusters**: order creation · fake verification · Stripe link · PayPal link · VNPay order/status.
- The verification stub is the P0.
- **Verdict**: **REWRITE** (verification path) **+ EXTRACT INFRASTRUCTURE** (Stripe/PayPal/VNPay adapters behind `PaymentGateway` port — ADR-002). Keep link-creation logic (it works).

## Bonus — GeminiAiService (527, concrete @Service misnamed as interface-style)

- Chat + analytics + WebClient + repositories; overlaps with GeminiAiScoringService's stack.
- **Verdict**: **EXTRACT INFRASTRUCTURE** (shared `AiClient` port for both) — consolidates the two AI stacks (A2 debt).
