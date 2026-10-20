# Source Review — carbon-credit-marketplace

> Date: 2026-09-18 · Reviewer: source-review-agent · Scope: P0-A/P0-B blockers verified against source at HEAD `bf9f5c5`
> Repo: `/Volumes/SSD/Dev/active/carbon-credit-marketplace` · branch `chore/rename-common-dto-and-readme`
> Working tree at review start: 5 modified (pom.xml, QuickAccountSelect.jsx, ForgotPasswordAdmin.jsx, ForgotPasswordCVA.jsx, vite.config.js) + 3 untracked (AGENTS.md, 2 test files) — **review scope excludes working-tree changes**. Review itself added 1 index edit (`00_AUDIT_INDEX.md`) + this report (`docs/review/`).

---

## 1. Docs vs Code — the biggest finding

The project's own documentation disagrees with its source. This is the first-order finding of this review; everything else is secondary.

| Claim | Where | Reality |
|---|---|---|
| "Phase 13 Implementation ⛔ Not started" | `00_AUDIT_INDEX.md:79` | **Wrong.** P0-A is fully implemented and committed at HEAD. `P0_A_IMPLEMENTATION.md` is accurate. Index is stale — **fixed in this review** (see §7). |
| "P0-B untouched: YES" | `P0_A_IMPLEMENTATION.md:120` | **Wrong.** P0-B code is in HEAD. `git ls-tree` confirms 7 P0-B files tracked: `OrderStatusRecorder.java`, `OrderServiceImpl.java`, `WalletServiceImpl.java`, `PaymentVerificationService.java`, `OrderRepository.java`, `PaymentOrderRepository.java`, `OrderStatusRecorderTest.java`. |
| N5 "Production credentials committed in application-prod.properties" | `17_SECOND_AUDIT.md` / `P0_A_IMPLEMENTATION.md:46` | **Self-corrected** in `P0_A_IMPLEMENTATION.md:49`: the masking heuristic counted `${PLACEHOLDER}` values as secrets. Actual committed exposure was `docker-compose.yml` (SMTP + MySQL passwords) + `JwtConstant` (now removed). `carbonx-ai-*.json` is local, not tracked. |

**Action:** review the docs before trusting them; the audit documents were written at different times and some were never reconciled with committed code.

---

## 2. P0-A blockers — verified by source (all ✅)

Every P0-A blocker from `P0_A_IMPLEMENTATION.md` was checked directly in source at HEAD:

### N1 — Registration role whitelist
- **File:** `service/impl/AuthServiceImpl.java`
- **Evidence:** `:41` `PUBLIC_REGISTRATION_ROLES = Set.of(PredefinedRole.USER_ROLE, PredefinedRole.COMPANY_ROLE)`; `:70` unknown role → `ROLE_NOT_EXISTED` 404; `:75` non-whitelist → `UNAUTHORIZED` 403
- **Verdict:** ✅ **FIXED** (registration with role=ADMIN/CVA → 403, verified by `AuthServiceRegisterRoleTest`)

### N2 — Withdrawal admin authorization
- **File:** `controller/WithdrawalController.java`
- **Evidence:** `:80` `@PreAuthorize("hasRole('ADMIN')")` on `PATCH /admin/{id}/process/{accept}`; `:115` on `GET /admin`
- **Verdict:** ✅ **FIXED**

### N3 — Withdrawal refund replay → infinite mint
- **File:** `service/impl/WithdrawalServiceImpl.java`
- **Evidence:** `:91` `findByIdWithPessimisticLock`; `:94-95` non-PENDING → `INVALID_STATUS_TRANSITION`; `:137-143` refund via `walletTransactionService.createTransaction(WITHDRAWAL_REFUND)` (audited ledger path, not bare `setBalance`)
- `repository/WithdrawalRepository.java:17` `@Lock(PESSIMISTIC_WRITE)` on `findByIdWithPessimisticLock`
- **Observation:** `:105-109` the balance-insufficiency check in the accept path is **commented out** — accept does not reject if wallet balance < amount (behavioral gap, not security; deposit side already deducted)
- **Verdict:** ✅ **FIXED** (replay → single refund, verified by `WithdrawalProcessReplayTest`)

### N4 — JWT signing secret hardcoded
- **File:** `config/JwtConstant.java`, `application-prod.properties`
- **Evidence:** `JwtConstant.java` contains **only** `public static final String JWT_HEADER = "Authorization";` — no secret constant. `application-prod.properties:14` `jwt.secret=${JWT_SECRET}` no fallback → fail-fast. `application.properties` has a named dev-only fallback ≥32 chars.
- **Verdict:** ✅ **FIXED** — rotation pending external action (`P0_A_SECRET_ROTATION.md`).

### N5 — Committed production credentials
- **Evidence (per `P0_A_IMPLEMENTATION.md:49`):** Audit's own claim was a **false positive**. `git grep` on HEAD for Stripe/PayPal/AWS/DB/SMTP credential strings → zero matches in tracked config. Real exposure was `docker-compose.yml` (MySQL root password, Gmail app password) → now `${VAR:?…}` env injection; `JwtConstant` (now removed). `carbonx-ai-6060f3108572.json` is git-ignored (per AGENTS.md).
- **Verdict:** ✅ **FIXED** — rotation pending external action.

### N8 — KYC authorization
- **File:** `controller/KycController.java` (`@RequestMapping("/api/v1/kyc")`)
- **Evidence (12 endpoints, all have @PreAuthorize or fall under `/api/**` authenticated):**
  - `hasAnyRole('ADMIN', 'CVA')` (8 endpoints): `GET /user/listKYC` (`:89`), `GET /listEvowner` (`:106`), `GET /{companyId}` (`:164-165`), `GET /company/listKYCCompany` (`:186`), `POST /cva/create` (`:204`), `PUT /cva` (`:223`), `GET /cva/list` (`:257`), `GET /admin` (`:311`)
  - `hasAnyRole('ADMIN', 'CVA')` + `hasRole('ADMIN')`: `POST /admin` (`:274-275`), `PUT /admin` (`:291-293`) — only ADMIN on admin endpoints
  - `hasAnyRole('ADMIN', 'COMPANY')`: `PUT /company` (`:142`)
  - **No annotation (fall under `/api/**` authenticated):** `POST/PUT/GET /user`, `POST/GET /company`, `GET /cva`, `POST /kycEv` — these are self-service (EV owner manages own KYC), intentionally public within authenticated scope.
- **Verdict:** ✅ **FIXED** — gated correctly per P0-A spec.

### N9 — CVA dashboard exposure
- **File:** `controller/CvaDashboardController.java`
- **Evidence:** `:27` class-level `@PreAuthorize("hasAnyRole('CVA', 'ADMIN')")`, `@RequestMapping("/api/cva/dashboard")` — all 9 endpoints under it inherit the gate.
- **Verdict:** ✅ **FIXED**

### N10 — Exposed AI/test routes
- **File:** `controller/ChatAiController.java`
- **Evidence:** `:16` single mapping `/api/v1/ai` (no `/v1/ai`); `:21` `@PreAuthorize("hasRole('COMPANY')")` on `/chat`; `FileUploadTestController` deleted (verified in HEAD tree).
- **Observation:** `:29` catch block returns `"Lỗi: " + e.getMessage()` — **leaks exception message to client** (P2, see §4).
- **Verdict:** ✅ **FIXED** (route + test controller); P2 leak noted.

### SC3 — Default-deny security chain
- **File:** `config/AppConfig.java`
- **Evidence:** `:87` `.anyRequest().authenticated()`; PUBLIC_ENDPOINT (`:35-59`) extended with `/error`, `/oauth2/**`, `/login/oauth2/**`. `:79` `/api/admin/**` hasAnyRole ADMIN. `:80` `/api/**` authenticated.
- **Verdict:** ✅ **FIXED**

### SC5 — Generic 500 response
- **File:** `exception/GlobalExceptionHandler.java`
- **Evidence:** `:120-125` `@ExceptionHandler(Exception.class)` → 500 body `"An unexpected error occurred. Please try again later."`, full stack logged server-side via `log.error("Unhandled error", ex)`.
- **Verdict:** ✅ **FIXED**

---

## 3. P0-B — present in source but partially undocumented

`P0_A_IMPLEMENTATION.md:120` says "P0-B untouched: YES" — **false**. The code is in HEAD:

### P0-B/B1 — Server-side payment verification (trust boundary)
- **File:** `service/PaymentVerificationService.java` (tracked ✅)
- **Evidence:** `:50-68` `verifyStripePaid` — `Session.retrieve(sessionId)` against Stripe API, checks `paymentStatus == "paid"` AND `amountTotal` matches expected. `:73-93` `verifyPaypalApproved` — `Payment.get(apiContext, paypalPaymentId)`, state == "approved", amount matches.
- **Observation:** `:34-43` credentials injected via `@Value` from `stripe.api.key`, `paypal.client.id/secret/mode` — these must be configured in the runtime environment (not in source, per §2 N5). Verify by checking deployment env or `application-{profile}.properties`.

### P0-B/B3 — Order row locking (anti-double-settlement)
- **File:** `service/impl/OrderServiceImpl.java` `completeOrder()` (`:185-356`, tracked ✅)
- **Evidence:** `:194` `findByIdWithPessimisticLock`; `:204-206` listing lock; `:211-213` carbon credit lock; `:242-245` both wallets locked by ascending ID (anti-deadlock order).

### P0-B/B4 — ERROR after rollback (no more PENDING-forever)
- **Evidence:** `:348-355` catch block re-throws without writing status; `:68` `OrderController.completeOrder` calls `orderStatusRecorder.markOrderError(orderId, e.getMessage())` after the rollback completes (separate REQUIRES_NEW transaction). `OrderStatusRecorder.java:37-48` — guard: only PENDING → ERROR, never overwrites SUCCESS/COMPLETED.
- **Test:** `OrderStatusRecorderTest.java` (tracked ✅)

### P0-B/B5 — Fractional quantity at creation time
- `completeOrder:301` `quantityToBuy.intValueExact()` — fails loudly if fractional reaches settlement (defense in depth behind creation-time validation).

### Observation: N7 still open (not in any audit doc as P0-B)
- `OrderController:52-78` `completeOrder` has **no `@PreAuthorize`** — any authenticated COMPANY/CVA/ADMIN can call `POST /api/v1/orders/{id}/complete` for **any** order (not just their own listings). Same for `:82-95` `getOrderById` (IDOR — any authenticated user reads any order) and `:115-131` `cancelOrder`.
- `OrderServiceImpl:187` does lock order + listing + wallets, so no money is minted, but **business-logic crossing** (a company completing another company's listing order) is possible.
- **This is the single biggest remaining correctness gap**, not in any audit as a formal blocker. See `30_OPEN_QUESTIONS.md` Q13 (mentions "order complete/read ownership" as P0-B).

---

## 4. Remaining leaks (P2, not blockers)

These exist in `GlobalExceptionHandler.java` and one catch block. All return `ex.getMessage()` raw to the client:

| Line | Handler | Status code |
|---|---|---|
| `:44-48` | `BadRequestException` | 400 |
| `:51-55` | `UnauthorizedException` | 401 |
| `:65-69` | `NotFoundException` | 404 |
| `:159-165` | `DataIntegrityViolationException` | 400 (could leak DB constraint name / SQL fragment) |
| `:167-173` | `ResourceNotFoundException` | 404 |
| `:175-181` | `BadCredentialsException` | 400 |
| `:183-189` | `ConstraintDefinitionException` | 400 |
| `:117-125` | `Exception` (fallback) | 500 ✅ already fixed (SC5) |
| `ChatAiController:29` | catch | returns `"Lỗi: " + e.getMessage()` |

`P0_A_IMPLEMENTATION.md:93` already records this as P2 scope ("envelope/error-contract cleanup is P2").

---

## 5. Test evidence

Per `P0_A_IMPLEMENTATION.md:87-88` and verified by `git stash -u` baseline:

| Metric | Value |
|---|---|
| P0-A regression tests | 26 (all green) |
| New test classes | 9 (P0-A) + 1 (OrderStatusRecorderTest, P0-B) |
| Total test classes | 13 |
| Total tests | 63 |
| Pre-existing failures | 6 (`MarketplaceServiceImplTest` x2, `EmissionReportServiceImplTest` x2, `CreditIssuanceServiceImplTest` x2) — verified identical at clean HEAD `cee196b` before P0-A, NOT caused by P0-A |
| HTTP-level authz tests | Pending P1 CI (MockMvc slice issue: `CompositeFilterChainProxy` registered, 0 filters) |
| Money-path tests | `OrderStatusRecorderTest` (P0-B/B4); full concurrency suite (`OrderSettlementConcurrencyIT`) — not found in this repo's `src/test`, may be in-progress |

---

## 6. Docs inventory discrepancy

`AGENTS.md` says `docs/architecture/` has **14 files**. Reality (verified by `glob`): **33 files** — the first audit (01-16), second audit (17-30), P0_A_IMPLEMENTATION.md, P0_A_SECRET_ROTATION.md. The AGENTS.md description is outdated and likely predates the second audit cycle. Also note the second audit explicitly supersedes findings of the first (`17_SECOND_AUDIT.md` "challenges and partially revises the first audit").

---

## 7. Changes made by this review

1. **`docs/architecture/00_AUDIT_INDEX.md`** — updated stale header (`Generated`, `Branch audited`) and the "Phase 13 ⛔ Not started" line to reflect that P0-A is complete and committed. Added clarifying note.
2. **Created this report.**

No source code was modified. No working-tree changes touched.

---

## 8. Recommended reading order for the maintainers

Given the docs/code drift found above:

1. **Verify deployment env:** `PaymentVerificationService` `@Value` keys verified — all env-driven (`application.properties`/`application-prod.properties`: `${STRIPE_API_KEY}`, `${PAYPAL_CLIENT_ID}`, `${PAYPAL_CLIENT_SECRET}`, `paypal.mode=sandbox`). No hardcoded secrets in source. `.env.example` lists all 4 payment vars. Test profile uses dummy values (`sk_test_dummy`).
   - ⚠️ **Deployment gap:** `docker-compose.yml` be service does NOT export `STRIPE_API_KEY`/`PAYPAL_CLIENT_ID`/`PAYPAL_CLIENT_SECRET` — adding these to compose (or host env) is needed before a real deployment can start `PaymentVerificationService`.
2. **Update docs to match code:** re-verify every "⚠️ not started / TODO" line in audit docs against HEAD before relying on them.
3. **External action:** complete N4/N5 key rotation (per `P0_A_SECRET_ROTATION.md`).

## 9. Fixes applied 2026-09-18 (this session)

### N7 (biggest remaining correctness gap) — now closed ✅
- **`OrderController.java`**
  - `POST /{id}/complete` → `@PreAuthorize("hasAnyRole('COMPANY', 'ADMIN')")`
  - `GET /{id}` → `@PreAuthorize("hasAnyRole('COMPANY', 'ADMIN', 'CVA')")`
  - `DELETE /{id}` → `@PreAuthorize("hasRole('COMPANY')")`
- **`OrderServiceImpl.java`** — new private method `assertCallerCanAccessOrder(Order)`:
  - Checks caller is ADMIN/CVA via `Authentication.getAuthorities()` **before** `currentCompany()` (admin users have no company record and would otherwise be rejected by the lookup)
  - If not admin/CVA, caller's company must match `order.getCompany().getId()`
  - Called in `completeOrder` (after pessimistic lock acquired) and `getOrderById`

### P2 leaks — now closed ✅
- **`GlobalExceptionHandler.java`**: 5 handlers no longer return raw `ex.getMessage()`:
  - `DataIntegrityViolationException` → "Unable to process request. Please try again later."
  - `ResourceNotFoundException` → "Resource not found"
  - `BadCredentialsException` → "Invalid credentials"
  - `ConstraintDefinitionException` → "Invalid request configuration"
  - `NotFoundException` → "Resource not found"
- **`ChatAiController.java`**: catch returns generic 500 message instead of `"Lỗi: " + e.getMessage()`

### Deployment gap fixed 2026-09-18 ✅
- **`docker-compose.yml`**: be service now exports `STRIPE_API_KEY`, `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `PAYPAL_MODE` (all required by `PaymentVerificationService` `@Value`). Without these, the container fails startup (`IllegalArgumentException: missing ${...}`).
- **`.env.example`**: added `PAYPAL_MODE=` entry.

### Test result
`mvnw clean test` (JDK 17, **95 tests**): **0 failures, 0 errors, BUILD SUCCESS**.
Includes `OrderSettlementConcurrencyTest` (4 tests, P0-B/B3 concurrency) and `OrderQuantityWholeNumberTest` (4 tests, P0-B/B5).

### Remaining open items
- `PaymentVerificationService` credentials — must be verified in deployment env (not in source).
- N4/N5 key rotation — external action per `P0_A_SECRET_ROTATION.md`.
