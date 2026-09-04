# 01 — Repository Map

> Evidence gathered 2026-08-16 by direct inspection (pom.xml, resources, tree walks, greps). File counts include the uncommitted working-tree diff.

## Technology Stack

| Layer | Technology | Evidence |
|---|---|---|
| Language | Java 17 (`pom.xml:16` `<java.version>17`) — note: `OPTIMIZATION_LOG.md` claims Java 21; the POM is authoritative | `backend/Market_carbon/pom.xml` |
| Framework | Spring Boot 3.3.3 via `dependencyManagement` import (no parent POM) | `pom.xml:17,27-37` |
| Web | Spring MVC (`spring-boot-starter-web`), WebFlux included **only** for WebClient (Vertex AI) | `pom.xml:44,187-189` |
| Security | Spring Security, jjwt 0.11.5, OAuth2 client (Google), BCrypt | `pom.xml:79-95,197-200`; `config/AppConfig.java` |
| Persistence | Spring Data JPA / Hibernate 6, MySQL 8 connector | `pom.xml:51-53,131-134` |
| Schema mgmt | `ddl-auto=update`; hand-written DDL dump at `backend/database/core_ccm.sql` (db `core_ccm3`); **no Flyway/Liquibase** | `application.properties`; `backend/database/` |
| Cache / KV | Redis — used **only** by `OtpServiceImpl` (grep: RedisTemplate appears in 1 file) | `service/impl/OtpServiceImpl.java` |
| Payments | VNPay (custom HMAC-SHA512 impl), Stripe java 24.9.0, PayPal `rest-api-sdk` 1.14.0 (legacy) | `pom.xml:182-195`; `service/VNPayService.java` |
| Cloud / AI | AWS S3 SDK 2.25.48; Google `google-genai` 1.24.0 **and** parallel Vertex AI WebClient stack (`AiConfig` + `AiVertexConfig` + `VertexWebClientConfig`) | `pom.xml:137-150,226-254` |
| Docs / PDF | springdoc-openapi 2.6.0; PDFBox, flying-saucer, openhtmltopdf, POI, commons-csv/text | `pom.xml:152-278` |
| Mapping | MapStruct 1.5.5 in annotation-processor path — **dead**: both `mapper/*.java` have `@Mapper` commented out | `mapper/UserMapper.java:10` |
| Rate limiting | Bucket4j 7.6.0 + custom interceptor | `config/RateLimitConfig.java` |
| Frontend | React 18, Vite, MUI 7 + React-Bootstrap + Redux Toolkit, axios, formik/yup, Nivo/Chart.js | `frontend/package.json` |

## Build System

Maven (wrapper not committed; system `mvn`). Plugins: compiler 3.11.0 (with `-parameters`, Lombok+MapStruct processors), `spring-boot-maven-plugin` (repackage). No surefire config, no JaCoCo, no static-analysis plugins, no envelope CI.

## Runtime

Single Spring Boot jar (port 8082, context `/api` implied by controller mappings), Java 17. Frontend built to static bundle served by nginx (`frontend/nginx.conf`, Docker).

## Main Modules (logical domains — all inside one Maven module)

| Domain | Controllers | Services (LOC of impl) | State |
|---|---|---|---|
| Auth / User / KYC | AuthController, UserController, KycController (334) | AuthServiceImpl 184, KycServiceImpl 427, UserServiceImplementation 266 | Active |
| Wallet & ledger | WalletController, WithdrawalController | WalletServiceImpl 405, WalletTransactionServiceImpl 279, WithdrawalServiceImpl 169 | **Bug-critical** (see 13_SECURITY / 04 debt) |
| Payments | PaymentController, VNPaymentController, PaymentDetailsController | PaymentServiceImpl 254, VNPayService 125 | **Bug-critical** |
| Credit lifecycle | CarbonCreditController, CreditIssuanceController, MyCreditController (254) | CreditIssuanceServiceImpl 694, MyCreditServiceImpl 725, CarbonCreditServiceImpl 251 | Active |
| Marketplace & orders | MarketplaceController, OrderController | MarketplaceServiceImpl 658, OrderServiceImpl 363 | Bug-critical |
| Profit sharing | ProfitSharingController, CompanyPayoutController | ProfitSharingServiceImpl 574, CompanyPayoutQueryServiceImpl 780 | Active |
| Emissions / CVA | EmissionReportController 257, ReportController, ReportAnalysisController, CvaDashboardController 171 | EmissionReportServiceImpl 527, ReportAnalysisService 342, analysis/rules (10 files) | Active |
| AI | ChatAiController, EmissionAiController | GeminiAiScoringService 1105, GeminiAiService 527, VertexGeminiService | Two parallel stacks |
| Projects / vehicles | ProjectController, ProjectApplicationController 176, VehicleController 197 | ProjectServiceImpl 175, ProjectApplicationServiceImpl 304, VehicleServiceImpl 204, admin/VehicleControlServiceImpl 212 | Active |
| Notifications | NotificationController | SseService, helper/notification/* (4), EmailServiceImpl 292 | Active (SSE + @Async) |
| Admin / dashboard | AdminController, ApiController | DashboardCardService, service/admin/* | Active |

Package inventory: `controller/` 28 · `service/` 40 interfaces + `service/impl/` 33 · `repository/` 26 · `model/` 25 entities + BaseEntity · `dto/` 85 (request 44 / response 41 / analysis 3 / dashboard 4) · `config/` 25 · `exception/` 13 · `scheduler/` 4 · `common/` 25 (incl. validator 5, annotation 3) · `helper/notification/` 4 · `certificate/` 4 · `spec/` 1 · `mapper/` 2 (disabled) · `utils/` 6 + `utils/Tuong/` 3.

## Entry Points

1. HTTP API `/api/v1/**` (28 controllers) behind `JwtTokenValidator` + `RateLimitInterceptor`
2. Google OAuth2 login (`/oauth2/...`) — session-based island inside JWT app (`AppConfig:90-124`)
3. VNPay return controller (Thymeleaf view, `VNPaymentController:34-49`)
4. `@Scheduled`: `CreditExpiryScheduler` (daily 02:00, bulk update), `UserCleanupScheduler`
5. `DataInitializer` (ApplicationRunner) — seeds roles, demo accounts (admin/company/cva, fixed passwords), sample data each startup
6. SSE emitters via `NotificationController` + WebSocket config present

## Core Dependencies (internal)

Controller → Service interface → ServiceImpl → Repository → Model. Cross-domain service calls: marketplace→credit-issuance & wallet-transactions; profitsharing→wallet; payment→SSE. `SecurityContextHolder` read directly in ≥6 services.

## External Dependencies

VNPay gateway, Stripe API, PayPal API, AWS S3, Gmail SMTP, Google OAuth2, Google Gemini API, Google Vertex AI.

## Databases

MySQL 8 (`core_ccm`). Schema via `ddl-auto=update` + `backend/database/core_ccm.sql` reference dump. Indexes: only those implied by entity mappings; no evidence of query-driven index tuning (UNKNOWN — needs `EXPLAIN` review, see 12_PERFORMANCE_AUDIT).

## Caches

Redis for OTP only. No HTTP/entity caching despite `README` claiming "Redis caching".

## Messaging

None. In-process: SSE push, WebSocket config, `@Async("profitSharingTaskExecutor")` email/payout tasks.

## External APIs

Stripe Checkout (session create), PayPal Payments (legacy SDK), VNPay redirect+return, Gemini generateContent, Vertex AI endpoints, S3 object storage, SMTP.

## Deployment Model

docker-compose on a VPS: `db` (mysql:8.0, root/12345), `be` (profile `local`), `fe` (nginx). Public domain `carbonx.io.vn` behind Cloudflare (commit 29069ec "Fix: Cloudflare HTTP/2 protocol error"). Compose pins the Gmail SMTP password in plaintext.

## CI/CD

**None.** No `.github/workflows`, no build pipeline, no test automation outside developer machines.

## Observability Stack

logback (console + p6spy SQL file), SLF4J. No Actuator, no metrics, no tracing, no MDC correlation (trace header only echoed in response envelope). 10 `System.out.println` sites.

## Security Components

JWT filter + BCrypt + method security (`@EnableMethodSecurity(securedEnabled=true)`, `AppConfig:26`), OAuth2 login with cookie-based authorization-request repository, Bucket4j rate limit, VNPay HMAC verification, CORS allowlist. Weaknesses: `.anyRequest().permitAll()` default, unverified deposit flow, committed secrets (details in 13_SECURITY_AUDIT).

## Tests

4 test classes / 26 `@Test` (MarketplaceServiceImplTest 8, CreditIssuanceServiceImplTest 8, EmissionReportServiceImplTest 5, ProjectApplicationServiceImplTest 5). All plain Mockito unit tests in default package `com.carbonx.marketcarbon` (root, not mirrored). No integration tests, no Testcontainers, no ArchUnit.

## Repository Tree (backend, abridged)

```text
carbon-credit-marketplace/
├── backend/
│   ├── database/core_ccm.sql          # hand-written DDL reference dump
│   └── Market_carbon/                 # Spring Boot app (Maven)
│       ├── pom.xml                    # artifactId: spring-rest-auth-vehicles-kyc (template name)
│       └── src/
│           ├── main/java/com/carbonx/marketcarbon/{controller,service,service/impl,repository,
│           │      model,dto,config,common,exception,scheduler,helper,certificate,spec,mapper,utils,utils/Tuong}
│           ├── main/resources/        # application{,-local,-prod}.properties, logback-spring.xml,
│           │                          # spy.properties, seed_large.py, templates/, static/, fonts/, i18n (VN/CN/FR)
│           └── test/java/com/carbonx/marketcarbon/   # 4 test classes (flat)
├── frontend/                          # React 18 + Vite (~184 src files, pages/, components/, apiCVA/, apiAdmin/)
├── docker-compose.yml                 # db + be + fe (contains committed secrets)
├── docs/architecture/                 # ← this audit
└── (junk at root: daq, "et --hard f55ba43", "git log --graph ...", "t --rebase",
    "e -i HEAD~3", "git" (empty), img_1.png, img_2.png, uploads/, stray package-lock.json,
    01-card-issuing.md, CV_METRICS.md, OPTIMIZATION_LOG.md, profolio.code-workspace,
    .commandcode/, scratchpad-audit/, backend/Market_carbon/logs/)
```
