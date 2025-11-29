
# Carbon Credit Marketplace (Spring Boot + React)

<p align="center">
  <img src="https://github.com/user-attachments/assets/3a24b37e-2896-4c90-8592-b267a3441daa" width="420"/>
</p>

## ⭐ Badges
<p align="center">
  <img src="https://img.shields.io/badge/Java-SpringBoot-green?style=for-the-badge&logo=springboot"/>
  <img src="https://img.shields.io/badge/React-18-blue?style=for-the-badge&logo=react"/>
  <img src="https://img.shields.io/badge/MySQL-8-orange?style=for-the-badge&logo=mysql"/>
  <img src="https://img.shields.io/badge/Redis-Cache-red?style=for-the-badge&logo=redis"/>
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker"/>
  <img src="https://img.shields.io/badge/Nginx-ReverseProxy-darkgreen?style=for-the-badge&logo=nginx"/>
</p>


---
# 🌿 Full Project Documentation (CarbonX Version)
## [Documentations](https://drive.google.com/drive/u/1/folders/1V0FyoZw_b9KMyj4aiCg9Z2sy7t4khyE-)
## 🌳 Project Overview
Carbon Credit Marketplace is a platform enabling Electric Vehicle (EV) owners, companies, and administrators to manage, trade, retire, and audit carbon credits.  
The system includes multi-role authentication, wallet system, marketplace listing, payment integrations (VNPay, Stripe, PayPal), CVA verification flow, and real‑time operations.

---
## 💻 Tech Stack

### 🧠 Backend – Java Spring Boot
- Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA / Hibernate
- MySQL
- Redis caching
- MapStruct
- Lombok
- AWS S3 Upload
- Google Auth / OAuth2
- Vertex AI (Gemini API for Casia AI chatbot)
- Docker & Docker‑Compose
- Nginx Reverse Proxy
- JUnit 5 + Mockito

### 🖥️ Frontend – React + Vite
- React 18
- Vite
- Material UI / React-Bootstrap
- React Router
- Nivo Charts / Chart.js
- Axios
- Zustand (optional)

### ☁️ External Integrations
- **VNPay Gateway**
- **Stripe**
- **PayPal**
- **AWS S3** (image & document storage)

---

## 📦 Core Features

### ✅ Authentication & Authorization
- Multi‑role: Admin, EV Owner, Company, CVA
- JWT Access / Refresh
- Google OAuth Login
- Email verification + OTP

### ✅ Wallet & Transactions
- Deposit (VNPay / Stripe / PayPal)
- Withdrawal
- Transaction history
- Admin approval flow

### ✅ Carbon Credits
- Issue credits from Credit Batches
- Status: AVAILABLE / TRADED / RETIRED
- Retire credits (Company)
- Profit‑Sharing (EV owners share % of listing sale)

### ✅ Marketplace
- Create Listing
- Update / Cancel listing
- Buy credits (Company)
- Search & filter listings

### ✅ CVA Verification Flow
- Company submits emission reports
- CVA verifies & approves
- Document validation
- Audit logs

### ✅ Admin Dashboard
- Credit counts, listing analytics
- User management
- Wallet & finance overview
- Project management

---

## 🛠️ Project Structure

```
backend/
 ├── Market_carbon/
 │    ├── common/
 │    ├── certificate/
 │    ├── controller/
 │    ├── mapper/
 │    ├── exception/
 │    ├── service/
 │    ├── repository/
 │    ├── dto/
 │    ├── config/
 │    ├── scheduler/
 │    ├── utils/
 │    └── model/
frontend/
 ├── src/
 │    ├── pages/
 │    ├── components/
 │    ├── hooks/
 │    ├── assets/
 │    ├── context/
 │    ├── styles/
 │    └── utils/
docker/
 ├── docker-compose.yml
 └── nginx/
```

---
---

## 🔑 Demo Accounts (For Testing)

To quickly explore the platform without creating new accounts, use the following demo users:
Link CVA : https://carbonx.io.vn/cva/carbonX/mkp/login 
Link ADMIN: https://carbonx.io.vn/admin/carbonX/mkp/login
Link CarbonX: https://carbonx.io.vn/login
| Role     | Email               | Password     | Notes                                  |
|----------|-------------------|------------|----------------------------------------|
| Admin    | admin1@gmail.com     | Tuong2005@  | Access full dashboard and user management |
| Company  | company@example.com   | Password@1| Can list, buy, and retire carbon credits |
| CVA      | cva@example.com       | Password@1    | Can verify company emissions and audit documents |

**How to use:**
1. Run the project locally or access the deployed demo.
2. Log in using the above credentials.
3. Explore the core features for each role:
   - **Admin**: Dashboard, user management, finance overview.
   - **Company**: Marketplace, listing creation, credit retirement.
   - **CVA**: Verification workflow, document approval, audit logs.

> ⚠️ Note: These demo accounts are reset periodically and are intended for testing only.

## 🚀 Quick Start (Development)

### 1️⃣ Clone Project
```bash
git clone https://github.com/LeHuyTuong/Carbon-Credit-Marketplace
cd Carbon-Credit-Marketplace
```

### 2️⃣ Backend Setup
```bash
cd backend/Market_carbon
cp .env.example .env
mvn clean install
mvn spring-boot:run
```

### 3️⃣ Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

### 4️⃣ Docker Deployment (Recommended on VPS)
```bash
docker compose up --build -d
```

---

## 🔧 Environment Variables

### Backend `.env`
```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=carbon_market
DB_USERNAME=root
DB_PASSWORD=yourpassword

JWT_SECRET=your_jwt_secret
AWS_ACCESS_KEY=...
AWS_SECRET_KEY=...
AWS_BUCKET_NAME=...
VNPAY_TMN_CODE=...
STRIPE_SECRET_KEY=...
PAYPAL_CLIENT_ID=...
PAYPAL_SECRET=...
```

### Frontend `.env`
```
VITE_API_BASE=https://yourdomain/api/v1
```

---

## 🔥 Screenshots 
![img_2.png](img_2.png)
---


# 👥 AQHighTeam

<div align="center">

| No | Student ID | Name | GitHub URL |
| --- |------------| --- | --- |
| 1 | SE196732  | Nguyễn Gia Khiêm | https://github.com/giakhiem20051710 |
| 2 | SE196853   | Lê Huy Tường | https://github.com/LeHuyTuong |
| 3 | SE196587  | Phan Bảo Tín | https://github.com/linh20051708 |
| 4 | SE193952  | Phạm Thị Diệu Linh | https://github.com/PhanBaoTin|

</div>

---

## ✅ License
This project is part of an academic submission. External contributions are not accepted.

---

## 🙌 Acknowledgements
Thanks to the instructors, teammates, and tools that supported the project.
