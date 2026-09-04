# 21 — Money Invariants Audit

> Invariants inferred from code behavior, not invented. "Enforced" = where the code guarantees it; "Violated" = concrete counterexample found.

## Invariant Table

| # | Invariant | Where enforced | Where violated | Test coverage | DB enforcement | Recovery strategy |
|---|---|---|---|---|---|---|
| I1 | Wallet balance ≥ 0 | `WalletTransactionServiceImpl:94-96` (post-check before write); `transferFunds:343-347` (locked pre-check) | Withdrawal refund replay *adds* money (no upper invariant but creates unbacked funds, N3); deposit race can double-credit | none | no CHECK constraint | none |
| I2 | Every balance mutation produces a ledger record | `createTransaction` (all its callers); `transferFunds` writes 2 rows | **Withdrawal rejection refund** — direct `setBalance` + save, no `WalletTransaction` (`WithdrawalServiceImpl:125-130`) | none | n/a | none |
| I3 | A verified payment credits exactly once | status PENDING check in `processPaymentOrder` | Status flip and credit in **separate transactions** → concurrent double-credit; partial-failure leaves SUCCEEDED w/o credit (`20`) | none | no unique constraint on (payment_order_id) in ledger | none |
| I4 | Payment credited only to its owner | — | `WalletController:95` credits caller's wallet with any order's amount | none | n/a | none |
| I5 | An order settles at most once | `OrderServiceImpl:185-188` status check | Check happens **before** the listing lock; order row never locked → two concurrent completes both settle (N6) | none | no status-guarded UPDATE | none |
| I6 | A listing cannot sell more than its quantity | Lock + re-check under lock (`OrderServiceImpl:191-209`) | — (correct) | none (mocked in tests? no) | no constraint; app-level only | none |
| I7 | Buyer debit = seller credit + platform fee | fee stored at creation (`OrderServiceImpl:100-112`) | **Fee never deducted**: buyer debit = seller credit = totalPrice (`processFinancialTransactions:328-361`); profit sharing pays owners separately from company wallet at market price × pct (`ProfitSharingServiceImpl:139-146`). Whether the fee is an unimplemented feature or intentionally waived: **UNKNOWN — business decision required** | none | n/a | n/a |
| I8 | Carbon credit quantity ≥ 0 | clamping `.max(ZERO)` on seller wallet credit balance (`OrderServiceImpl:290`) | Clamping **masks** negative states instead of failing — a symptom-hider, and source credit amounts clamped to 0 similarly (`:235-237`) | none | no CHECK | none |
| I9 | Ledger rows reflect actual wallet mutations (balanceBefore/After) | debit side of `transferFunds`; all `createTransaction` rows | **Credit side of `transferFunds`** records the sender's before/after on the receiver's row (`WalletServiceImpl:379-380`) — every profit-sharing credit row is wrong | none | n/a | data-repair script needed |
| I10 | Credit issuance quantity = purchased quantity | `issueTradeCredit` loops `intValueExact()` × 1-unit credits | Fractional purchases throw mid-settlement (fail-loud, audit correction C1); wallet carbon balance credited `intValue()` of quantity (`OrderServiceImpl:272`) — **consistent only because fractional never reaches here**; integral-only by accident of failure, not by validation | none | no constraint | none |
| I11 | Trade credits have unique serials | `SerialNumberService.allocate` per unit (`issueTradeCredit`) | UNKNOWN — atomicity of serial counter not verified (concurrent allocations from two settlements) | none | creditCode unique constraint in entity (`unique=true`) — DB will reject duplicates loudly | rely on DB unique → settlement fails visibly |
| I12 | Withdrawal pays out at most the requested (already-debited) amount | request-time balance check + immediate debit | approval path re-check commented out; SUCCEEDED withdrawal has **no payout ledger record** (manual bank transfer assumed, unauditable) | none | n/a | none |

## Derived Reconciliation Queries (operational safety net — to be added)

```sql
-- R1: ledger consistency per wallet
SELECT w.id FROM wallets w
JOIN (SELECT wallet_id, SUM(balance_after - balance_before) AS delta
      FROM wallet_transaction GROUP BY wallet_id) t ON t.wallet_id = w.id
WHERE w.balance <> t.delta;   -- non-empty = corruption (I2/I9 victims)

-- R2: orders settled more than once (post I5-fix guard)
SELECT marketplace_listing_id, COUNT(*) FROM orders
WHERE order_status='SUCCESS' GROUP BY marketplace_listing_id HAVING COUNT(*) > 1;

-- R3: payment orders SUCCEEDED without matching ADD_MONEY ledger row
SELECT po.id FROM payment_order po
WHERE po.status='SUCCEEDED'
  AND NOT EXISTS (SELECT 1 FROM wallet_transaction wt
                  WHERE wt.description LIKE CONCAT('%deposit%') /* replace with explicit payment_order FK after fix */);

-- R4: carbon credit inventory vs wallet carbon_credit_balance
-- (definition depends on intended semantics of chain credits; requires business input — 30 open question)
```
Note: R3 needs an explicit `wallet_transaction.payment_order_id` FK — currently the linkage is only via free-text description, itself an audit defect.

## Conclusion

Of 12 core money invariants, **4 are outright violated** (I2, I3, I4, I9), **2 are racy** (I5, concurrent I3), **1 is phantom by design ambiguity** (I7), and **none has test or DB enforcement**. The system's financial correctness rests on the happy path being the only path.
