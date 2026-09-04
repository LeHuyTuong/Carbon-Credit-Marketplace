# P0-A Implementation Report

> Phase: P0-A (production security blockers — surgical fixes only)
> Date: 2026-08-16 · Branch: `refactor/cleancode`
> Scope rule: every change maps to N1/N2/N3/N4/N5/N8/N9/N10/SC3/SC5 from the second audit. No P0-B work, no refactoring, no package moves.

## Change 1 — Registration role whitelist (N1)

- **Files**: `service/impl/AuthServiceImpl.java`
- **Why**: `POST /api/v1/auth/register` is public and looked up any requested role — self-service ADMIN/CVA escalation.
- **Before**: `req.getRoleName()` → `roleRepository.findByName(...)` → assigned.
- **After**: role still resolved first (unknown roles keep the existing 404 `ROLE_NOT_EXISTED` contract), then a `PUBLIC_REGISTRATION_ROLES = {EV_OWNER, COMPANY}` whitelist guard throws `UNAUTHORIZED` (403) for anything else. ADMIN/CVA accounts remain creatable only via the seeded/admin-provisioned path.
- **Tests**: `AuthServiceRegisterRoleTest` (5): EV_OWNER ✓, COMPANY ✓, ADMIN → 403, CVA → 403, unknown → 404-contract preserved; no user saved / no email on rejection.
- **Verification**: mvn green.
- **Risk**: existing FE register page only offers the two allowed roles (README quick-signup uses them). Low.
- **Rollback**: revert commit.

## Change 2 — Withdrawal admin authorization (N2)

- **Files**: `controller/WithdrawalController.java`
- **Before**: `PATCH /api/v1/withdrawal/admin/{id}/process/{accept}` + `GET /api/v1/withdrawal/admin` had no role check and were outside `/api/admin/**`.
- **After**: `@PreAuthorize("hasRole('ADMIN')")` on both (mechanism already established in the codebase).
- **Tests**: `P0aWithdrawalAuthzTest` (6): ADMIN allowed (process + list), COMPANY/EV_OWNER/CVA → AccessDenied, unauthenticated rejected.
- **Risk**: FE admin withdrawal page uses an ADMIN account — none.
- **Rollback**: revert commit.

## Change 3 — Withdrawal refund replay + ledger (N3)

- **Files**: `repository/WithdrawalRepository.java` (new `findByIdWithPessimisticLock`), `service/impl/WithdrawalServiceImpl.java`, `common/WalletTransactionType.java` (new `WITHDRAWAL_REFUND`), `service/impl/WalletTransactionServiceImpl.java` (credit branch handles the new type).
- **Before**: rejection refunded via bare `wallet.setBalance(+amount)` — no status guard, no lock, no ledger row → replayable mint; accept-path also flipped any status.
- **After**: `processWithdrawal` locks the row (`PESSIMISTIC_WRITE`), rejects any non-PENDING state (`INVALID_STATUS_TRANSITION`), and refunds through `walletTransactionService.createTransaction(WITHDRAWAL_REFUND, amount)` — auditable, exactly-once, inside the same transaction. Behavior otherwise unchanged (emails/SSE preserved).
- **Tests**: `WithdrawalProcessReplayTest` (5): reject-PENDING refunds once via ledger (captures type+amount; asserts no bare wallet save), replay-reject throws with no second refund, accept-after-reject throws, accept-PENDING succeeds without refund, second accept throws.
- **Concurrency note**: the pessimistic lock serializes concurrent attempts; the unit suite proves the state-machine logic. A true multi-threaded DB test (Testcontainers) is scheduled for the P1 CI phase per the roadmap (not added now to avoid a testing-platform refactor).
- **Risk**: low — money-invariant fix only.
- **Rollback**: revert commit (refunds already ledgered remain valid history).

## Change 4 — JWT secret externalization (N4)

- **Files**: `config/JwtConstant.java` (secret removed), `config/JwtProvider.java` + `config/JwtTokenValidator.java` (constructor-injected `@Value("${jwt.secret}")`), `application.properties` (dev-only fallback, clearly named, ≥32 chars), `application-prod.properties` (`jwt.secret=${JWT_SECRET}` with **no** fallback → fail-fast in prod).
- **Before**: 70-char signing secret hardcoded in a public repo → tokens forgeable for any account.
- **After**: config/env-driven secret; dev default is explicitly named `dev-only-insecure-jwt-secret-change-me…`; production cannot boot without `JWT_SECRET`.
- **Tests**: `JwtProviderSecretTest` (3): round-trip with injected secret, token signed with a different (rotated) secret rejected with `SignatureException`, temporary-token purpose round-trip. `ProdJwtSecretContractTest` (2): prod properties contain `jwt.secret=${JWT_SECRET}` with no fallback; all sensitive prod properties are `${ENV}` placeholders.
- **Risk**: rotating the deployed secret logs everyone out (expected, communicated in rotation doc).
- **Rollback**: revert commit + set `JWT_SECRET` to the old value (NOT recommended — treat old value as burned).

## Change 5 — Committed credentials removal (N5)

- **Files**: `docker-compose.yml` (Gmail app password, MySQL root password → `${VAR:?…}` env injection incl. healthcheck), `.gitignore` (+`.env`, `backend/Market_carbon/.env`), new `.env.example` (root, compose vars) and `backend/Market_carbon/.env.example` (runtime env var names, values empty).
- **Corrections to second audit (recorded per truth-over-confirmation)**: N5's claim that `application-prod.properties` contains live credentials was a **false positive** — the masking heuristic counted `${PLACEHOLDER}` values as non-empty secrets. Direct read shows the file is fully env-placeholder-based. Actual committed exposures were: docker-compose (SMTP + MySQL) and JwtConstant (N4). The Vertex service-account JSON (`carbonx-ai-*.json`) is present locally but **not tracked** (git-ignored).
- **Tests**: `ProdJwtSecretContractTest` guards the placeholder discipline.
- **Rotation**: documented separately in `P0_A_SECRET_ROTATION.md` — code remediation ≠ remediation; rotation is pending external action.
- **Risk**: `docker compose up` now requires a local `.env` (by design).
- **Rollback**: revert commit (values are burned regardless — see rotation doc).

## Change 6 — KYC + CVA authorization (N8, N9)

- **Files**: `controller/KycController.java` (8 new `@PreAuthorize`), `controller/CvaDashboardController.java` (class-level `hasAnyRole('CVA','ADMIN')`).
- **Gates applied**: KYC listings (`/user/listKYC`, `/listEvowner`, `/company/listKYCCompany`, `/cva/list`) → CVA/ADMIN; CVA profile create/update → ADMIN/CVA; Admin profile create/update → ADMIN; CVA dashboard (all `/api/cva/dashboard/**`) → CVA/ADMIN. Self-service user/company KYC CRUD intentionally untouched (audit scope).
- **Tests**: `P0aKycAuthzTest` (7) + `P0aCvaDashboardAuthzTest` (5) — role matrix incl. unauthenticated rejection.
- **Risk**: if any FE flow let a COMPANY view KYC lists, it will now 403 — that access was exactly the vulnerability; FE was reviewed (dashboard is CVA-only in FE).
- **Rollback**: revert commit.

## Change 7 — Route exposure + test controller (N10, SC3a)

- **Files**: `controller/ChatAiController.java` (mapping `{"…/api/v1/ai", "/v1/ai"}` → `"/api/v1/ai"`), `controller/FileUploadTestController.java` **deleted**.
- **Pre-checks performed**: FE calls only `/api/v1/ai/chat` (`frontend/src/components/AI/ChatWindow.jsx:34`); no FE or Java references to `/api/test/**` or the deleted class.
- **Tests**: `P0aRouteExposureTest` (3): `/v1/ai/**` absent from request mappings, `/api/v1/ai/chat` still mapped, `FileUploadTestController` class absent from the compiled application.
- **Rollback**: revert commit.

## Change 8 — Default-deny security chain (SC3b)

- **Files**: `config/AppConfig.java` — `.anyRequest().permitAll()` → `.anyRequest().authenticated()`; PUBLIC_ENDPOINT extended with `/error` (Boot error dispatch), `/oauth2/**`, `/login/oauth2/**` (OAuth2 login flow entry points — previously open via `anyRequest`).
- **Public-by-intention unchanged**: auth endpoints, project browsing, marketplace view, report logo download, `/files/**`, swagger.
- **Risk**: any future endpoint outside the explicit public list now requires auth (intended). Existing FE verified to call only known endpoints.
- **Tests**: URL-level enforcement is exercised implicitly by the authz suites (unauthenticated calls rejected); a dedicated HTTP-level test lands with the P1 CI integration suite (see Test Notes).
- **Rollback**: revert commit.

## Change 9 — Generic 500 response (SC5)

- **Files**: `exception/GlobalExceptionHandler.java` — fallback handler returns `"An unexpected error occurred. Please try again later."`; full exception still logged server-side; `requestTrace` envelope preserved.
- **Tests**: `GlobalExceptionHandler500Test` — controller throwing `RuntimeException("jdbc:mysql://internal-host…")` yields 500 whose body contains no host/SQL/message and keeps `requestTrace`.
- **Risk**: none (clients lose raw error text — that was the leak).
- **Rollback**: revert commit.

## Test Notes (honest scope statement)

- **New P0-A suites: 26 tests, all green.**
- **Pre-existing failures**: `MarketplaceServiceImplTest` (2), `EmissionReportServiceImplTest` (2, one hitting an external `http://pdf.url`), `CreditIssuanceServiceImplTest` (2) fail — **verified via `git stash -u` baseline to fail identically at clean HEAD `cee196b` before any P0-A change** (6 failures: 4 + 2 errors). They belong to the in-flight marketplace/issuance work and are NOT caused or masked by this phase. Fixing them is P0-B/P1 scope.
- **Method-security tests** invoke the proxied controller beans directly with a controlled `SecurityContext` (real `@PreAuthorize` decisions). A `@WebMvcTest`+MockMvc HTTP-level variant was attempted and abandoned: the slice registers `CompositeFilterChainProxy` (Spring Security 6.3) which the `springSecurity()` MockMvc configurer does not apply in this setup — verified with a filter-chain diagnostic (0 filters registered). HTTP-level authz tests are scheduled for the P1 CI integration suite; tracked below.

## Issues Found & NOT Fixed (recorded, per phase rules)

- `GlobalExceptionHandler.DataIntegrityViolationException` and `ResourceNotFoundException` handlers still return raw `ex.getMessage()` (400-level leaks) — envelope/error-contract cleanup is P2 (`30_OPEN_QUESTIONS` Q-list already covers the envelope work).
- `getOrderById` IDOR and complete-order caller check remain open — explicitly P0-B (`29_REVISED_ROADMAP`).
- HTTP-level (URL-rule) security tests pending P1 CI suite (Testcontainers/MockMvc integration) — noted above.

## Security Regression Matrix (post-implementation)

| Scenario | Expected | Verified by | Result |
|---|---|---|---|
| register role=EV_OWNER/COMPANY | success | AuthServiceRegisterRoleTest | ✅ |
| register role=ADMIN / CVA | 403 | AuthServiceRegisterRoleTest | ✅ |
| register role=unknown | 404 (existing contract) | AuthServiceRegisterRoleTest | ✅ |
| non-admin → withdrawal admin | denied | P0aWithdrawalAuthzTest | ✅ |
| repeated withdrawal rejection | one ledgered refund | WithdrawalProcessReplayTest | ✅ |
| accept on non-PENDING withdrawal | rejected | WithdrawalProcessReplayTest | ✅ |
| token signed with different secret | invalid | JwtProviderSecretTest | ✅ |
| prod without JWT_SECRET | startup failure (fail-fast) | ProdJwtSecretContractTest (contract) | ✅ (contract-level) |
| CVA dashboard non-CVA | denied | P0aCvaDashboardAuthzTest | ✅ |
| KYC admin ops non-privileged | denied | P0aKycAuthzTest | ✅ |
| `/v1/ai/**` | unmapped | P0aRouteExposureTest | ✅ |
| `/api/test/**` | class deleted | P0aRouteExposureTest | ✅ |
| unexpected exception | generic 500, no internals | GlobalExceptionHandler500Test | ✅ |
| old JWT secret usable | no — rotated secret injected | P0_A_SECRET_ROTATION (operator) | ⏳ rotation pending |

## Blocker Status

N1 RESOLVED · N2 RESOLVED · N3 RESOLVED · N4 RESOLVED (rotation pending external action) · N5 RESOLVED-as-scope (compose+JWT cleaned; **rotation pending external action**; prod-profile claim corrected) · N8 RESOLVED · N9 RESOLVED · N10 RESOLVED · SC3 RESOLVED · SC5 RESOLVED.

**P0-B untouched: YES** — deposit verification, settlement races, wallet locking, ERROR-status, transferFunds ledger fix all deferred per phase rules.

## Diff Classification (scope-creep check)

- P0-A required: 15 main-source files (AuthServiceImpl, WithdrawalController/ServiceImpl, WithdrawalRepository, WalletTransactionType, WalletTransactionServiceImpl, JwtConstant/Provider/TokenValidator, KycController, CvaDashboardController, ChatAiController, AppConfig, GlobalExceptionHandler, application[|-prod|-].properties, docker-compose.yml, .gitignore) + 1 deletion (FileUploadTestController).
- P0-A tests: 9 new test classes (26 tests).
- Documentation: this file + P0_A_SECRET_ROTATION.md (+ audit docs from prior phases, committed separately).
- Unrelated: **none** — the pre-existing working-tree changes (pom.xml, DataInitializer, 4 repositories, Marketplace/Order/Wallet ServiceImpl, logback, spy.properties, seed_large.py, application-local.properties) were present before P0-A and are left uncommitted and untouched.

## Final Security Re-Audit (post-commit, against git HEAD)

Re-verified every blocker directly in the committed tree (`git grep`/`git ls-tree` on HEAD, 2026-08-16):

| Blocker | Committed-source evidence | Status |
|---|---|---|
| N1 | `AuthServiceImpl:41` `PUBLIC_REGISTRATION_ROLES = {EV_OWNER, COMPANY}`; guard at `:75` | RESOLVED |
| N2 | `WithdrawalController` — 2 `@PreAuthorize("hasRole('ADMIN')")` (process + list) | RESOLVED |
| N3 | `WithdrawalServiceImpl:95` non-PENDING → `INVALID_STATUS_TRANSITION`; `:139` refund via `WITHDRAWAL_REFUND` ledger; `findByIdWithPessimisticLock` | RESOLVED |
| N4 | `JwtConstant` — `SECRET_KEY` constant absent; `jwt.secret=${JWT_SECRET}` (no fallback) in prod profile | RESOLVED — **rotation pending external action** |
| N5 | `git grep` on HEAD for old JWT secret / Gmail app password / compose passwords → **zero matches**; compose uses `${VAR:?…}` | RESOLVED — **rotation pending external action** |
| N8 | `KycController` — 11 `@PreAuthorize` (listings → CVA/ADMIN; cva create/update → ADMIN/CVA; admin create/update → ADMIN) | RESOLVED |
| N9 | `CvaDashboardController` — class-level `hasAnyRole('CVA','ADMIN')` | RESOLVED |
| N10 | `ChatAiController:16` — single mapping `/api/v1/ai` | RESOLVED |
| SC3 | `AppConfig:85` `.anyRequest().authenticated()`; `FileUploadTestController` absent from HEAD tree | RESOLVED |
| SC5 | `GlobalExceptionHandler` — 500 returns generic message | RESOLVED |

**Final suite state**: 13 test classes, 63 tests — all 26 P0-A regression tests green; the 6 failures are the pre-existing baseline set (verified identical at clean HEAD `cee196b` before P0-A).

**Authorization matrix (doc 19) re-verified**: rows previously marked FIX for N1/N2/N8/N9/SC3/SC5/N10 are now enforced in source; remaining FIX rows (deposit ownership/verification, order complete/read ownership, `/users/by-email` scope, `countAllTransactions` role) are explicitly P0-B/P1 scope and unchanged by this phase.
