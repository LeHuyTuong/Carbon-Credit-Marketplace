# 01 — SCOPE: Phạm vi remediation

> Mục tiêu: đưa hệ thống tới mức **an toàn khi xử tiền thật** và **deploy được bằng docker compose**
> mà KHÔNG tái cấu trúc kiến trúc. Mọi thứ ngoài mục tiêu đó → OUT.

---

## 1. Định nghĩa mức nghiêm trọng

| Mức | Ý nghĩa | Quyết định |
|---|---|---|
| 🔴 **P0** | Mất tiền, chuyển tiền trái phép, hoặc deploy gãy | Sửa ngay, chặn merge nếu chưa có |
| 🟠 **P1** | Sai lệch dữ liệu/tiền chỉ xuất hiện dưới race hoặc biên cụ thể | Sửa trong sprint |
| 🟡 **P2** | Chất lượng, dead code, hygiene — không sai logic hiện tại | Xếp hàng, làm dần |

## 2. IN-SCOPE — danh sách đầy đủ (map với plan)

### P0
| ID | Vấn đề | Vị trí (đã xác minh) | Effort |
|---|---|---|---|
| FIX-P0-1 | `POST /orders/{id}/complete` không kiểm tra quyền — user bất kỳ settle được đơn người khác, di chuyển tiền của nạn nhân | `OrderController.java:52` (không có `@PreAuthorize`), `OrderServiceImpl.java:187` | M |
| FIX-P0-2 | Phí 5% (`trading_fee`) lưu vào `platformFee` nhưng seller nhận **toàn bộ** `totalPrice` → 5% bốc hơi khỏi hệ thống, vi phạm bảo toàn tiền | `OrderServiceImpl.java:118-119`, `:377-386` | M |
| FIX-P0-3 | FE đọc `import.meta.env.VITE_API_BASE` nhưng compose truyền `REACT_APP_API_BASE_URL` + nginx không có proxy `/api` → ảnh Docker gọi API bằng `undefined` | `frontend/src/utils/apiFetch.js:7`, `docker-compose.yml:55`, `frontend/nginx.conf` | S |
| FIX-P0-4 | Logout chỉ xóa `"auth"`, JWT admin/CVA còn nằm lại cả 2 storage | `frontend/src/context/AuthContext.jsx:55-57` vs `apiAdmin/apiLogin.js:30-40`, `apiCVA/apiAuthor.js:20-29` | S |

### P1
| ID | Vấn đề | Vị trí | Effort |
|---|---|---|---|
| FIX-P1-5 | Chỉ chặn `SUCCESS`; đơn CANCELLED vẫn settle được (kết hợp P0-1 → cancel vô nghĩa) | `OrderServiceImpl.java:197-200` | S |
| FIX-P1-6 | Rút tiền: tạo PENDING (tx 1) rồi ghi nợ (tx 2 ở controller) → debit fail để lại PENDING không tiền; admin từ chối thì refund → **tạo tiền từ hư không** | `WithdrawalServiceImpl.java:55-84`, `WithdrawalController.java:51-71`, refund `WithdrawalServiceImpl.java:132-143` | M |
| FIX-P1-7 | VNPay return URL bị `.authenticated()` che → gateway redirect không JWT → 401, nạp VNPay không xác nhận được. Khi mở public PHẢI kèm verify HMAC | `AppConfig.java:80`, VNPay flow `PaymentServiceImpl` | M |
| FIX-P1-8 | `updateOrderStatus` đọc-ghi không khóa, không @Transactional → đua với `applyVnPayDeposit`, có thể đè SUCCEEDED→FAILED | `PaymentServiceImpl.java:350-358` | S |
| FIX-P1-9 | `carbon_credit_balance` đổi trực tiếp không khóa ví; seller decrement bị `.max(ZERO)` che drift; không qua ledger | `OrderServiceImpl.java:309-321`, issuance `CreditIssuanceServiceImpl.java:167,419` | L |
| FIX-P1-10 | Phân phối lợi nhuận đánh dấu COMPLETED kể cả khi có owner FAILED | `ProfitSharingServiceImpl.java:431-439` | S |
| FIX-P1-11 | Tỷ giá hardcode 26.000, rounding HALF_DOWN đi / khác chiều về → drift thầm lặng | `CurrencyConverter.java:8-17`, `PaymentServiceImpl.java:207` | S |
| FIX-P1-12 | FE: RoleRoute dùng biến sai (`hasPermission` tính đúng mà không dùng); success-check theo substring (`"12003"` chứa `"200"`); LoginAdmin luôn `remember=true` | `RoleRoute.jsx:11,17`, `apiFetch.js:165-173`, `LoginAdmin.jsx:85` | S |

### P2 / Hardening (tóm tắt — chi tiết ở 04)
- Bảo mật vận hành: console.log in Bearer token, sourcemap prod, demo credentials công khai trên README trỏ domain live, bật lại `@PreAuthorize` cho `GET /wallet/transactions/count`, ownership cho `GET /orders/{id}`, validation amount nạp tiền.
- Nợ kỹ thuật: `CarbonCreditController` dead file, `addBalanceToWallet` (nạp tiền miễn phí) vẫn public, 2 scheduler hết hạn trùng giờ, cron test mỗi phút, import sai `io.lettuce...Param`, `Double` cho giá bình quân, FE nhân bản ×3 trang login, deps chết (Redux/mdb/chart.js/axios), SSE chưa wire.
- Hạ tầng: CI chưa có, fresh clone thiếu `application-local.properties` có thể không boot, `ddl-auto=update` ở prod (chỉ ghi nhận, Flyway để OUT).

## 3. OUT-OF-SCOPE — cố tình KHÔNG làm trong đợt này

| Không làm | Lý do |
|---|---|
| Tái cấu trúc package-by-feature | `28_ARCHITECTURE_REASSESSMENT.md` đã quyết defer — tôn trọng ADR |
| Flyway migration thay `ddl-auto` | Rủi ro schema lớn, cần baseline dump live; làm sau khi P0–P1 ổn |
| Scale-out SSE qua Redis pub/sub | Single-instance là đủ cho quy mô hiện tại |
| Refactor `GeminiAiScoringService` (1.105 dòng) | Không nằm trên luồng tiền; ghi nhận ở DEBT |
| Refresh-token flow đầy đủ | Thiết kế cần quyết định (access TTL, revoke) — lên backlog riêng, không làm vội |
| History rewrite để xóa secrets khỏi git cũ | Nguy hiểm; đã có checklist rotation riêng |

## 4. Definition of Done (áp dụng cho mọi FIX)

1. Test mới **viết trước**, fail vì đúng lý do, pass sau fix.
2. Toàn bộ test cũ vẫn xanh (`./mvnw test` — 100 @Test hiện tại).
3. Không thay đổi shape API envelope (`ApiRequest/ApiResponse`) — FE đang phụ thuộc.
4. Sau mỗi phase: 3 query đối soát ở [README §4](README.md#4-query-đối-soát-dùng-sau-mỗi-phase) trả về rỗng.
5. Commit message引用 FIX-ID (xem README §2).

## 5. Ai làm gì

| Việc | Trạng thái |
|---|---|
| Audit toàn hệ thống, xác minh bằng chứng file:dòng | ✅ Agent đã xong |
| Viết scope + plan + lessons (bộ docs này) | ✅ Agent đã xong |
| Vệ sinh repo (file rác, media, .gitignore) | ✅ Agent đã xong (commit `chore(repo)`) |
| Code fix theo 02/03/04 | 👤 Bạn tự làm — docs viết đủ chi tiết để code theo từng bước |
