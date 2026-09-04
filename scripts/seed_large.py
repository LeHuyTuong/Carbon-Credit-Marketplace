#!/usr/bin/env python3
"""Seed script: tạo dữ liệu lớn cho baseline measurement.
Sử dụng CSV + LOAD DATA INFILE để load nhanh.
"""
import csv, random, sys, os
from datetime import datetime, timedelta

random.seed(42)

OUTDIR = "/tmp/carbonx_seed"
os.makedirs(OUTDIR, exist_ok=True)

# Constants — scale lớn theo yêu cầu
N_USERS        = 1_000
N_COMPANIES    = 1_000
N_ROLES        = 4
N_PROJECTS     = 5
N_REPORTS      = 1_000
N_BATCHES      = 4_000
N_CREDITS      = 200_000   # 50 credits/batch
N_LISTINGS     = 200_000   # 1:1 với credits; chỉ 2_000 AVAILABLE
N_ORDERS       = 100_000
N_PAYMENT_ORD  = 100_000
N_TXN          = 1_000_000  # 1k/txn, 1000 wallets → 1000/txn per wallet
N_WALLET       = 1_000
N_WITHDRAWAL   = 100

# BCrypt hash of "Password@1" (BCryptPasswordEncoder, 10 rounds, $2a$ prefix)
BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad.4cqxUUqWda.2qW"

# ---- roles.csv ----
with open(f"{OUTDIR}/roles.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_ROLES):
        names = ["ROLE_ADMIN", "ROLE_USER", "ROLE_COMPANY", "ROLE_CVA"]
        w.writerow([i+1, names[i], f"Role {names[i]}"])

# ---- users.csv ----
with open(f"{OUTDIR}/users.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_USERS):
        uid = i + 1
        if uid == 1:
            w.writerow([1, "testuser@example.com", BCRYPT_HASH, "ACTIVE", "", "", "", datetime.now(), datetime.now()])
        elif uid <= 3:
            emails = ["admin_seed@example.com", "company_seed@example.com", "cva_seed@example.com"]
            w.writerow([uid, emails[uid-2], BCRYPT_HASH, "ACTIVE", "", "", "", datetime.now(), datetime.now()])
        else:
            w.writerow([uid, f"bulk{i}@example.com", BCRYPT_HASH, "ACTIVE", "", "", "", datetime.now(), datetime.now()])

# ---- user_role.csv ----
with open(f"{OUTDIR}/user_role.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_USERS):
        w.writerow([i+1, 3])  # ROLE_COMPANY for all

# ---- company.csv ----
with open(f"{OUTDIR}/company.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_COMPANIES):
        w.writerow([i+1, i+1, f"C{i+1:06d}", f"Business License {i+1}", f"TAX{i+1:06d}", f"Address {i+1}", datetime.now(), datetime.now()])

# ---- wallets.csv ----
with open(f"{OUTDIR}/wallets.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_WALLET):
        w.writerow([i+1, i+1, i+1, None, 1000000.00, 0])  # balance=1M

# ---- project.csv ----
with open(f"{OUTDIR}/project.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_PROJECTS):
        w.writerow([i+1, f"PROJ-{i:04d}", f"Project {i+1}", "Carbon offset project", "ACTIVE", "1000", "Solar", "pdf", "logo.png", "0.5", "10.00", "5.00", "2.00", "2020-01-01", "2024-12-31", datetime.now(), datetime.now()])

# ---- emission_report.csv ----
with open(f"{OUTDIR}/emission_report.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_REPORTS):
        w.writerow([i+1, (i % N_PROJECTS) + 1, (i % N_COMPANIES) + 1, "2024-Q1", "EV", "APPROVED", "1000.0000", "5000.0000", "100.000", "10", f"file{i}.csv", f"https://s3/url/report{i}.csv", f"sha256_{i}", None, None, None, "2024-06-01 00:00:00", "2024-06-15 00:00:00", "2024-06-20 00:00:00", datetime.now(), datetime.now()])

# ---- emission_report_details.csv ----
with open(f"{OUTDIR}/emission_report_details.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_REPORTS):
        w.writerow([i+1, (i % N_PROJECTS) + 1, (i % N_COMPANIES) + 1, f"PLATE-{i:06d}", "1000.0000", "500.0000", "2024-Q1"])

# ---- credit_batches.csv ----
with open(f"{OUTDIR}/credit_batches.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_BATCHES):
        rep_id = (i % N_REPORTS) + 1
        w.writerow([i+1, f"BATCH-{i:06d}", (i % N_PROJECTS)+1, (i % N_COMPANIES)+1, rep_id, 2025, "5000.000", "100.000", 50, f"PRE-{i:04d}-", i*1000, i*1000+999, "ISSUED", "SYSTEM", datetime.now(), "2025-12-31", datetime.now()])

# ---- carbon_credits.csv ----
with open(f"{OUTDIR}/carbon_credits.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(N_CREDITS):
        batch_id = (i // 50) + 1
        company_id = ((batch_id - 1) % N_COMPANIES) + 1
        w.writerow([i+1, f"CR-{i:08d}", "Carbon Credit", "AVAILABLE", 2025, "1.0000", "1.0000", 1, "10.00", None, None, (i % N_PROJECTS)+1, company_id, batch_id, None, "SYSTEM", "2024-06-01 00:00:00", "2025-12-31", datetime.now(), datetime.now()])

# ---- marketplace_listings.csv ----
with open(f"{OUTDIR}/marketplace_listings.csv", "w", newline="") as f:
    w = csv.writer(f)
    now = datetime.now()
    for i in range(N_LISTINGS):
        credit_id = i+1
        # Lấy company_id từ carbon_credit (company_id = ((batch_id-1) % N_COMPANIES) + 1 = ((i//50 % N_COMPANIES)) + 1)
        batch_id = (i // 50) + 1
        seller_company_id = ((batch_id - 1) % N_COMPANIES) + 1

        # 2,000 AVAILABLE (first 2000), rest is SOLD
        if i < 2_000:
            status = "AVAILABLE"
            expires = now + timedelta(days=30)
        else:
            status = "SOLD"
            expires = now - timedelta(days=1)

        w.writerow([i+1, f"uuid-{i:08d}", seller_company_id, credit_id, 1, 1, 0, "10.00",
                     status, expires, now])

# ---- orders.csv ----
with open(f"{OUTDIR}/orders.csv", "w", newline="") as f:
    w = csv.writer(f)
    now = datetime.now()
    for i in range(N_ORDERS):
        buyer_company_id = (i % N_COMPANIES) + 1
        seller_company_id = ((i + N_COMPANIES//2) % N_COMPANIES) + 1
        credit_id = (i + N_COMPANIES//2) % N_LISTINGS + 1
        w.writerow([i+1, credit_id, buyer_company_id, credit_id, "BUY", "SUCCESS", 1, "10.00", "10.00", "0.50", "9.50", now, now])

# ---- payment_order.csv ----
with open(f"{OUTDIR}/payment_order.csv", "w", newline="") as f:
    w = csv.writer(f)
    now = datetime.now()
    for i in range(N_PAYMENT_ORD):
        w.writerow([i+1, 1, "1000000.00", "BANK_TRANSFER", 1, f"TXNREF-{i:08d}", now, now])

# ---- wallet_transaction.csv (1M rows) ----
# Distribute across 1000 wallets, ~1000 txn per wallet
with open(f"{OUTDIR}/wallet_transaction.csv", "w", newline="") as f:
    w = csv.writer(f)
    now = datetime.now()
    txn_types = ["DEPOSIT", "WITHDRAWAL", "BUY", "SELL", "FEE", "PAYOUT"]
    count = 0
    for wallet_id in range(1, N_WALLET + 1):
        for j in range(1000):
            amount = round(random.uniform(100, 50000), 2)
            balance = 1000000.00 - (j * amount / 100)  # approximate running balance
            w.writerow([count + 1, wallet_id, random.choice(txn_types), amount,
                        balance, balance + amount, f"Transaction {count+1}",
                        random.randint(1, N_ORDERS) if random.random() < 0.5 else None,
                        random.randint(1, N_PAYMENT_ORD) if random.random() < 0.3 else None,
                        None, None, now + timedelta(seconds=j)])
            count += 1

# ---- payment_detail.csv (for withdrawals) ----
with open(f"{OUTDIR}/payment_detail.csv", "w", newline="") as f:
    w = csv.writer(f)
    for i in range(100):
        w.writerow([i+1, 1, "BANK", "Bank Name", "12345678", "9876543210", "Test User",
                     "testuser@example.com", datetime.now(), datetime.now()])

# ---- withdrawal.csv ----
with open(f"{OUTDIR}/withdrawal.csv", "w", newline="") as f:
    w = csv.writer(f)
    now = datetime.now()
    for i in range(100):
        w.writerow([i+1, 1, 1, "100.00", 1, now, None])

print(f"Total rows generated:")
print(f"  roles: {N_ROLES}")
print(f"  users: {N_USERS}")
print(f"  user_role: {N_USERS}")
print(f"  company: {N_COMPANIES}")
print(f"  wallets: {N_WALLET}")
print(f"  projects: {N_PROJECTS}")
print(f"  emission_reports: {N_REPORTS}")
print(f"  emission_report_details: {N_REPORTS}")
print(f"  credit_batches: {N_BATCHES}")
print(f"  carbon_credits: {N_CREDITS}")
print(f"  marketplace_listings: {N_LISTINGS}")
print(f"  orders: {N_ORDERS}")
print(f"  payment_order: {N_PAYMENT_ORD}")
print(f"  wallet_transaction: {N_TXN}")
print(f"  payment_detail: {100}")
print(f"  withdrawal: {100}")
print(f"Files written to {OUTDIR}/")
