# Plan — Khởi tạo Frontend v2 (CarbonX)

## Understanding
Dựng mới hoàn toàn `frontend-v2` từ con số 0 bằng React 19 + TypeScript + Vite 6 + Tailwind v4 + TanStack Query v5 + Axios + Zustand + React Router v7 + RHF + Zod + Recharts + Sonner. Không import/sửa backend hay `frontend/` cũ; chỉ đọc hợp đồng API của backend để khớp shape dữ liệu.

## Bản chốt màn
Mục tiêu: tra cứu / quản trị + phễu đăng ký (Marketplace)
Vai chính: COMPANY (doanh nghiệp mua tín chỉ)
Việc 30 giây: xem danh sách tin đăng, lọc theo dự án/giá rồi mở chi tiết
Số lần bấm: 3 ở 375px (mở menu → Marketplace → chọn tin)
Kiểu màn: Master/tra cứu + Dashboard
Vào từ đâu: `/` → Navbar "Marketplace"; `/app/company/*` → Sidebar
Ghi bằng: 2 request (`GET /marketplace`, `GET /projects`)

## Existing Code
Thư mục đích trống. Backend (read-only) có: `GET /api/v1/marketplace`, `/api/v1/projects`, `/api/v1/wallet`, `/api/v1/paymentDetails`, `/api/v1/withdrawal`, `/api/v1/companies/payouts/{id}/export.xlsx`, SSE `GET /api/v1/notifications` (event `notification`). Envelope: `responseData` (auth) và `response` (các endpoint khác). Role backend `CC_BUYER` tương ứng `COMPANY` ở frontend.

## Requirements
- Functional: cấu hình build; shared layer (store, api, sse, guard, components); layouts + router lazy + providers; 9 màn hình (login/register/home/marketplace/4 dashboard/wallet/payouts).
- Non-functional: 0 lỗi TypeScript, build succeed; token chỉ lưu trong Zustand persist; 401/429 có toast; inline field error; responsive 375px; không sourcemap production; proxy `/api` → `localhost:8082`.

## Assumptions
- Không có server backend khi chạy gate → UI render empty state có chủ đích, không crash.
- Dữ liệu API có thể thiếu field → mapping phòng thủ với fallback ở shared/api/mappers.
- `VITE_API_BASE` mặc định `/api/v1`; SSE dùng cùng base.

## Plan
1. [đọc] khảo sát backend read-only để chốt envelope, endpoint, enum trạng thái (đã xong).
2. [ghi] viết bộ kiểm thử biên chấp nhận: chạy `npx tsc --noEmit` và `npm run build` phải exit 0; kiểm tra cấu trúc file bằng `find`.
3. [ghi] tạo cấu hình: package.json, tsconfig(.node).json, vite.config.ts, index.html, .env.example, src/index.css, src/vite-env.d.ts.
4. [ghi] tạo shared/: auth.store.ts, api/client.ts, api/sse.ts, api/types.ts, guards/RoleGuard.tsx, components/ (6 file), lib/(utils, format).
5. [ghi] tạo app/: providers.tsx, router.tsx, layouts/PublicLayout + DashboardLayout.
6. [ghi] tạo features/auth (LoginPage, RegisterPage) và pages/ (Home, Marketplace, 4 Dashboard, Wallet, Payouts) + components con theo feature.
7. [ghi] chạy `npm install`, `npx tsc --noEmit`, `npm run build`; sửa lỗi TypeSoduct/build đến khi xanh (không dùng @ts-ignore/any trốn).
8. [đọc] tự review checklist 12 mục + kiểm diff không có TODO/console.log/softening; báo cáo kết quả kèm output lệnh.

## Files / Modules Affected
~45 file TypeScript/TSX mới, 5 file cấu hình, 0 file backend/frontend cũ bị sửa, 0 lần push/CI, 0 bản ghi DB.

## Program Design
- Ranh giới: `shared/` không import `features/` hay `pages/`; `app/` compose shared + features; mỗi feature sở hữu page + component con + hook query riêng.
- Types/DTO: `ApiEnvelope<T> = { response?: T; responseData?: T; ... }`; `AuthUser { id, email, fullName, role }`; `Role = 'EV_OWNER'|'COMPANY'|'CVA'|'ADMIN'`; `MarketplaceListing { listingId, quantity, availableQuantity, pricePerCredit, sellerCompanyName, projectTitle, expiresAt, ... }`; `Wallet { id, balance, carbonCreditBalance }`; `Notification { id, title, message, createdAt }`.
- Signatures: `apiClient.get/post/put/patch/delete<T>(url, config) => Promise<T>` unwrap envelope; `useNotificationsSse(): { notifications, unreadCount, clear }`; `RoleGuard({ allow, children })`.
- Call graph: `Page → useXxxQuery → apiClient → response.data.response|responseData → mapper → component`. `LoginPage.submit → useAuth.setAuth → navigate(roleHome)`.
- Status badge maps: CreditStatus, EmissionStatus, OrderStatus, ApplicationStatus, WithdrawalStatus → màu emerald/amber/rose/slate.

## Risks
- API chưa chạy: mọi page phải có loading/empty/error state, không dùng `?? []` che lỗi đọc (hiện rõ "Không tải được dữ liệu" + nút thử lại).
- Token hết hạn: interceptor 401 xoá auth + toast một lần, không redirect loop.
- 429: toast "Quá nhiều yêu cầu" + không retry vô hạn.
- Role frontend COMPANY vs backend CC_BUYER: normalize khi đọc claims.

## Test Seams
- `apiClient` interceptor: bắt 401/429 khác nhau; bỏ sót: network error chi tiết từ server.
- `RoleGuard`: bắt sai role bị chặn; bỏ sót: authorization thật phải do server (frontend chỉ UX).
- `authStore`: bắt persist/logout đúng field; bỏ sót: token bị đọc bởi script độc hại (không giải quyết được ở localStorage).
- Build/type gate: bắt lỗi compile; bỏ sót: lỗi runtime khi backend không chạy.

## Acceptance Criteria
- `npm install` exit 0; `npx tsc --noEmit` exit 0; `npm run build` exit 0 với `sourcemap: false` (không có file `.map` trong `dist/assets`).
- 9 màn hình tồn tại tại đúng path; router dùng `React.lazy` + `Suspense` cho mọi page.
- Có đúng 1 Axios instance (`src/shared/api/client.ts`) với Bearer + X-Trace-Id + xử lý 401/429.
- Login/Register hiển thị lỗi Zod ngay dưới field tương ứng, không làm mất giá trị đã nhập.
- RoleGuard chỉ cho phép role nằm trong `allow`; DashboardLayout dùng chung cho 4 role.
- 375px: navbar/sidebar có mobile drawer, không overflow ngang ở các page chính.
