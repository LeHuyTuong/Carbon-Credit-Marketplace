# 09 — Package and Module Design (Target)

## Full Package Tree

```text
com.carbonx.marketcarbon
├── CarbonXApplication.java
├── shared
│   ├── api          ApiResponse (unified envelope) · PageResponse
│   ├── security     CurrentUserResolver · @CurrentUser annotations
│   └── error        ErrorCode · AppException hierarchy
├── config
│   ├── SecurityConfig (from AppConfig: filter chain, endpoint rules)
│   ├── CorsConfig · AsyncConfig · WebConfig · OpenApiConfig · RateLimitConfig
│   └── DemoDataSeeder (profile="demo")
├── auth
│   ├── api          AuthController (register/login/OTP/refresh)
│   └── service      AuthService · OtpService (Redis)
├── user
│   ├── api          UserController · KycController · dto/
│   ├── service      UserService · KycService
│   ├── repository   UserRepository · RoleRepository · CompanyRepository · EVOwnerRepository · CvaRepository · AdminRepository
│   └── model        User · Role · Company · EVOwner · Cva · Admin
├── wallet
│   ├── api          WalletController · WithdrawalController · dto/
│   ├── service      WalletService · WalletTransactionService · DepositService · WalletSummaryAssembler
│   ├── repository   WalletRepository · WalletTransactionRepository · WithdrawalRepository
│   └── model        Wallet · WalletTransaction · Withdrawal
├── payment
│   ├── api          PaymentController · WebhookControllers (vnpay/stripe/paypal)
│   ├── service      PaymentOrderService
│   ├── gateway      PaymentGateway (port: createPayment·verifyPayment·parseWebhook)
│   │   ├── vnpay/   VnPayAdapter (+ HMAC utils)
│   │   ├── stripe/  StripeAdapter
│   │   └── paypal/  PayPalAdapter
│   ├── repository   PaymentOrderRepository · PaymentDetailsRepository
│   └── model        PaymentOrder · PaymentDetails
├── credit
│   ├── api          CarbonCreditController · CreditIssuanceController · dto/
│   ├── service      CreditIssuanceService · SerialNumberService · RetirementService · certificate/
│   ├── repository   CarbonCreditRepository · CreditBatchRepository · CreditCertificateRepository · CreditSerialCounterRepository
│   └── model        CarbonCredit · CreditBatch · CreditCertificate · CreditSerialCounter
├── marketplace
│   ├── api          MarketplaceController · OrderController · dto/
│   ├── service      ListingService · OrderCommandService · OrderSettlementService · PricingService
│   ├── repository   MarketplaceListingRepository · OrderRepository · OrderStatsRepository
│   └── model        MarketPlaceListing · Order
├── profitsharing
│   ├── api          ProfitSharingController · CompanyPayoutController · dto/
│   ├── service      ProfitSharingService · CompanyPayoutQueryService
│   ├── repository   ProfitDistributionRepository · ProfitDistributionDetailRepository
│   └── model        ProfitDistribution · ProfitDistributionDetail
├── emission
│   ├── api          EmissionReportController · ReportController · ReportAnalysisController · CvaDashboardController
│   ├── service      EmissionReportService · ReportAnalysisService · rules/
│   ├── repository   EmissionReportRepository · EmissionReportDetailRepository
│   └── model        EmissionReport · EmissionReportDetail
├── ai
│   ├── api          ChatAiController · EmissionAiController
│   ├── service      AiScoringService · ChatService · ScoringPromptBuilder · DataQualityStatistics
│   ├── client       AiClient (port) → gemini/ · vertex/
│   └── (config)     ScoringProperties
├── project          (api/service/repository/model: Project, ProjectApplication)
├── vehicle          (api/service/repository/model: Vehicle, control endpoints)
├── notification     SseService · EmailService · NotificationController
└── filestorage      StorageService port → s3/ · local/
```

## Package Contracts (major packages)

### `shared`
- **Purpose**: cross-cutting API/error plumbing with zero business logic.
- **Responsibilities**: response envelope, error codes, pagination, current-user resolution.
- **Allowed deps**: none (except Spring/Java std).
- **Forbidden deps**: everything else in the app.
- **Public API**: everything (it is the kernel).
- **Internal**: none.

### `auth`
- **Purpose**: credential lifecycle.
- **Responsibilities**: register/login/OTP/refresh/OAuth2 handoff.
- **Allowed**: `user.repository` (read), `shared`, `notification.service`.
- **Forbidden**: JWT filter internals leaking outward; no repository of other features.
- **Public API**: `AuthService` + `/api/v1/auth/**`.

### `wallet`
- **Purpose**: single owner of balance mutations and ledger truth.
- **Responsibilities**: provisioning, locked transfers, verified deposits, summaries, withdrawal records.
- **Allowed**: `shared`, `payment.service` (read verified orders), `notification.service`.
- **Forbidden**: payment SDK types; `SecurityContextHolder`; direct `payment.repository`.
- **Public API**: `WalletService`, `DepositService`, `WalletTransactionService`.
- **Internal**: `WalletSummaryAssembler`, balance math helpers.

### `payment`
- **Purpose**: PaymentOrder lifecycle + gateway abstraction.
- **Responsibilities**: create order, receive return/webhook, verify signatures server-side, publish verified payments.
- **Allowed**: `shared`, `wallet.service.DepositService` (credit on verified payment).
- **Forbidden**: Stripe/PayPal/VNPay SDK types outside `gateway/*` subpackages; wallet repository access.
- **Public API**: `PaymentOrderService`, `PaymentGateway` port, webhook endpoints.
- **Internal**: adapter HTTP details, HMAC utils.

### `marketplace`
- **Purpose**: listings + orders + settlement orchestration.
- **Allowed**: `credit.service` (issuance), `wallet.service` (settlement debits/credits), `shared`.
- **Forbidden**: touching `Wallet`/`CarbonCredit` repositories directly (must go through services).
- **Public API**: `ListingService`, `OrderCommandService`, `OrderSettlementService`.

### `credit` / `profitsharing` / `emission` / `ai` / `project` / `vehicle` / `notification` / `filestorage`
Same pattern: **Purpose** = one domain; **Allowed** = `shared` + explicitly sanctioned services; **Forbidden** = other features' repositories/models and SDK leakage; **Public API** = the service interfaces currently consumed elsewhere; **Internal** = assemblers, calculators, adapters.

## Migration Notes

- Moves are mechanical (package/import changes only) — one feature per PR, Phase 4 of the roadmap.
- The two existing feature-ish packages (`service/analysis`, `service/credit`) move first as proof.
- `utils/Tuong` dissolves into `shared.api` (ADR-005); `common` splits into `shared` + domain enums beside their models.
