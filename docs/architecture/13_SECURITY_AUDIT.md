# 13 — Security Audit

> Severity: Critical / High / Medium / Low. Evidence = file:line read directly.

## SC1 — CRITICAL: Client-trusted deposit confirmation (free money)

- **Where**: `WalletController.addMoneyToWallet` (`POST /api/v1/wallet/deposit?order_id&payment_id`, lines 80-106) → `PaymentServiceImpl.processPaymentOrder` (lines 113-140).
- **Evidence**: `PaymentServiceImpl:126` — `boolean paymentConfirmed = true; // hoặc verifyPaymentFromGateway(paymentId)`; `paymentId` accepted but never used; no check that `order.getUser()` is the caller; credit applied to *current user's* wallet via `addBalanceToWallet(order.getAmount())` (`WalletController:95`).
- **Exploit paths** (both trivial): (1) `POST /api/v1/payment` (create PENDING order, amount N, no payment) → `POST /wallet/deposit?order_id=…` → wallet +N; (2) use another user's `order_id` to steal their deposit.
- **Fix**: Phase 1 (server-side verification + ownership + single-transaction idempotent credit). Sources: Stripe/PayPal webhook docs (06 §F2).

## SC2 — CRITICAL: Committed secrets

- **Evidence**: `docker-compose.yml` (be service): real Gmail SMTP address + app password (`SPRING_MAIL_PASSWORD: vzzv…`); MySQL root password `12345` in two places. `application-local.properties` (in worktree) holds live local credentials. Demo account passwords hardcoded in `DataInitializer:43-58` and published in README for the public deployment.
- **Fix**: rotate Gmail app password immediately; move compose creds to `.env` (gitignored); profile-gate demo seeding.

## SC3 — HIGH: Default-open security chain + test endpoints

- **Evidence**: `AppConfig:81` `.anyRequest().permitAll()`; `FileUploadTestController` (`/api/test/upload1e`) with unbounded multipart→S3 upload and println logging of filenames; Swagger public (intentional, acceptable for now).
- **Fix**: `.anyRequest().authenticated()`; delete test controller.

## SC4 — HIGH: VNPay return never credits wallet; Stripe/PayPal have no verification at all

- **Evidence**: `VNPayService.orderReturn:110-118` (status flip only); `PaymentServiceImpl:143-228` creates links, no webhook endpoints exist anywhere.
- **Fix**: Phase 1 gateway adapters + webhooks.

## SC5 — MEDIUM: 500 responses leak internal exception messages

- **Evidence**: `GlobalExceptionHandler:118-124` returns `ex.getMessage()` (may contain SQL fragments, class names, file paths).
- **Fix**: generic message + server-side logged trace id.

## SC6 — MEDIUM: Sensitive data in logs/stdout

- **Evidence**: `JwtTokenValidator:90` prints user emails per request; `PaymentServiceImpl:165` prints Stripe session object; `AppConfig:116` prints OAuth emails; AI configs log request URLs (`AiConfig:77`, `VertexWebClientConfig:75`).
- **Fix**: SLF4J debug-level, masked.

## SC7 — MEDIUM: OAuth2 token in redirect URL

- **Evidence**: `AppConfig:117` `redirect(frontendUrl + "/oauth-success?token=" + token)` — JWT leaks via browser history/referrer/proxy logs.
- **Fix**: short-lived one-time code exchanged for token, or httpOnly cookie.

## SC8 — MEDIUM: Authorization gaps (IDOR) & disabled checks

- **Evidence**: `OrderServiceImpl.getOrderById:126-138` (no ownership check; contrast `cancelOrder:165-167`); `WalletController:143` `@PreAuthorize("hasRole('ADMIN')")` commented out on all-transactions count; admin endpoint matcher only covers `/api/admin/**` while many admin-ish endpoints live under `/api/v1/**`.
- **Fix**: ownership checks; re-enable `@PreAuthorize`; inventory admin endpoints vs matcher.

## SC9 — LOW-MEDIUM: CORS misconfiguration (dead wildcards) + unnecessary httpBasic

- **Evidence**: `AppConfig:129-147` — `setAllowedOrigins` with pattern strings (`http://192.168.*.*:*`, `http://127.0.0.1:*`) that only work with `setAllowedOriginPatterns`; they silently never match (dead config masking intent). `httpBasic(withDefaults())` enabled (`:88`) without need.
- **Fix**: `setAllowedOriginPatterns` if patterns intended; drop httpBasic.

## SC10 — LOW: Input/file handling

- **Evidence**: `FileUploadTestController` accepts arbitrary multipart (removed in Phase 0); KYC multipart endpoints exist with no visible size/type limits beyond Spring defaults (Status: UNKNOWN — needs config review); CSV import path (`CsvBatchException`) validates batch-wise.
- **Fix**: multipart size caps in properties; content-type allowlist.

## SC11 — LOW-MEDIUM: Dependency risks

- **Evidence**: jjwt 0.11.5 (older line; 0.12.x current); PayPal `rest-api-sdk` 1.14.0 (deprecated SDK); `org.json` 20231013 alongside Jackson; duplicate pom entries (`pom.xml:163-167 vs 176-180`, `197-200 vs 256-259`).
- **Fix**: upgrades scheduled Phase 2/long-term; dedupe now.

## Positives (recorded so they are not "fixed" away)

BCrypt; JWT expiry/validation filter; VNPay HMAC verify; rate limiting; cookie-based OAuth2 request repository; `DelegatingSecurityContextAsyncTaskExecutor`; `@Valid` DTO validation; parameterized queries only (no string-concatenated SQL found — JPQL/derived queries throughout).
