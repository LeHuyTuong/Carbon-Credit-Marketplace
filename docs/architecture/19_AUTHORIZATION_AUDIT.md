# 19 — Authorization Audit

> Method: full endpoint inventory of all 28 controllers (method + path + role annotation + service-side ownership check). "—" = none found. Confidence HIGH unless noted.

## Authorization Matrix (sensitive resources)

| Method & Endpoint | Controller | Role check (URL) | Role check (method) | Ownership check in service | IDOR / Escalation | Decision |
|---|---|---|---|---|---|---|
| POST /api/v1/auth/register | Auth | permitAll | — | n/a | **Role self-assignment (N1)** | FIX (whitelist) |
| POST /api/v1/payment | Payment | authenticated | — | order tied to currentUser | none (creation only) | OK |
| POST /api/v1/wallet/deposit | Wallet | authenticated | — | **none — order may belong to anyone** | **Cross-user deposit theft + unverified credit (SC1)** | FIX |
| GET /api/v1/wallet | Wallet | authenticated | — | currentUser wallet | OK | OK |
| GET /wallet/transactions/count | Wallet | authenticated | commented out (`WalletController:143`) | none | Info leak (total count) | FIX |
| POST /api/v1/orders | Order | authenticated | COMPANY | buyer = current company | OK | OK |
| POST /api/v1/orders/{id}/complete | Order | authenticated | COMPANY (`OrderController:32`) | **none — any company can settle any order (N7)** | Horizontal escalation | FIX |
| GET /api/v1/orders/{id} | Order | authenticated | COMPANY (`:88`) | **none (IDOR)** | Read of others' orders | FIX |
| DELETE /api/v1/orders/{id} | Order | authenticated | COMPANY | buyer check (`cancelOrder:165`) | OK | OK |
| POST /api/v1/withdrawal/{amount} | Withdrawal | authenticated | — | currentUser | OK (flow bugs in 21) | OK |
| PATCH /api/v1/withdrawal/admin/{id}/process/{accept} | Withdrawal | **no matcher** (`/api/admin/**` only) | **none** | none | **Any user approves withdrawals (N2)** | FIX |
| GET /api/v1/withdrawal/admin | Withdrawal | no matcher | none | none | All users' withdrawal data (N2b) | FIX |
| POST /api/v1/paymentDetails (+PUT/DELETE) | PaymentDetails | authenticated | — | user-scoped queries (`:73,84`) | OK | OK |
| POST /api/v1/marketplace (list) | Marketplace | authenticated | COMPANY (`:31`) | current company | OK | OK |
| GET /api/v1/marketplace | Marketplace | permitAll (PUBLIC_ENDPOINT) | — | public by design | OK (intended) | OK |
| DELETE /api/v1/marketplace/{listingId} | Marketplace | authenticated | COMPANY (`:103`) | seller check via service (`resolveOwnedCredit`) | OK | OK |
| POST /api/v1/profit-sharing/share | ProfitSharing | authenticated | COMPANY (`:31`) | report.seller == company (`ProfitSharingServiceImpl:172`) | OK | OK |
| GET /api/v1/companies/ev-owners, /payouts/* | CompanyPayout | authenticated | — | currentCompany scoping (`CompanyPayoutQueryServiceImpl`) | MEDIUM confidence OK; spot-verified `getDistributionSummary` is aggregate | Verify in fix phase |
| GET /api/v1/my/credits/** | MyCredit | authenticated | COMPANY (class-level `:37`) | currentCompanyId | OK | OK |
| POST /api/v1/my/credits/retire | MyCredit | authenticated | COMPANY | ownership via currentCompany (`retireCreditsFromBatch`) | OK | OK |
| POST /api/v1/credits/issue, /issued/{reportId} | CreditIssuance | authenticated | ADMIN (`:45,66`) | n/a | OK | OK |
| POST /api/v1/reports/upload | EmissionReport | authenticated | COMPANY (`:50`) | current company | OK | OK |
| PUT /api/v1/reports/{id}/verify, /approve | EmissionReport | authenticated | CVA / ADMIN (`:72,98,112,126,149`) | n/a | OK | OK |
| POST /api/v1/reports/{id}/ai-score | EmissionAi | authenticated | CVA/ADMIN (`:18`) | n/a | OK | OK |
| POST /api/v1/reports/{id}/analyze | ReportAnalysis | authenticated | — (UNKNOWN, not inspected) | UNKNOWN | UNKNOWN | Verify |
| GET /api/v1/reports/files/download | Report | **permitAll (PUBLIC_ENDPOINT)** | — | none | **Unauthenticated file access (scope of files UNKNOWN — comment says "Logo")** | FIX/verify |
| POST /api/v1/kyc/user, /company | Kyc | authenticated | — | currentUser profile | Self-KYC by design | OK |
| POST /api/v1/kyc/cva/create, /kyc/admin | Kyc | authenticated | **none** | existence-check only (`KycServiceImpl:235,320`) | **Any user creates Admin/CVA profiles (N8)** | FIX |
| GET /api/v1/kyc/listEvowner, /company/listKYCCompany | Kyc | authenticated | none | none | Cross-user KYC listing | FIX (role-gate to CVA/ADMIN) |
| GET /api/cva/dashboard/** | CvaDashboard | authenticated (via /api/**) | **none** | none | **Any role sees CVA aggregates (N9)** | FIX |
| POST /api/v1/projects, PUT/DELETE /{id} | Project | authenticated | ADMIN (`:32` area) | n/a | OK | OK |
| PUT /project-applications/{id}/cva-decision, /admin-decision | ProjectApplication | authenticated | UNKNOWN (not verified this pass) | UNKNOWN | UNKNOWN | Verify |
| POST /api/v1/vehicles, PUT/DELETE | Vehicle | authenticated | not verified in detail | currentCompany patterns seen | UNKNOWN | Verify |
| GET /api/v1/users, /users/{id}, /users/count, PATCH /users/{id}/status | User | authenticated | **ADMIN on all four** (`UserController:73,166,201,208`) | n/a | OK (corrects audit-1 note) | OK |
| GET /api/v1/users/by-email | User | authenticated | none | none | User enumeration/PII (N18) | FIX (admin-only or self) |
| POST /api/v1/ai/chat (+ /v1/ai/chat) | ChatAi | mixed (see N10) | COMPANY | n/a | Rate-limit bypass on /v1 route | FIX route |
| POST /api/test/upload1e | FileUploadTest | authenticated (/api/**) | none | none | Test endpoint in prod | DELETE |
| GET /api/v1/VNpayment/** | VNPayment | authenticated | — | order created for currentUser | OK | OK |

## Pattern Findings

1. **Vertical escalation paths (3 independent)**: registration role (N1), KYC admin/cva profile creation (N8), withdrawal admin endpoints (N2). Any one is game over; all three coexist.
2. **Horizontal (IDOR) gaps**: deposit crediting (SC1), order read + order completion (N7 + audit-1 #8). Ownership is enforced on *cancel* but not *read* or *complete* — inconsistent authorization across operations on the same resource.
3. **Trust boundary confusion**: `/api/admin/**` URL matcher is treated as "the admin area," but admin functions live at `/api/v1/...` in at least 4 controllers (withdrawal admin, users admin — properly annotated —, kyc admin — not annotated). URL-space and annotation-space authorization disagree.
4. **UNKNOWNs kept honest**: ReportAnalysis analyze, ProjectApplication decisions, Vehicle ownership, KYC list endpoints' intended audiences — marked UNKNOWN/verify; do not assume secure or insecure without reading them in the fix phase.

## Required Fixes (minimum set)

1. Registration: whitelist roles to `EV_OWNER`, `COMPANY`; CVA/ADMIN only via seeded/admin-created accounts.
2. Withdrawal admin endpoints: `@PreAuthorize("hasRole('ADMIN')")` + status-guarded, ledger-recorded refund (see 21).
3. Deposit: ownership check + verification (Phase P0-B).
4. Order complete/read: buyer ownership check.
5. KYC cva/admin create: role gate; KYC list endpoints: CVA/ADMIN gate.
6. CvaDashboard: role gate.
7. `/users/by-email`: restrict to ADMIN or authenticated-self lookup.
8. Remove `/v1/ai` duplicate mapping; keep `/api/v1/ai` under rate limit.
9. `/reports/files/download`: inventory served files; require auth or restrict to public assets.
10. ArchUnit-style regression test: every `**/admin/**` path must have an ADMIN annotation; every `/orders|/wallet` handler must reference ownership (manual list initially).
