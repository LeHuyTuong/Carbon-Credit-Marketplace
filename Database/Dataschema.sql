enum user_status { ACTIVE; INACTIVE; SUSPENDED }
enum issuance_status { DRAFT; SUBMITTED; UNDER_REVIEW; APPROVED; REJECTED }
enum listing_type { FIXED; AUCTION }
enum listing_status { DRAFT; ACTIVE; FILLED; CANCELED; EXPIRED }
enum order_status { PENDING; PAID; CANCELED; SETTLED }
enum bid_status { OPEN; OUTBID; WON; CANCELED }
enum payout_status { REQUESTED; PROCESSING; COMPLETED; FAILED }
enum wallet_type { CASH; CARBON }
enum direction_t { DR; CR }

Table roles {
  id bigint [pk, increment]
  code varchar [not null, unique]         // ví dụ: ADMIN, EV_OWNER, CVA
  description text
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table users {
  id bigint [pk, increment]
  email varchar [not null, unique]
  password_hash varchar [not null]
  full_name varchar [not null]
  phone varchar
  status user_status [not null, default: 'ACTIVE']
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table user_roles {
  user_id bigint [not null]
  role_id bigint [not null]
  created_at datetime [not null]
  Note: 'PRIMARY KEY (user_id, role_id)'
}

Table organizations {
  id bigint [pk, increment]
  name varchar [not null]
  type varchar [not null]                  // 'CVA', 'COMPANY', 'MARKET_OP'
  region_id bigint
  status user_status [not null, default: 'ACTIVE']
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table regions {
  id bigint [pk, increment]
  code varchar [not null, unique]          // ví dụ: VN.HCM, VN.HN, JP.TK...
  name varchar [not null]
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table emission_factors {
  id bigint [pk, increment]
  region_code varchar [not null]           // FK -> regions.code
  factor_ice_kgco2_per_km decimal(10,4) [not null]
  factor_grid_kgco2_per_kwh decimal(10,4) [not null]
  effective_from date [not null]
  effective_to date [not null]
  created_at datetime [not null]
  updated_at datetime [not null]
  Note: 'UNIQUE(region_code, effective_from)'
}

Table vehicles {
  id bigint [pk, increment]
  owner_id bigint [not null]               // FK -> users.id
  vin varchar(17) [not null, unique]
  make varchar [not null]
  model varchar [not null]
  year smallint [not null]
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table issuance_requests {
  id bigint [pk, increment]
  owner_id bigint [not null]               // users.id
  vehicle_id bigint [not null]             // vehicles.id
  period_start date [not null]
  period_end date [not null]
  est_reduction_tco2e decimal(12,3) [not null]
  status issuance_status [not null, default: 'SUBMITTED']
  submitted_at datetime
  decided_at datetime
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table certificates {
  id bigint [pk, increment]
  request_id bigint [not null]             // issuance_requests.id
  serial_no varchar [not null, unique]
  issued_tco2e decimal(12,3) [not null]
  issuer_org_id bigint [not null]          // organizations.id (CVA)
  issued_at datetime [not null]
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table credit_lots {
  id bigint [pk, increment]
  certificate_id bigint [not null]         // certificates.id
  owner_id bigint [not null]               // users.id
  quantity_tco2e decimal(12,3) [not null]
  remaining_tco2e decimal(12,3) [not null]
  region_code varchar [not null]           // regions.code
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table wallets {
  id bigint [pk, increment]
  user_id bigint [not null]                // users.id
  wallet_type wallet_type [not null]       // CASH or CARBON
  currency char(3)                         // ISO 4217, chỉ dùng với CASH
  created_at datetime [not null]
  updated_at datetime [not null]
  Note: 'UNIQUE(user_id, wallet_type, currency)'
}

Table ledger_accounts {
  id bigint [pk, increment]
  wallet_id bigint                          // null = system account
  code varchar [not null, unique]
  name varchar [not null]
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table ledger_entries {
  id bigint [pk, increment]
  account_id bigint [not null]             // ledger_accounts.id
  ref_type varchar [not null]              // 'ORDER','PAYOUT','WITHDRAWAL',...
  ref_id bigint [not null]
  direction direction_t [not null]         // DR/CR
  amount decimal(18,6) [not null]
  occurred_at datetime [not null]
}

Table listings {
  id bigint [pk, increment]
  seller_id bigint [not null]              // users.id
  lot_id bigint [not null]                 // credit_lots.id
  type listing_type [not null]             // FIXED/AUCTION
  quantity_tco2e decimal(12,3) [not null]
  unit_price decimal(18,6) [not null]
  start_at datetime [not null]
  end_at datetime [not null]
  region_code varchar [not null]           // redundant từ lot.region_code để filter
  status listing_status [not null, default: 'ACTIVE']
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table bids {
  id bigint [pk, increment]
  listing_id bigint [not null]             // listings.id
  buyer_id bigint [not null]               // users.id
  quantity_tco2e decimal(12,3) [not null]
  bid_price decimal(18,6) [not null]
  status bid_status [not null, default: 'OPEN']
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table orders {
  id bigint [pk, increment]
  listing_id bigint [not null]             // listings.id
  buyer_id bigint [not null]               // users.id
  quantity_tco2e decimal(12,3) [not null]
  unit_price decimal(18,6) [not null]
  status order_status [not null, default: 'PENDING']
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table trades {
  id bigint [pk, increment]
  order_id bigint [not null]               // orders.id
  fee_amount decimal(18,6) [not null, default: 0]
  net_amount decimal(18,6) [not null]      // quantity * price - fee
  settled_at datetime
  created_at datetime [not null]
  updated_at datetime [not null]
}

Table audit_logs {
  id bigint [pk, increment]
  actor_id bigint                           // users.id (nullable cho system)
  action varchar [not null]
  target_type varchar [not null]
  target_id bigint [not null]
  details json
  occurred_at datetime [not null]
}

/* FKs (viết chuẩn chiều parent -> child) */
Ref: user_roles.user_id > users.id
Ref: user_roles.role_id > roles.id
Ref: organizations.region_id > regions.id
Ref: emission_factors.region_code > regions.code
Ref: vehicles.owner_id > users.id
Ref: issuance_requests.owner_id > users.id
Ref: issuance_requests.vehicle_id > vehicles.id
Ref: certificates.request_id > issuance_requests.id
Ref: certificates.issuer_org_id > organizations.id
Ref: credit_lots.certificate_id > certificates.id
Ref: credit_lots.owner_id > users.id
Ref: credit_lots.region_code > regions.code
Ref: wallets.user_id > users.id
Ref: ledger_accounts.wallet_id > wallets.id
Ref: ledger_entries.account_id > ledger_accounts.id
Ref: listings.seller_id > users.id
Ref: listings.lot_id > credit_lots.id
Ref: listings.region_code > regions.code
Ref: bids.listing_id > listings.id
Ref: bids.buyer_id > users.id
Ref: orders.listing_id > listings.id
Ref: orders.buyer_id > users.id
Ref: trades.order_id > orders.id
