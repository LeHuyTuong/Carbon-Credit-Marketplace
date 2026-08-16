# 20 — Payment State-Machine Audit

## Actual State Machines (as implemented)

### PaymentOrder (Stripe/PayPal path)
```text
[PENDING] --processPaymentOrder(true hardcoded)--> [SUCCEEDED]   (PaymentServiceImpl:113-140)
[PENDING] --(never, no failure path written)------> [FAILED]
re-enter processPaymentOrder on SUCCEEDED --> returns false, no re-credit (replay guard for the simple case)
```
- No webhook exists. Transition trigger = frontend calling `POST /wallet/deposit` after Stripe/PayPal redirect.
- **Verification step: absent** (`paymentConfirmed = true`, `paymentId` unused).
- **Ownership: absent** — `order.getUser()` never compared to caller.
- **Two-step partial failure**: status flip (tx A: `processPaymentOrder`) and wallet credit (tx B: `addBalanceToWallet` in controller, then `createTransaction` in tx C) are separate transactions. Crash between A and B ⇒ SUCCEEDED order with no credit; concurrent duplicate calls between A and B ⇒ both may pass "PENDING?" read before either commits ⇒ **double credit race** (read-then-act without lock across the pair).

### PaymentOrder (VNPay path)
```text
[PENDING] --orderReturn, HMAC OK, vnp_ResponseCode=00--> [SUCCEEDED]  (VNPayService:110-115)
[PENDING] --HMAC OK, code != 00-------------------------> [FAILED]
re-enter: updateOrderStatus only mutates if still PENDING (:247-250) — idempotent status-wise
```
- Signature: HMAC-SHA512 verified — good.
- Amount: covered by signature (vnp_Amount inside hash) — tamper-proof.
- **Gap: SUCCEEDED is terminal with no effect** — the VNPay path never credits the wallet at all. Users presumably fall back to calling `/wallet/deposit` manually (the unverified path).

## Mandated Questions — Answers

| Question | Answer | Evidence |
|---|---|---|
| Can the same payment be credited twice? | **Yes, via the concurrent race** (status check and credit in separate transactions, no lock spanning both); simple sequential replay is blocked by the PENDING check | `WalletController:89-95`; `PaymentServiceImpl:116-123` |
| Can a user credit a payment they do not own? | **Yes** — no ownership check; caller's wallet is credited with the order amount | `WalletController:95` |
| Can a payment be marked successful without provider evidence? | **Yes — that is the default path** | `PaymentServiceImpl:126` |
| Can a webhook arrive before the frontend return? | N/A — **no webhooks exist** | repo-wide grep |
| Can a webhook be replayed? | N/A (no webhooks); VNPay return replay is signature-valid but status-idempotent | `VNPayService:108-118` |
| Can two workers process the same payment concurrently? | Yes — `findByIdWithLock` serializes the status flip only; the credit step is outside | `PaymentServiceImpl:116` vs `WalletController:95` |
| Provider succeeds but our transaction fails? | Order stays PENDING forever (status flip happens first in the happy path) or SUCCEEDED-without-credit (if credit fails); **no retry/recovery** | flow trace |
| Local success + duplicate webhook? | N/A (no webhooks); duplicate return handled by status guard | `PaymentServiceImpl:244-251` |
| Reconciliation with provider? | **Absent** — no query of Stripe/PayPal/VNPay state after the fact, no report, no cron | grep: no reconcile code |

## Withdrawal State Machine (money-out)

```text
[PENDING] --admin process(true)-->  [SUCCEEDED]  (payout assumed manual/offline; no ledger record of payout)
[PENDING] --admin process(false)--> [REJECTED]   + balance refund WITHOUT ledger record, WITHOUT status guard → replayable mint (N3)
```
- Approval does not re-check balance (check commented out, `WithdrawalServiceImpl:96-99`).
- The debit at *request* time is a proper ledger transaction (controller-orchestrated); the refund at *rejection* is a bare `setBalance` — asymmetric and unaudited.

## Required Target State Machine (design for the fix)

```text
PENDING ──verify(provider evidence, owner-matched, amount-matched)──▶ CREDITING ──▶ SUCCEEDED
   │                                                                     (single DB transaction:
   ├── verify failed ──────────────────────────────▶ FAILED               lock order, flip status, credit wallet,
   └── timeout (scheduler) ────────────────────────▶ EXPIRED              write ledger row)
Webhooks: signature-verified, event-id dedup, idempotent by construction (status transition under lock).
Reconciliation job: daily compare provider settlements vs local SUCCEEDED orders; alert on drift.
```
Sources: Stripe webhook docs, PayPal webhook guide, idempotency articles (06 §F2).
