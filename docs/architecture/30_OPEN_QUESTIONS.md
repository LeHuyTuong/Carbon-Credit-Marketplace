# 30 — Open Questions (UNKNOWNs requiring evidence or business decisions)

## Business-Intent Questions (owner: product/team)

| # | Question | Why it matters | Default assumption if unanswered |
|---|---|---|---|
| Q1 | Is `platform_fee` (`trading_fee=0.05`) supposed to be deducted? From whom? Where does it go? | Invariant I7 is phantom; any fee implementation changes settlement math | Not implemented on purpose → document "fee waived" and drop the column from UI math |
| Q2 | Should a COMPANY be able to buy its own listing of its *own* credits via a second company account? (self-buy guard exists only for same-company) | Market-manipulation vector | Keep current same-company check |
| Q3 | Are withdrawals paid out manually via bank transfer after SUCCEEDED? By whom, with what record? | No payout ledger exists (I12) | Manual process — add a recorded "payout executed" action |
| Q4 | Intended audience of KYC list endpoints and CVA dashboard — CVA only, or ADMIN too? | Gates in P0-A | CVA+ADMIN |
| Q5 | Is `/api/v1/reports/files/download` meant to serve public assets (logos) only, or user documents? | permitAll today | Public logos only — restrict path to assets |
| Q6 | VNPay deposits: after `orderReturn` marks SUCCEEDED, who was supposed to credit the wallet? (dead path today) | Defines the fix's webhook shape | Credit via verified return/webhook in P0-B |
| Q7 | Fractional credit quantities: business-meaningful or bug? | Validation policy (reject vs support) | Reject at creation (P0-B) |

## Technical UNKNOWNs (owner: verification tasks)

| # | Question | How to resolve |
|---|---|---|
| Q8 | Actual production DB schema (drift from entities? indexes present?) | `mysqldump --no-data` from prod → diff vs entities (P2 prerequisite) |
| Q9 | Do backups exist on the VPS? | Operator confirmation; else stand up 26 baseline |
| Q10 | Are the committed Stripe/PayPal keys **live** or test-mode? Determines severity of N5 exposure urgency | Check provider dashboards; rotate regardless |
| Q11 | `SerialNumberService` allocation atomicity under concurrent settlements (I11) | Read service + concurrency test |
| Q12 | `users.email` unique constraint in live DB? | Q8 dump; add unique in P2 if absent |
| Q13 | ReportAnalysis `/analyze`, ProjectApplication decisions, Vehicle ownership checks — authz verified? | Extend 19 matrix (read services) during P0-A |
| Q14 | WebSocket starter usage (SSE is the visible push mechanism) — dead dependency? | grep client usage; remove if dead (P4) |
| Q15 | Profit-distribution stuck in PROCESSING after partial failure — existing production rows affected? | Query prod (read-only) for PROCESSING distributions |
| Q16 | Hikari/other pool settings in the deployed profile (only local is tuned) | Inspect deployed env; document |
| Q17 | Known-vulnerability status of pinned versions | Enable Dependabot (24-A) — zero-risk action, do immediately |

## Explicit Non-Goals (confirmed by the audits)

- Microservices / multi-module Maven (28).
- Package-by-feature migration before triggers fire (28).
- Rewrites of cohesive services (27: EmissionReport, Kyc, CreditIssuance, CompanyPayoutQuery).
- OpenTelemetry/full APM at current scale (15).
