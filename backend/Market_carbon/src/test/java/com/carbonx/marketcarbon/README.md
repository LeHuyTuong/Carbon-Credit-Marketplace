# 🧪 Hướng dẫn Kiểm thử - Carbon Credit Marketplace

Dự án **Carbon Credit Marketplace** gồm hai phần chính:
- **Backend:** Java Spring Boot (quản lý logic nghiệp vụ, API, xác thực, v.v.)
- **Frontend:** React (giao diện web dành cho người dùng)

Tài liệu này hướng dẫn cách thiết lập môi trường, chạy kiểm thử, và xem báo cáo coverage cho cả hai phần.

---

## 1️⃣. Yêu cầu Môi trường

Trước khi bắt đầu, hãy đảm bảo đã cài đặt đủ các công cụ sau:

### 🔹 Backend (Java / Spring Boot)
- **Java JDK 17** *(hoặc phiên bản tương thích với Spring Boot)*
- **Apache Maven** *(hoặc sử dụng script `mvnw` có sẵn trong dự án)*

### 🔹 Frontend (React)
- **Node.js** ≥ v18.x (hoặc v20.x)
- **npm** hoặc **yarn**

---

## 2️⃣. Chạy Kiểm thử Backend (JUnit 5 + Mockito)

Tất cả **Unit Test** và **Integration Test** cho Controller, Service, Repository được viết bằng **JUnit 5** và **Mockito**.

### ▶️ 2.1. Chạy Toàn bộ Test Suite

Tại thư mục gốc của backend (nơi chứa `pom.xml` và `mvnw`):

**Windows (PowerShell / CMD):**
```bash
./mvnw.cmd test
```

**Linux/macOS:**
```bash
./mvnw test
```

> Lệnh này sẽ biên dịch mã nguồn, chạy toàn bộ test, và sinh báo cáo coverage bằng **JaCoCo**.

---

### 🎯 2.2. Chạy Một Class Test Cụ Thể

Ví dụ: chạy test cho `MarketplaceServiceImplTest` để debug nhanh:

**Windows:**
```bash
./mvnw.cmd test -Dtest=MarketplaceServiceImplTest
```

**Linux/macOS:**
```bash
./mvnw test -Dtest=MarketplaceServiceImplTest
```

---

### 📊 2.3. Xem Báo cáo Coverage (JaCoCo)

Sau khi test hoàn tất, JaCoCo sẽ sinh file HTML tại:
```
target/site/jacoco/index.html
```

Mở file này bằng trình duyệt để xem chi tiết coverage cho từng:
- **Package**
- **Class**
- **Phương thức**

> 🎯 Mục tiêu coverage: **≥ 80%**.

---

## 3️⃣. Chạy Kiểm thử Frontend (React Testing Library + Jest)

Các component và hook của React được test bằng **React Testing Library (RTL)** và **Jest**.

### 📦 3.1. Cài đặt Dependencies

Di chuyển đến thư mục frontend:
```bash
cd frontend
npm install
# hoặc
yarn install
```

---

### 🧩 3.2. Chạy Test và Coverage

Chạy tất cả test và sinh báo cáo coverage:

```bash
npm test -- --coverage
# hoặc
yarn test --coverage
```

Nếu muốn chạy test **một lần duy nhất** (không ở chế độ watch):
```bash
npm test -- --coverage --watchAll=false
```

---

### 📈 3.3. Xem Báo cáo Coverage (Jest)

Sau khi test xong, mở file sau để xem báo cáo:
```
frontend/coverage/lcov-report/index.html
```

> Xem chi tiết coverage theo từng component, hook, và logic UI.

---

## 🧰 Gợi ý Nâng cao

- Dùng **IntelliJ IDEA / VSCode** để chạy test trực tiếp trong IDE.
- Tích hợp **CI/CD (GitHub Actions)** để tự động chạy test mỗi lần push code.
- Cấu hình plugin **SonarQube** hoặc **Jacoco Badge Generator** để hiển thị coverage trực tiếp trên README.

---

## 📄 Kết luận

Bộ kiểm thử giúp:
- Đảm bảo logic nghiệp vụ của Carbon Credit Marketplace hoạt động ổn định.
- Phát hiện sớm lỗi tích hợp giữa các module (Wallet, Marketplace, Credit Batch, Payment...).
- Đảm bảo UI React hoạt động đúng với API backend.

---

📌 **Tác giả:** CarbonX QA Team  
📅 **Phiên bản:** 1.0  
🧩 **Công nghệ:** Spring Boot • React • JUnit5 • Mockito • Jest • Jacoco  
