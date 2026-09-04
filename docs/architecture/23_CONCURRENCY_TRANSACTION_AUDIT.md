# 23 — Concurrency & Transaction Audit

## Flow-by-Flow Transaction Maps

### F1 — Order settlement (`OrderServiceImpl.completeOrder`)
```text
tx1 (jakarta @Transactional):
  read Order (findByIdWithDetails)           [NO LOCK]
  check status != SUCCESS                    [idempotency check OUTSIDE lock — RACE N6]
  lock Listing   (PESSIMISTIC_WRITE + fetch) [lock 1]
  lock Credit   (PESSIMISTIC_WRITE)          [lock 2]
  read buyer/seller Wallets (fetch join)     [NO LOCK — N1]
  mutate credit; issueTradeCredit (loop inserts + serial allocations)
  mutate wallet carbon balances (save)
  mutate listing (save); order → SUCCESS (save)
  processFinancialTransactions:
    createTransaction(buyer debit)  → re-reads wallet by id [NO LOCK], checks balance, setBalance, save, insert ledger
    createTransaction(seller credit)→ same [NO LOCK]
  catch(Exception) → order = ERROR; save     [same tx → rolled back, status lost — S2]
commit
Lock order: listing → credit → (wallets unlocked)
```
**Defects**: N6 double-settlement (status check before lock); wallet reads/debits unlocked (double-spend across concurrent settlements of the same buyer); ERROR write rolled back; ~2N serial inserts inside one tx (fine for correctness, latency grows with quantity).

### F2 — Wallet transfer (`WalletServiceImpl.transferFunds`)
```text
tx1: lock fromWallet (PESSIMISTIC_WRITE) [lock A, by id]
     lock toWallet   (PESSIMISTIC_WRITE) [lock B, by id]
     check funds; debit A; credit B; write 2 ledger rows (credit row has wrong before/after — SC-2)
```
**Defects**: no documented global lock order vs F1 (deadlock possibility if a flow ever locks wallets before listing/credit — currently F1 doesn't lock wallets, so no interleaving today; fragile by accident); ledger bug.

### F3 — Deposit (`WalletController.addMoneyToWallet` — controller-orchestrated)
```text
tx1: processPaymentOrder → findByIdWithLock(paymentOrder) [LOCK on payment order]
     PENDING? → SUCCEEDED (save)
tx2: (controller) getPaymentOrderById (no lock)
tx3: addBalanceToWallet → createTransaction (wallet read NO LOCK, balance check, setBalance, ledger insert)
```
**Defects**: verification absent (SC1); ownership absent; **tx1/tx3 not atomic** — crash or concurrent re-entry between them double-credits or orphans (20/I3).

### F4 — Withdrawal
```text
request: tx1: requestWithdrawal (balance check NO LOCK, insert Withdrawal PENDING)
         tx2: createTransaction WITHDRAWAL (controller; debit + ledger)   [two-phase, not atomic]
process(false) [ANY USER — N2]: tx: refund setBalance(+amount) NO LOCK, NO status check [REPLAY MINT — N3], NO ledger
process(true)  [ANY USER]: tx: status → SUCCEEDED; no payout record
```

### F5 — Profit sharing (`ProfitSharingServiceImpl.shareCompanyProfit`)
```text
@Async("profitSharingTaskExecutor") (DelegatingSecurityContext propagates auth — good)
  reads company wallet (NO LOCK)
  aggregate contributions (reads)
  getSelf().createDistributionEvent  [self-proxy for @Transactional on internal call]
  per owner: walletService.transferFunds (tx per owner: dual locks + ledger)   [F2]
  markDistributionCompleted
```
**Defects**: aggregate uses a **stale wallet read** for the total to distribute; per-owner F2 locks sequentially — if the company wallet's balance drops concurrently, a later transferFunds fails INSUFFICIENT_FUNDS mid-distribution → partial distribution, event left PROCESSING (no compensation/restart path found — UNKNOWN recovery intent); `getSelf()` proxy hack.

### F6 — Schedulers
`CreditExpiryScheduler`: single bulk UPDATE at 02:00 daily, `@Transactional` — fine. `UserCleanupScheduler`: not re-read this pass (UNKNOWN detail, low risk).

### F7 — Credit retirement (`MyCreditServiceImpl.retireCreditsFromBatch`)
Read earlier as read-check-write on credit rows without locks (spot evidence `:361+`); UNKNOWN full detail — retirement races with settlement sales theoretically possible. Marked for verification in fix phase.

## Cross-Cutting Findings

| # | Finding | Evidence | Severity |
|---|---|---|---|
| T1 | No global lock-order policy (listing/credit here; wallets by id there) | F1 vs F2 | Medium (fragile) |
| T2 | Read-then-act idempotency checks outside locks (order status, payment status) | F1/F3 | High |
| T3 | Wallet mutations on unlocked reads in 3 flows | F1/F3/F4 | High |
| T4 | Multi-transaction multi-step money flows without saga/compensation | F3/F4/F5 | High |
| T5 | External calls (Stripe/PayPal/Gemini) — none currently inside a DB tx except none found in settlement; link creation paths are non-transactional (acceptable); Gemini blocking calls on request threads | `PaymentServiceImpl:143-228`; `GeminiAiScoringService` | Medium |
| T6 | `@Async` crossing tx boundaries with no transactional outbox; email/SSE failures swallowed (`catch log.warn`) | `WithdrawalServiceImpl:104-110` | Medium |
| T7 | Self-invocation workaround instead of structure | `ProfitSharingServiceImpl:63-74` | Low-Medium |
| T8 | Mixed annotation semantics (16 jakarta / 7 Spring) on all of the above | grep | Medium |
| T9 | Connection pool: Hikari configured only in `application-local.properties` (max size etc.); **prod profile pool settings UNKNOWN** — settlement holds locks across serial issuance; pool exhaustion under load plausible | properties diff | Medium |

## Concurrency Test Requirements (Rule F)

Before changing any of F1–F5: Testcontainers MySQL test that runs (a) two threads completing the same order, (b) two threads settling different orders of the same buyer, (c) two threads calling deposit for the same payment order, (d) 10× replay of withdrawal rejection. Assert ledger/balance invariants (21) after each. These tests *will fail today* — that is the point: they become the acceptance tests for the P0-B fixes.
