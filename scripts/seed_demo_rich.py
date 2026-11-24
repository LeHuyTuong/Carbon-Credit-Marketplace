#!/usr/bin/env python3
"""Idempotent rich demo seeder for CarbonX local MySQL database (`test`).

Adds realistic Vietnamese EV owners, companies, vehicles, project applications,
emission reports + per-vehicle details, credit batches, marketplace listings,
orders, wallet ledger transactions, profit distributions, and withdrawals
without disturbing the 4 core demo login accounts (ids 1..4).
"""
import subprocess
import sys

DB = "test"
PW_HASH = "$2a$10$Ayu46.oRB4GIk6LCLHAaEeotoupawqSrTPTNiufMSbiIubhShEzw."

COMPANIES = [
    # (user_id, email, company_id, code, name, tax, biz_lic, address, wallet_id, balance, credit_bal)
    (101, "xanhsm@example.com", 10, "XSM", "Xanh SM Green Taxi JSC", "0109988771", "BL-XSM-2025", "Khu đô thị Vinhomes Riverside, Long Biên, Hà Nội", 101, 420000.00, 3200.0000),
    (102, "datbike@example.com", 11, "DBK", "Dat Bike Electric Mobility", "0316677882", "BL-DBK-2025", "45 Nguyễn Hữu Cảnh, Bình Thạnh, TP.HCM", 102, 185000.00, 1450.0000),
    (103, "selex@example.com", 12, "SLX", "Selex Motors Smart Battery", "0107766553", "BL-SLX-2025", "KCN Yên Phong, Bắc Ninh", 103, 142500.00, 980.0000),
    (104, "mailinh@example.com", 13, "MLG", "Mai Linh Green EV Fleet", "0301122334", "BL-MLG-2025", "64 Hai Bà Trưng, Quận 1, TP.HCM", 104, 310000.00, 2150.0000),
    (105, "evnsolar@example.com", 14, "EVN", "EVN Solar Charging Hub", "0100100079", "BL-EVN-2025", "11 Cửa Bắc, Ba Đình, Hà Nội", 105, 680000.00, 5400.0000),
]

EV_OWNERS = [
    # (user_id, email, name, phone, doc_num, gender, birth, address, company_id, wallet_id, balance)
    (201, "tuan.nguyen@example.com", "Nguyễn Minh Tuấn", "0901234501", "079094001001", "MALE", "1994-03-12", "Quận 7, TP. Hồ Chí Minh", 2, 201, 2450.00),
    (202, "ha.tran@example.com", "Trần Thu Hà", "0901234502", "001195002002", "FEMALE", "1995-07-21", "Cầu Giấy, Hà Nội", 2, 202, 1890.50),
    (203, "nam.pham@example.com", "Phạm Hoàng Nam", "0901234503", "048092003003", "MALE", "1992-11-05", "Hải Châu, Đà Nẵng", 2, 203, 3120.75),
    (204, "anh.le@example.com", "Lê Thị Ngọc Ánh", "0901234504", "079196004004", "FEMALE", "1996-01-19", "Thủ Đức, TP. Hồ Chí Minh", 2, 204, 1540.00),
    (205, "bao.vo@example.com", "Võ Quốc Bảo", "0901234505", "092091005005", "MALE", "1991-09-30", "Ninh Kiều, Cần Thơ", 2, 205, 2780.25),
    (206, "lananh.dang@example.com", "Đặng Thị Lan Anh", "0901234506", "031197006006", "FEMALE", "1997-04-14", "Ngô Quyền, Hải Phòng", 2, 206, 980.00),
    (207, "hung.bui@example.com", "Bùi Văn Hùng", "0901234507", "077090007007", "MALE", "1990-06-25", "TP. Vũng Tàu", 2, 207, 4210.00),
    (208, "khang.hoang@example.com", "Hoàng Minh Khang", "0901234508", "056093008008", "MALE", "1993-12-08", "Nha Trang, Khánh Hòa", 2, 208, 1675.50),
    (209, "thao.do@example.com", "Đỗ Phương Thảo", "0901234509", "046198009009", "FEMALE", "1998-02-17", "TP. Huế", 10, 209, 2100.00),
    (210, "ducanh.ngo@example.com", "Ngô Đức Anh", "0901234510", "001094010010", "MALE", "1994-08-22", "Thanh Xuân, Hà Nội", 10, 210, 1430.00),
    (211, "dat.vu@example.com", "Vũ Thành Đạt", "0901234511", "079095011011", "MALE", "1995-05-11", "Bình Thạnh, TP. Hồ Chí Minh", 11, 211, 890.00),
    (212, "ngan.phan@example.com", "Phan Thị Kim Ngân", "0901234512", "079193012012", "FEMALE", "1993-10-03", "Quận 3, TP. Hồ Chí Minh", 12, 212, 1760.00),
    (213, "hai.truong@example.com", "Trương Quang Hải", "0901234513", "048096013013", "MALE", "1996-07-29", "Sơn Trà, Đà Nẵng", 13, 213, 2540.00),
    (214, "mai.ly@example.com", "Lý Ngọc Mai", "0901234514", "079199014014", "FEMALE", "1999-11-15", "Phú Nhuận, TP. Hồ Chí Minh", 13, 214, 1320.00),
    (215, "phuc.duong@example.com", "Dương Văn Phúc", "0901234515", "068092015015", "MALE", "1992-04-04", "Đà Lạt, Lâm Đồng", 14, 215, 2990.00),
]

VEHICLES = [
    # (id, plate, brand, model, company_id, ev_owner_id)
    (101, "51K-901.12", "VinFast", "VF 8 Eco", 2, 201),
    (102, "51K-902.34", "VinFast", "VF 5 Plus", 2, 201),
    (103, "30K-711.45", "VinFast", "VF e34", 2, 202),
    (104, "43A-512.88", "VinFast", "VF 7 Plus", 2, 203),
    (105, "43A-519.02", "Hyundai", "Ioniq 5", 2, 203),
    (106, "51L-118.67", "VinFast", "VF 6", 2, 204),
    (107, "65A-334.91", "VinFast", "VF e34", 2, 205),
    (108, "15A-882.19", "VinFast", "VF 5 Plus", 2, 206),
    (109, "72A-661.55", "VinFast", "VF 9 Plus", 2, 207),
    (110, "72A-668.90", "BYD", "Atto 3", 2, 207),
    (111, "79A-445.21", "VinFast", "VF e34", 2, 208),
    (112, "75A-229.73", "VinFast", "VF 5 Plus", 10, 209),
    (113, "30E-884.10", "VinFast", "VF e34", 10, 210),
    (114, "30E-884.11", "VinFast", "VF 8 Plus", 10, 210),
    (115, "59M1-456.78", "Dat Bike", "Weaver++", 11, 211),
    (116, "59M1-456.79", "Dat Bike", "Quantum", 11, 211),
    (117, "59S2-109.44", "Selex", "Camel 2", 12, 212),
    (118, "43E-102.33", "VinFast", "VF e34", 13, 213),
    (119, "43E-102.34", "VinFast", "VF 5 Plus", 13, 213),
    (120, "51K-773.28", "VinFast", "VF 6 Plus", 13, 214),
    (121, "49A-551.09", "Hyundai", "Kona Electric", 14, 215),
    (122, "49A-551.10", "VinFast", "VF 7", 14, 215),
]


def esc(s: str) -> str:
    return s.replace("'", "''")


def build_sql() -> str:
    lines = [
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS = 0;",
    ]

    # 1. Companies (users, user_role, company, wallets)
    for uid, email, cid, code, name, tax, lic, addr, wid, bal, cbal in COMPANIES:
        lines.append(
            f"INSERT IGNORE INTO users (id, email, password_hash, status, wallet_id) "
            f"VALUES ({uid}, '{esc(email)}', '{PW_HASH}', 'ACTIVE', {wid});"
        )
        lines.append(f"INSERT IGNORE INTO user_role (user_id, role_id) VALUES ({uid}, 3);")
        lines.append(
            f"INSERT IGNORE INTO company (id, created_at, updated_at, address, business_license, code, company_name, tax_code, user_id) "
            f"VALUES ({cid}, NOW(), NOW(), '{esc(addr)}', '{esc(lic)}', '{esc(code)}', '{esc(name)}', '{esc(tax)}', {uid});"
        )
        lines.append(
            f"INSERT IGNORE INTO wallets (id, balance, carbon_credit_balance, company_id, user_id) "
            f"VALUES ({wid}, {bal:.2f}, {cbal:.4f}, {cid}, {uid});"
        )

    # 2. EV Owners (users, user_role, ev_owner, wallets)
    for uid, email, name, phone, doc, gender, birth, addr, cid, wid, bal in EV_OWNERS:
        lines.append(
            f"INSERT IGNORE INTO users (id, email, password_hash, status, wallet_id) "
            f"VALUES ({uid}, '{esc(email)}', '{PW_HASH}', 'ACTIVE', {wid});"
        )
        lines.append(f"INSERT IGNORE INTO user_role (user_id, role_id) VALUES ({uid}, 1);")
        lines.append(
            f"INSERT IGNORE INTO ev_owner (user_id, created_at, updated_at, address, birth_date, country, document_number, document_type, email, gender, name, phone, company_id) "
            f"VALUES ({uid}, NOW(), NOW(), '{esc(addr)}', '{birth}', 'Việt Nam', '{doc}', 'CCCD', '{esc(email)}', '{gender}', '{esc(name)}', '{phone}', {cid});"
        )
        lines.append(
            f"INSERT IGNORE INTO wallets (id, balance, carbon_credit_balance, company_id, user_id) "
            f"VALUES ({wid}, {bal:.2f}, 0.0000, NULL, {uid});"
        )

    # 3. Vehicles
    for vid, plate, brand, model, cid, owner_id in VEHICLES:
        lines.append(
            f"INSERT IGNORE INTO vehicles (id, created_at, updated_at, brand, document_url, model, plate_number, company_id, ev_owner_id) "
            f"VALUES ({vid}, DATE_SUB(NOW(), INTERVAL {vid - 90} DAY), NOW(), '{esc(brand)}', '/images/vinfast-vfe34.png', '{esc(model)}', '{esc(plate)}', {cid}, {owner_id});"
        )

    # 4. Project Applications (diverse workflow states for CVA & Admin queues)
    apps = [
        (10, "ADMIN_APPROVED", 10, 1, 1, 1, "Hồ sơ trạm sạc đủ tiêu chuẩn", "Đã duyệt tham gia"),
        (11, "ADMIN_APPROVED", 11, 2, 1, 1, "Đạt chuẩn đo lường VM0038", "Chấp thuận cấp 2"),
        (12, "CVA_APPROVED", 12, 3, 1, None, "Đã xác minh danh sách 450 trạm đổi pin", None),
        (13, "CVA_APPROVED", 13, 2, 1, None, "Đủ điều kiện đo đếm hành trình taxi điện", None),
        (14, "UNDER_REVIEW", 14, 4, None, None, None, None),
        (15, "SUBMITTED", 10, 6, None, None, None, None),
    ]
    for aid, st, cid, pid, rev_id, fin_id, rnote, fnote in apps:
        rev_sql = str(rev_id) if rev_id else "NULL"
        fin_sql = str(fin_id) if fin_id else "NULL"
        rn_sql = f"'{esc(rnote)}'" if rnote else "NULL"
        fn_sql = f"'{esc(fnote)}'" if fnote else "NULL"
        lines.append(
            f"INSERT IGNORE INTO project_application (id, created_at, updated_at, status, submitted_at, company_id, project_id, reviewer_id, final_reviewer_id, review_note, final_review_note) "
            f"VALUES ({aid}, DATE_SUB(NOW(), INTERVAL {25 - aid} DAY), NOW(), '{st}', DATE_SUB(NOW(), INTERVAL {25 - aid} DAY), {cid}, {pid}, {rev_sql}, {fin_sql}, {rn_sql}, {fn_sql});"
        )

    # 5. Emission Reports (5..10) + Emission Report Details for multi-owner payout preview
    reports = [
        (5, 1, 2, "CREDIT_ISSUED", "2026-09", 184200.0000, 124650.0000, 9),
        (6, 2, 10, "CREDIT_ISSUED", "2026-10", 245000.0000, 165800.0000, 6),
        (7, 3, 13, "ADMIN_APPROVED", "2026-10", 112400.0000, 76050.0000, 4),
        (8, 4, 14, "CVA_APPROVED", "2026-11", 198600.0000, 134400.0000, 5),
        (9, 1, 11, "SUBMITTED", "2026-11", 64200.0000, 43450.0000, 3),
        (10, 2, 2, "PAID_OUT", "2026-08", 215000.0000, 145500.0000, 9),
    ]
    for rid, pid, sid, st, period, energy, co2, vcount in reports:
        cva_id_sql = "NULL" if st == "SUBMITTED" else "1"
        cva_name_sql = "NULL" if st == "SUBMITTED" else "'Trần Thị Minh Châu'"
        lines.append(
            f"INSERT IGNORE INTO emission_reports (id, created_at, updated_at, submitted_at, period, source, status, total_energy, total_co2, vehicle_count, project_id, seller_id, verified_by_cva_id, verified_by_cva_name, ai_pre_score, verification_score) "
            f"VALUES ({rid}, DATE_SUB(NOW(), INTERVAL {15 - rid} DAY), NOW(), DATE_SUB(NOW(), INTERVAL {15 - rid} DAY), '{period}', 'CSV_UPLOAD', '{st}', {energy:.4f}, {co2:.4f}, {vcount}, {pid}, {sid}, {cva_id_sql}, {cva_name_sql}, 94.50, 96.00);"
        )

    # Populate report #5 (CREDIT_ISSUED, company 2) and #10 (PAID_OUT, company 2) with 9 vehicles / 8 EV owners each!
    comp2_vehicles = [
        ("51K-889.21", 28400.0),
        ("30K-662.18", 24100.0),
        ("51K-901.12", 22500.0),
        ("30K-711.45", 19800.0),
        ("43A-512.88", 21200.0),
        ("51L-118.67", 17400.0),
        ("65A-334.91", 18900.0),
        ("72A-661.55", 16300.0),
        ("79A-445.21", 15600.0),
    ]
    detail_id = 100
    for report_id, project_id, company_id, period, factor in [
        (5, 1, 2, "2026-09", 1.0),
        (10, 2, 2, "2026-08", 1.167),
    ]:
        for plate, en in comp2_vehicles:
            energy = round(en * factor, 4)
            co2_kg = round(energy * 0.6767, 4)
            lines.append(
                f"INSERT IGNORE INTO emission_report_details (id, co2_kg, company_id, period, project_id, total_energy, vehicle_plate, report_id) "
                f"VALUES ({detail_id}, {co2_kg:.4f}, {company_id}, '{period}', {project_id}, {energy:.4f}, '{plate}', {report_id});"
            )
            detail_id += 1

    # 6. Credit Batches (for reports 5, 6, 10 + extra)
    batches = [
        (10, "BATCH-VGM-2026-09-005", 124, 2505, 2628, 124.650, 2, 1, 5),
        (11, "BATCH-XSM-2026-10-006", 165, 2629, 2793, 165.800, 10, 2, 6),
        (12, "BATCH-VGM-2026-08-010", 145, 2794, 2938, 145.500, 2, 2, 10),
    ]
    for bid, bcode, cnt, sfrom, sto, tco2, cid, pid, rid in batches:
        lines.append(
            f"INSERT IGNORE INTO credit_batches (id, batch_code, created_at, credits_count, expires_at, issued_at, issued_by, residual_tco2e, serial_from, serial_prefix, serial_to, status, total_tco2e, vintage_year, company_id, project_id, report_id) "
            f"VALUES ({bid}, '{bcode}', NOW(), {cnt}, '2030-12-31', NOW(), 'admin@gmail.com', {tco2:.3f}, {sfrom}, 'VN-CBO-2026', {sto}, 'ISSUED', {tco2:.3f}, 2026, {cid}, {pid}, {rid});"
        )

    # 7. Marketplace Listings (rich variety across companies & prices)
    # Use existing carbon_credit_ids or create new carbon_credits for companies
    extra_credits = [
        (901, "CR-XSM-2026-01", "Tín chỉ Taxi Điện Xanh SM Q3", 1200.0, 19.50, 10, 2, 11),
        (902, "CR-DBK-2026-01", "Tín chỉ Xe Máy Điện Dat Bike", 650.0, 15.80, 11, 1, 10),
        (903, "CR-SLX-2026-01", "Tín chỉ Đổi Pin Selex Motors", 480.0, 17.20, 12, 3, 10),
        (904, "CR-MLG-2026-01", "Tín chỉ Mai Linh EV Fleet", 950.0, 20.40, 13, 2, 11),
        (905, "CR-EVN-2026-01", "Tín chỉ Trạm Sạc Mặt Trời EVN", 1800.0, 24.90, 14, 4, 11),
        (906, "CR-VGM-2026-09", "Tín chỉ VinFast Mobility Q3/2026", 540.0, 22.00, 2, 1, 10),
        (907, "CR-VGN-2026-10", "Tín chỉ V-Green Sạc Nhanh T10", 860.0, 26.50, 1, 6, 1),
        (908, "CR-XSM-2026-10", "Tín chỉ Xanh SM Miền Trung", 420.0, 18.00, 10, 3, 11),
    ]
    for cr_id, code, name, amt, price, cid, pid, bid in extra_credits:
        lines.append(
            f"INSERT IGNORE INTO carbon_credits (id, created_at, updated_at, amount, carbon_credit, credit_code, current_price, expiry_date, issued_at, issued_by, listed_amount, name, status, t_co2e, vintage_year, batch_id, company_id, project_id) "
            f"VALUES ({cr_id}, NOW(), NOW(), {amt:.2f}, {amt:.3f}, '{code}', {price:.2f}, '2030-12-31', NOW(), 'admin@gmail.com', {amt:.2f}, '{esc(name)}', 'LISTED', {amt:.3f}, 2026, {bid}, {cid}, {pid});"
        )

    listings = [
        (10, 10, 901, 880.0, 1200.0, 320.0, 19.50, "AVAILABLE"),
        (11, 11, 902, 500.0, 650.0, 150.0, 15.80, "AVAILABLE"),
        (12, 12, 903, 480.0, 480.0, 0.0, 17.20, "AVAILABLE"),
        (13, 13, 904, 710.0, 950.0, 240.0, 20.40, "AVAILABLE"),
        (14, 14, 905, 1450.0, 1800.0, 350.0, 24.90, "AVAILABLE"),
        (15, 2, 906, 440.0, 540.0, 100.0, 22.00, "AVAILABLE"),
        (16, 1, 907, 660.0, 860.0, 200.0, 26.50, "AVAILABLE"),
        (17, 10, 908, 0.0, 420.0, 420.0, 18.00, "SOLD"),
    ]
    for lid, cid, cr_id, qty, orig, sold, price, st in listings:
        lines.append(
            f"INSERT IGNORE INTO marketplace_listings (id, created_at, expires_at, original_quantity, price_per_credit, quantity, sold_quantity, status, carbon_credits_id, company_id) "
            f"VALUES ({lid}, DATE_SUB(NOW(), INTERVAL {20 - lid} DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), {orig:.4f}, {price:.2f}, {qty:.4f}, {sold:.4f}, '{st}', {cr_id}, {cid});"
        )

    # 8. Orders (history of marketplace trades)
    orders_data = [
        (10, 2, 10, 901, 100.0, 19.50),
        (11, 13, 11, 902, 150.0, 15.80),
        (12, 10, 15, 906, 100.0, 22.00),
        (13, 2, 16, 907, 200.0, 26.50),
        (14, 14, 13, 904, 240.0, 20.40),
        (15, 11, 14, 905, 350.0, 24.90),
    ]
    for oid, buyer_cid, lid, cr_id, qty, up in orders_data:
        tot = round(qty * up, 2)
        fee = round(tot * 0.05, 2)
        payout = round(tot - fee, 2)
        lines.append(
            f"INSERT IGNORE INTO orders (id, completed_at, created_at, order_status, order_type, platform_fee, quantity, seller_payout, total_price, unit_price, carbon_credit_id, company_id, listing_id) "
            f"VALUES ({oid}, DATE_SUB(NOW(), INTERVAL {20 - oid} DAY), DATE_SUB(NOW(), INTERVAL {20 - oid} DAY), 'SUCCESS', 'BUY', {fee:.2f}, {qty:.4f}, {payout:.2f}, {tot:.2f}, {up:.2f}, {cr_id}, {buyer_cid}, {lid});"
        )

    # 9. Profit Distribution + Details (showing real historical multi-owner payouts!)
    lines.append(
        "INSERT IGNORE INTO profit_distribution (id, created_at, updated_at, claim_token, status, total_credits_distributed, total_money_distributed, company_user_id, emission_report_id, project_id) "
        "VALUES (10, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'seed-dist-10', 'COMPLETED', 145.50, 2910.00, 2, 10, 2);"
    )
    pd_rows = [
        (101, 4, 28400.0, 22.40, 448.00),
        (102, 201, 24100.0, 19.00, 380.00),
        (103, 202, 22500.0, 17.75, 355.00),
        (104, 203, 19800.0, 15.60, 312.00),
        (105, 204, 21200.0, 16.70, 334.00),
        (106, 205, 17400.0, 13.70, 274.00),
        (107, 206, 18900.0, 14.90, 298.00),
        (108, 207, 16300.0, 12.85, 257.00),
        (109, 208, 15600.0, 12.60, 252.00),
    ]
    for pdid, owner_id, en, cr, mon in pd_rows:
        lines.append(
            f"INSERT IGNORE INTO profit_distribution_detail (id, created_at, updated_at, credit_amount, energy_amount, money_amount, status, distribution_id, ev_owner_id) "
            f"VALUES ({pdid}, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), {cr:.2f}, {en:.2f}, {mon:.2f}, 'SUCCESS', 10, {owner_id});"
        )

    # 10. Wallet Transactions (traceable ledger entries with credit_batch_id & order_id)
    txns = [
        (101, 2, "BUY_CARBON_CREDIT", 1950.00, 250265.00, 248315.00, "Mua 100 tín chỉ Taxi Điện Xanh SM", None, 10, 11),
        (102, 101, "SELL_CARBON_CREDIT", 1852.50, 418147.50, 420000.00, "Bán 100 tín chỉ (sau phí sàn 5%)", None, 10, 11),
        (103, 2, "EV_OWNER_PAYOUT", 448.00, 248763.00, 248315.00, "Chia sẻ doanh thu lô BATCH-VGM-2026-08-010", 10, None, 12),
        (104, 4, "EV_OWNER_PAYOUT_RECEIVE", 448.00, 802.00, 1250.00, "Nhận chia sẻ doanh thu lô BATCH-VGM-2026-08-010 (serial 2794-2816)", 10, None, 12),
        (105, 201, "EV_OWNER_PAYOUT_RECEIVE", 380.00, 2070.00, 2450.00, "Nhận chia sẻ doanh thu lô BATCH-VGM-2026-08-010", 10, None, 12),
        (106, 202, "EV_OWNER_PAYOUT_RECEIVE", 355.00, 1535.50, 1890.50, "Nhận chia sẻ doanh thu lô BATCH-VGM-2026-08-010", 10, None, 12),
        (107, 203, "EV_OWNER_PAYOUT_RECEIVE", 312.00, 2808.75, 3120.75, "Nhận chia sẻ doanh thu lô BATCH-VGM-2026-08-010", 10, None, 12),
        (108, 105, "ADD_MONEY", 50000.00, 630000.00, 680000.00, "Nạp ví doanh nghiệp qua Stripe", None, None, None),
    ]
    for tid, wid, ttype, amt, bbefore, bafter, desc, dist_id, ord_id, batch_id in txns:
        d_sql = str(dist_id) if dist_id else "NULL"
        o_sql = str(ord_id) if ord_id else "NULL"
        b_sql = str(batch_id) if batch_id else "NULL"
        lines.append(
            f"INSERT IGNORE INTO wallet_transaction (id, amount, balance_before, balance_after, created_at, description, transaction_type, wallet_id, distribution_id, order_id, credit_batch_id) "
            f"VALUES ({tid}, {amt:.2f}, {bbefore:.2f}, {bafter:.2f}, DATE_SUB(NOW(), INTERVAL {110 - tid} HOUR), '{esc(desc)}', '{ttype}', {wid}, {d_sql}, {o_sql}, {b_sql});"
        )

    # 11. Withdrawals (PENDING, APPROVED, REJECTED for Admin queue)
    withdrawals = [
        (10, 201, 450.00, "PENDING", None),
        (11, 203, 620.00, "PENDING", None),
        (12, 207, 1200.00, "PENDING", None),
        (13, 202, 300.00, "APPROVED", "NOW()"),
        (14, 205, 500.00, "SUCCEEDED", "DATE_SUB(NOW(), INTERVAL 2 DAY)"),
        (15, 206, 950.00, "REJECTED", "DATE_SUB(NOW(), INTERVAL 3 DAY)"),
    ]
    for wdid, uid, amt, st, proc in withdrawals:
        proc_sql = proc if proc else "NULL"
        lines.append(
            f"INSERT IGNORE INTO withdrawal (id, amount, requested_at, processed_at, status, user_id) "
            f"VALUES ({wdid}, {amt:.2f}, DATE_SUB(NOW(), INTERVAL {16 - wdid} DAY), {proc_sql}, '{st}', {uid});"
        )

    lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    return "\n".join(lines) + "\n"


def main() -> int:
    sql = build_sql()
    proc = subprocess.run(
        ["mysql", "--default-character-set=utf8mb4", DB],
        input=sql,
        text=True,
        capture_output=True,
    )
    if proc.returncode != 0:
        print(proc.stderr, file=sys.stderr)
        return proc.returncode
    print("Rich demo data seeded successfully.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
