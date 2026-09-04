# 04 — PLAN P2: Hardening bảo mật + dọn nợ kỹ thuật + hạ tầng

> Làm khi P0–P1 ổn định. Mục nhóm: `HARD-*` (bảo mật vận hành), `DEBT-*` (dead code/deps),
> `INFRA-*` (CI/cấu hình). Ưu tiên thứ tự trong file.

---

## §1 · HARD — Vệ sinh bảo mật (mỗi mục ≤ 30 phút)

### HARD-1 · Xóa log rò rỉ token + tắt sourcemap prod
- `frontend/src/utils/apiFetch.js:104` — `console.log("Fetching:", url, config)` in cả header
  `Authorization: Bearer ...`. **Xóa** (hoặc giữ ở mức dev-only: `if (import.meta.env.DEV)`).
- Các console khác: `apiFetch.js:176`, `RoleRoute.jsx:8`, trang Order/Wallet.
- `frontend/vite.config.js:26` — `sourcemap: true` đóng gói source gốc lên production →
  đổi thành `sourcemap: false` (hoặc chỉ bật qua flag build riêng cho việc debug đã kiểm soát).
- Kiểm chứng: build xong, view-source không thấy chuỗi `//# sourceMappingURL`.

### HARD-2 · Đơn giản hóa chọn token trong apiFetch
Hiện trạng `apiFetch.js:14-58`: 4 nguồn (`auth`, `admin_token`, `cva_token`, legacy `token`),
nhánh CVA có thể ghi đè nhánh admin. Sau FIX-P0-4 có `tokenStore.js` rồi thì:
1. Login nào cũng lưu vào **một** key theo namespace (`tokenStore.setActive(role, jwt)`).
2. `apiFetch` đọc đúng 1 nguồn; xóa nhánh fallback legacy sau 1 release (đặt deprecation log).

### HARD-3 · Bật lại gate admin bị comment
`WalletController.java:140`: `@PreAuthorize("hasRole('ADMIN')")` bị comment cho
`GET /wallet/transactions/count`. Bật lại. Test: `P0aRouteExposureTest` thêm case này.

### HARD-4 · Ownership cho `GET /orders/{id}`
`OrderServiceImpl.java:135` trả order cho bất kỳ ai đăng nhập. Áp cùng pattern FIX-P0-1:
chỉ buyer/seller của đơn hoặc ADMIN.

### HARD-5 · Validation đầu vào tiền
- `PaymentOrderRequest.amount`: `@NotNull @DecimalMin(value = "10")` + `paymentMethod` `@NotBlank`
  — hiện nạp số âm/0 tạo được link thanh toán rác, và NPE tại `PaymentController.java:44`.
- `createOrderVNPay` để trống `paymentMethod` → NPE trong switch của
  `assertDepositVerifiable` (`PaymentServiceImpl.java:162`) — set giá trị ngay lúc tạo.

### HARD-6 · Demo credentials công khai
README đang publish `admin1@gmail.com / Tuong2005@` cho domain **live** `carbonx.io.vn`.
1. Đổi mật khẩu tất cả account demo trên server (không đợi code).
2. README: thay bảng credentials bằng chú thích "liên hệ team để nhận tài khoản demo".
3. Đi checklist [`docs/architecture/P0_A_SECRET_ROTATION.md`](../architecture/P0_A_SECRET_ROTATION.md)
   nếu chưa chạy hết (mật khẩu pattern `Tuong2005@` gợi ý password cá nhân tái sử dụng — đổi ở chỗ khác nữa nếu bạn dùng chung).

### HARD-7 · Ledger row nạp tiền thiếu tham chiếu
Cột `payment_order_id` tồn tại nhưng deposit không điền → đối soát phải đoán bằng description.
Điền khi gọi `createTransaction(ADD_MONEY)`, kèm test assert cột được set.

---

## §2 · DEBT — Dead code & phụ thuộc chết (dọn an toàn, không đổi behavior)

### Backend
| ID | Việc | Vị trí |
|---|---|---|
| DEBT-1 | Xóa nguyên file `CarbonCreditController.java` (toàn bộ bị comment) | `controller/CarbonCreditController.java` |
| DEBT-2 | Xóa `addBalanceToWallet` (endpoint "nạp tiền miễn phí" không caller — nguy hiểm nếu ai vô tình wire vào) | `WalletServiceImpl.java:91`, interface `WalletService` |
| DEBT-3 | Xóa 2 block comment lớn trong `WalletController` (:57-76, :155-170) | git history giữ rồi |
| DEBT-4 | Gộp 2 scheduler hết hạn credit trùng lịch 02:00 (`CreditExpiryScheduler` + `ExpiredCreditScheduler`) — giữ 1, xác định logic khác nhau trước | `scheduler/` |
| DEBT-5 | Sửa import sai `io.lettuce.core.dynamic.annotation.Param` → `org.springframework.data.repository.query.Param`; rà các file repository khác | `WalletRepository.java:6` |
| DEBT-6 | `getWeightedAveragePrice()` trả `Double` → đổi `BigDecimal` (float nuôi công thức giá/payout là nợ kỹ thuật nguy hiểm) | `MarketplaceListingRepository.java:84-88` |
| DEBT-7 | `PriceUpdateScheduler`: cron mỗi phút là giá trị "test" — đưa ra config `app.pricing.update-cron`, review deprecated `ROUND_HALF_UP` usage | `scheduler/PriceUpdateScheduler.java` |
| DEBT-8 | Tách PDF/S3 upload + gửi email RA KHỎI transaction DB ở issuance (IO chậm giữ lock) — làm khi chạm P1-9 | `CreditIssuanceServiceImpl` |

### Frontend
| ID | Việc | Ghi chú |
|---|---|---|
| DEBT-9 | Gộp 3 trang Login (~660 dòng ≈ 90% giống nhau) thành `<LoginPage variant="user|admin|cva">` | Làm tương tự ForgotPassword ×3, ChangePassword ×2 |
| DEBT-10 | Bỏ deps chết: `@reduxjs/toolkit react-redux mdb-react-ui-kit chart.js react-chartjs-2 chartjs-plugin-datalabels axios` | 0 imports (axios dùng đúng 1 file → chuyển sang apiFetch); `npm uninstall ...` |
| DEBT-11 | Quyết SSE: hoặc wire thật EventSource (polyfill hỗ trợ header Auth, nhớ `proxy_buffering off` từ FIX-P0-3) hoặc bỏ `event-source-polyfill` khỏi package.json | Hiện trạng: import ở `Navbar.jsx:6` nhưng không bao giờ instantiate |
| DEBT-12 | `apiCVA/dashboardCVA.js:14-45` fetch thủ công → chuyển qua `apiFetch` (đồng bộ trace-id + error shape) |
| DEBT-13 | Review bundle: chỉ dùng nivo → cân nhắc bỏ `@nivo/geo` nếu GeographyChart admin ít dùng |

---

## §3 · INFRA — CI + cấu hình sống còn

### INFRA-1 · CI tối thiểu (GitHub Actions)
```yaml
# .github/workflows/ci.yml
name: ci
on: [push, pull_request]
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "17", cache: maven }
      - run: cd backend/Market_carbon && ./mvnw -B verify   # unit + IT Testcontainers
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: npm, cache-dependency-path: frontend/package-lock.json }
      - run: cd frontend && npm ci && npm run build
```
PR đỏ = chặn merge. Đây là "lưới an toàn" Phase P1 của roadmap team.

### INFRA-2 · Fresh clone boot được
- `application-local.properties` hiện **untracked** → fresh clone thiếu placeholder
  (`AI_GEMINI_API_KEY`, `AWS_S3_BUCKET`, `STRIPE_API_KEY`... đều không default trong
  `application.properties`) có thể fail startup.
- Case đã xác nhận cụ thể: `trading_fee` chỉ định nghĩa trong file untracked này, trong khi
  `@Value("${trading_fee}")` không có default (`OrderServiceImpl.java:47`) → fresh clone chắc chắn
  chết lúc start. Đã xử lý trong FIX-P0-2 Bước 0 (`trading.fee-rate` có default).
- Chọn 1: (a) commit `application-local.properties.example` + README chỉ dẫn copy; hoặc
  (b) thêm default no-op cho mọi placeholder (`${AI_GEMINI_API_KEY:}` + enabled=false).
- Kiểm chứng: clone mới về, chỉ `cp .env.example .env`, `docker compose up --build` → backend UP.

### INFRA-3 · Health endpoint + graceful shutdown
- Bật `spring-boot-starter-actuator`, expose `health` only; compose healthcheck dùng nó thay vì
  ping TCP (nếu sau này thêm).
- `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s`
  — tránh cắt giữa transaction settle khi deploy lại.

---

## Checklist hoàn thành Phase 3

- [ ] Không còn console.log in token; sourcemap off
- [ ] `/wallet/transactions/count` chặn ADMIN; `/orders/{id}` ownership
- [ ] Credentials demo đã đổi + README gỡ bảng mật khẩu
- [ ] `npm uninstall` xong deps chết; FE build nhẹ đi đáng kể
- [ ] CI xanh trên PR; fresh clone compose-up chạy được end-to-end
