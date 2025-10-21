# Carbon Credit Marketplace for EV Owners

<img width="954" height="846" alt="image-20250921-154343 (1)" src="https://github.com/user-attachments/assets/3a24b37e-2896-4c90-8592-b267a3441daa" />

![Views](https://views.whatilearened.today/views/github/LeHuyTuong/Carbon-Credit-Marketplace.svg)

<img width="954" height="846" alt="Architecture Snapshot" src="https://github.com/user-attachments/assets/5fb63c3d-f918-460b-b7b1-0133024fa67a" />

A production‑adjacent, end‑to‑end platform that enables EV owners and green enterprises to issue, list, buy, and settle carbon credits. The stack is Java Spring Boot (API), React 18 (web), MySQL/Redis (data/caching), with optional AWS S3 for evidence storage and Stripe/PayPal/VNPAY for payments. Everything can be orchestrated locally via Docker Compose.

> **Business value:** monetize verified EV charging activity; enforce KYC/AML guardrails; enable a transparent primary/secondary market for carbon assets; provide a warm path from prototype to real‑world operations.

---

## Table of Contents
- [Core Capabilities](#core-capabilities)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Quickstart](#quickstart)
  - [Option A — Docker Compose](#option-a--docker-compose)
  - [Option B — Local Dev](#option-b--local-dev)
- [Configuration](#configuration)
  - [Backend `application.yml` (sample)](#backend-applicationyml-sample)
  - [Frontend env (sample)](#frontend-env-sample)
- [API Docs](#api-docs)
- [Folder Structure](#folder-structure)
- [Key Domain Models](#key-domain-models)
- [Key API Surfaces](#key-api-surfaces)
- [Payment Lifecycle](#payment-lifecycle)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Acknowledgements & License](#acknowledgements--license)

---

## Core Capabilities

**Identity & On‑boarding**
- JWT‑secured auth (login/logout/refresh), password hashing.
- KYC flows for **Company** and **EV Owner** (business license, tax code, legal name, address, documents). Trading is gated by KYC status.

**Wallet & Payments**
- Multi‑rail funding: **Stripe**, **PayPal**, **VNPAY**.
- Payment Orders with status machine (e.g., `CREATED → PENDING → SUCCEEDED/FAILED`).
- Wallet ledger with deposits, purchases, withdrawals; FX helpers (e.g., USD→VND) for unified reporting.

**Carbon Credits & Marketplace**
- Credit issuance tied to projects/companies and verified charging data.
- Partial listing support; track listed vs. unlisted quantities.
- Market Listings with order workflow (browse, buy, fulfill, settle). Statuses drive transparency (e.g., `PENDING`, `APPROVED`, `SOLD`).

**Operations & UX**
- Responsive React dashboard (Admin, Company, EV Owner viewpoints).
- Real‑time feedback with validations and toasts; charts for portfolio and volumes.
- Dockerized local environment for fast repeatable setup.

---

## Architecture

```mermaid
flowchart LR
  subgraph Web[Frontend]
    FE[React 18 / Vite App]
    FE -->|HTTPS REST (Axios)| API
  end

  subgraph Core[Backend]
    API[Spring Boot 3.x]
    API -->|JPA| DB[(MySQL 8)]
    API -->|Cache| RD[(Redis)]
    API -->|Object Storage| S3[(AWS S3)]
    API -->|Payments| PAY[Payment Integrations]
  end

  subgraph Providers
    PAY --> Stripe[Stripe]
    PAY --> PayPal[PayPal]
    PAY --> VNPAY[VNPAY]
  end

  API --> DOCS[Swagger / OpenAPI]
  RD --> DB
```

---

## Tech Stack

**Backend**:
<p>
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/>
  <img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white"/>
</p>


**Frontend**: 
<p>
  <img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=000"/>
  <img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white"/>
  <img src="https://img.shields.io/badge/React%20Router-CA4245?style=for-the-badge&logo=reactrouter&logoColor=white"/>
  <img src="https://img.shields.io/badge/Material%20UI-007FFF?style=for-the-badge&logo=mui&logoColor=white"/>
  <img src="https://img.shields.io/badge/Bootstrap-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white"/>
  <img src="https://img.shields.io/badge/Chart.js-FF6384?style=for-the-badge&logo=chartdotjs&logoColor=white"/>
</p>


**Data**: 
<p>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/>
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"/>
</p>


**Integrations**:
<p>
  <img src="https://img.shields.io/badge/Stripe-635BFF?style=for-the-badge&logo=stripe&logoColor=white"/>
  <img src="https://img.shields.io/badge/PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white"/>
  <img src="https://img.shields.io/badge/VNPAY-0099CC?style=for-the-badge&logoColor=white"/>
  <img src="https://img.shields.io/badge/AWS%20S3-569A31?style=for-the-badge&logo=amazonaws&logoColor=white"/>
  <img src="https://img.shields.io/badge/SMTP-FF8C00?style=for-the-badge&logoColor=white"/>
</p>



**Ops/Tooling**: 
<p>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker%20Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white"/>
  <img src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white"/>
  <img src="https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white"/>
  <img src="https://img.shields.io/badge/VS%20Code-007ACC?style=for-the-badge&logo=visualstudiocode&logoColor=white"/>
</p>

---

## Quickstart

### Option A — Docker Compose
```bash
# From repo root
docker compose up -d
```
- Spins up **MySQL** and the **Spring Boot API** with the configured environment.
- Frontend can run locally with Vite or be containerized separately.

### Option B — Local Dev
**Backend**
```bash
cd backend/Market_carbon
mvn spring-boot:run
# API → http://localhost:8082
```
**Frontend**
```bash
cd frontend
npm install
npm run dev
# Web → http://localhost:5173
```

> Tables are auto‑generated via JPA/Hibernate on first run (based on your config). Ensure DB connectivity before starting the API.

---

## Configuration

Set environment variables via `application.yml` (Spring profiles) and frontend `.env`.

### Backend `application.yml` (sample)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/core_ccm?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: 12345
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
  redis:
    host: localhost
    port: 6379

server:
  port: 8082

security:
  jwt:
    secret: ${JWT_SECRET:change-me}
    expiration: 86400000 # 1 day (ms)

payments:
  stripe:
    apiKey: ${STRIPE_API_KEY:}
  paypal:
    clientId: ${PAYPAL_CLIENT_ID:}
    clientSecret: ${PAYPAL_CLIENT_SECRET:}
  vnpay:
    tmnCode: ${VNPAY_TMN_CODE:}
    hashSecret: ${VNPAY_HASH_SECRET:}
    returnUrl: http://localhost:5173/payments/return

aws:
  s3:
    bucket: ${AWS_S3_BUCKET:}
    region: ${AWS_REGION:ap-southeast-1}
    accessKey: ${AWS_ACCESS_KEY_ID:}
    secretKey: ${AWS_SECRET_ACCESS_KEY:}

mail:
  host: smtp.example.com
  port: 587
  username: ${SMTP_USER:}
  password: ${SMTP_PASS:}
```

### Frontend env (sample)
Create `frontend/.env.local`:
```dotenv
VITE_API_BASE=http://localhost:8082
VITE_APP_NAME=Carbon Credit Marketplace
```

> **Ports**: API `8082`, Vite `5173`, MySQL `3307`, Redis `6379` (adjust per your Docker Compose).

---

## API Docs
- Swagger UI: `[http://localhost:8082/swagger-ui.html](https://carbonx.io.vn/swagger-ui/index.html#/)`

---

## Folder Structure
```
Carbon-Credit-Marketplace/
├─ docker-compose.yml              # Orchestrates MySQL and API
├─ backend/
│  └─ Market_carbon/
│     ├─ pom.xml                   # Spring/AWS/Payments deps
│     └─ src/
│        ├─ main/java/com/carbonx/marketcarbon/
│        │  ├─ controller/         # REST controllers
│        │  ├─ service/            # Services + impls
│        │  ├─ model/              # Entities (CarbonCredit, Company, Order, ...)
│        │  └─ config/             # Security/Swagger/etc.
│        └─ main/resources/
│           └─ application.yml     # Profiles & env
└─ frontend/
   ├─ package.json                 # React/MUI/Router/Charts
   └─ src/
      ├─ pages/                    # Dashboard, Wallet, KYC, ...
      ├─ components/               # Reusable UI
      └─ utils/                    # API helpers/validators
```

---

## Key API Surfaces
> Concrete paths may vary; see Swagger for source of truth.

**Auth & Users**
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/refresh`

**KYC**
- `POST /api/v1/kyc/company` — create/update company KYC
- `GET /api/v1/kyc/status`

**Wallet & Payments**
- `POST /api/v1/wallet/deposit` — create Payment Order (Stripe/PayPal/VNPAY)
- `GET  /api/v1/wallet/orders` — list user payment orders
- `POST /api/v1/withdrawal/{amount}` — request withdrawal
- `GET  /api/v1/wallet/transactions` — ledger/history

**Credits & Market**
- `POST /api/v1/credits` — issue credits (admin/CVA)
- `POST /api/v1/listings` — list credits for sale
- `GET  /api/v1/listings` — browse listings
- `POST /api/v1/orders` — create purchase order

**Example**
```bash
curl -X POST http://localhost:8082/api/v1/orders \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
        "listingId": 123,
        "quantity": 50
      }'
```

---

## Payment Lifecycle

---

## Troubleshooting

**Port 8082 already in use** (Windows)
```bat
netstat -ano | findstr :8082
# Note PID, then
Taskkill /PID <PID> /F
```

**MySQL connection issues**
- Verify container exposes `3307` (host) → `3306` (container) or adjust `spring.datasource.url`.
- Ensure `MYSQL_ROOT_PASSWORD` and DB name match the app config.

**Insufficient wallet balance**
- Confirm deposit succeeded and wallet ledger reflects `SUCCEEDED` Payment Order.
- Check business rules on min withdrawal and fee schedule.

**Docker reset**
```bash
docker compose down -v  # remove volumes (DESTROYS DB DATA)
docker compose up -d
```

---

## Roadmap
- **Tokenization**: represent credits as NFTs on a permissioned chain to prevent double‑spend.
- **Mobile App**: React Native companion for iOS/Android.
- **Analytics**: price forecasting, cohort dashboards, anomalies.
- **Exchange Bridges**: connectors to external carbon markets for liquidity.
- **i18n & Multi‑currency**: beyond USD/VND; localized UX & pricing.

---

## Contributing
1. Fork → feature branch → PR. Keep diffs focused and well‑scoped.
2. Follow naming conventions and layering (Controller → Service → Repository → Model/DTO).
3. Use **Lombok** to reduce boilerplate; **MapStruct** for DTO mapping.
4. Add tests where meaningful (unit or slice tests). Avoid flaky e2e.
5. Never commit secrets. Use env variables or Vault‑style solutions.

---

## Acknowledgements & License
Built by the **Carbon X** team on top of open‑source ecosystems: Spring, React, MUI, Nivo and the Stripe/PayPal SDKs. Some UI components draw inspiration from public templates (e.g., Colorlib).

**License**: currently **undetermined**. The code is published for evaluation and learning. For commercial use or open‑sourcing derivatives, contact the repository owner to align on terms.

