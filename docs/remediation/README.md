# Remediation Guide — Kế hoạch sửa lỗi sau đánh giá toàn hệ thống

> Ngày lập: sau audit toàn hệ thống (backend + frontend + hạ tầng + test + tài liệu).
> Phương pháp: mọi phát hiện đều có bằng chứng `file:dòng` đã xác minh trực tiếp trên code,
> không suy diễn từ README. Bộ tài liệu này **thực thi chi tiết** các phase P0-B/P1/P2 trong
> [`docs/architecture/29_REVISED_ROADMAP.md`](../architecture/29_REVISED_ROADMAP.md) — không mâu thuẫn, chỉ cụ thể hóa.

---

## 1. Bộ tài liệu gồm 5 file — đọc theo thứ tự này

| File | Nội dung | Dùng khi nào |
|---|---|---|
| [`01_SCOPE.md`](01_SCOPE.md) | **Phạm vi**: cái gì sửa, cái gì KHÔNG sửa, mức ưu tiên, effort, Definition of Done | Trước khi bắt đầu |
| [`02_PLAN_P0.md`](02_PLAN_P0.md) | **4 fix P0** — tiền/authz gãy + deploy gãy. Có code BEFORE/AFTER từng bước | Làm tuần đầu tiên |
| [`03_PLAN_P1.md`](03_PLAN_P1.md) | **8 fix P1** — đúng đắn dưới race & luồng tiền biên | Sau khi P0 xong |
| [`04_PLAN_P2_HARDENING.md`](04_PLAN_P2_HARDENING.md) | Hardening bảo mật + dọn nợ kỹ thuật + CI | Khi P0–P1 ổn định |
| [`05_LESSONS_LEARNED.md`](05_LESSONS_LEARNED.md) | **12 bài học** trực quan (diagram + quy tắc + dấu hiệu nhận biết) | Đọc song song lúc code |

## 2. Quy tắc làm việc (giữ đúng Rule F của team: *test trước, fix sau*)

1. Với mỗi `FIX-xx`: **viết test thất bại trước** → thấy nó đỏ vì đúng lý do → mới sửa code → xanh.
2. **1 FIX = 1 commit**, message theo convention hiện có:
   ```
   fix(order): FIX-P0-1 enforce ownership on order completion
   fix(wallet): FIX-P1-6 make withdrawal request+debit atomic
   ```
3. Không sửa "tiện tay" ngoài scope của FIX đang làm — ghi vào `04` nếu thấy thêm.
4. Sau P0 chạy query đối soát ở [§4](#4-query-đối-soát-dùng-sau-mỗi-phase) — phải trả về rỗng.

## 3. Bảng tiến độ tổng (tick dần khi làm)

### Phase 1 — P0 (tuần này)
- [ ] `FIX-P0-1` 🔴 Chặn complete đơn người khác (authz) — *02_PLAN_P0 §1*
- [ ] `FIX-P0-2` 🔴 Thu phí giao dịch 5%, bảo toàn tiền — *02_PLAN_P0 §2*
- [ ] `FIX-P0-3` 🔴 Sửa deploy FE: nginx `/api` proxy + `VITE_API_BASE` — *02_PLAN_P0 §3*
- [ ] `FIX-P0-4` 🔴 Logout xóa sạch token admin/CVA — *02_PLAN_P0 §4*

### Phase 2 — P1
- [ ] `FIX-P1-5` 🟠 Cấm settle đơn CANCELLED — *03_PLAN_P1 §1*
- [ ] `FIX-P1-6` 🟠 Rút tiền: tạo yêu cầu + ghi nợ trong 1 transaction — *03_PLAN_P1 §2*
- [ ] `FIX-P1-7` 🟠 VNPay callback: permitAll + bắt buộc xác minh HMAC — *03_PLAN_P1 §3*
- [ ] `FIX-P1-8` 🟠 `updateOrderStatus`: khóa dòng + máy trạng thái — *03_PLAN_P1 §4*
- [ ] `FIX-P1-9` 🟠 `carbon_credit_balance` qua ledger, bỏ `.max(ZERO)` — *03_PLAN_P1 §5*
- [ ] `FIX-P1-10` 🟠 Trạng thái phân phối lợi nhuận PARTIAL/FAILED — *03_PLAN_P1 §6*
- [ ] `FIX-P1-11` 🟠 Tỷ giá ra config, thống nhất rounding — *03_PLAN_P1 §7*
- [ ] `FIX-P1-12` 🟠 FE: RoleRoute + success-code exact match + remember=false — *03_PLAN_P1 §8*

### Phase 3 — P2 / Hardening
- [ ] `HARD-1..6` Vệ sinh bảo mật (console.log token, sourcemap, creds demo...) — *04 §1*
- [ ] `DEBT-1..8` Dead code + deps thừa + trùng lặp FE — *04 §2*
- [ ] `INFRA-1..3` CI, health check, cấu hình boot fresh-clone — *04 §3*

> ✅ Đã xong TRƯỚC bộ tài liệu này (do agent thực hiện): vệ sinh repo — xóa file rác
> do gõ nhầm lệnh shell, untrack ảnh/docx cá nhân khỏi git, bổ sung `.gitignore`.
> Xem commit `chore(repo)` trên nhánh này.

## 4. Query đối soát dùng sau mỗi phase

Chạy trên MySQL (`core_ccm`) — cả 3 phải trả về **rỗng**:

```sql
-- (a) Ví nào lệch với ledger (ai viết balance ngoài createTransaction sẽ lộ)
SELECT w.id, w.balance, t.balance_after
FROM wallets w
JOIN wallet_transactions t ON t.id = (
    SELECT id FROM wallet_transactions WHERE wallet_id = w.id
    ORDER BY created_at DESC LIMIT 1)
WHERE w.balance <> t.balance_after;

-- (b) Bảo toàn tiền: tổng debit + credit phải ≈ 0 (sau FIX-P0-2 phải vẫn vậy)
SELECT SUM(amount) AS drift FROM wallet_transactions;

-- (c) Nghiệp vụ credit: ví credit ≠ tổng phát hành − bán − retire
--     (bật dùng sau FIX-P1-9 khi credit đi qua ledger)
```

## 5. Liên quan

- Tự-audit gốc của team: [`docs/architecture/00_AUDIT_INDEX.md`](../architecture/00_AUDIT_INDEX.md)
- Rotation secrets (việc ngoài code): [`docs/architecture/P0_A_SECRET_ROTATION.md`](../architecture/P0_A_SECRET_ROTATION.md)
