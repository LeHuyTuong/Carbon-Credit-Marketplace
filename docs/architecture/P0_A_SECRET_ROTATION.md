# P0-A — Secret Rotation Checklist

> Status legend: **Code remediation complete** = values no longer in tracked files.
> **Rotation pending** = external provider action still required.
> **Rotation verified** = provider confirmed the old credential dead.
> Removing values from Git does NOT undo exposure — history still contains them. Rotation is mandatory.

## Exposure Summary (verified during P0-A, 2026-08-16)

| Secret type | Where exposed | Code remediation | Required rotation | Verification | Post-rotation validation | Status |
|---|---|---|---|---|---|---|
| JWT signing secret | `config/JwtConstant.java` (hardcoded 70-char string, public repo) | ✅ removed; injected from `jwt.secret` / `JWT_SECRET` | Generate a new ≥ 32-byte random secret; set `JWT_SECRET` in every deployment environment | Token signed with the old secret must fail validation (`JwtProviderSecretTest.tokenSignedWithOldSecret_isRejectedAfterRotation` covers the mechanism) | All users re-login (expected — old tokens invalid); login + OTP flows work | **Rotation pending external action** |
| Gmail SMTP credentials (address + app password) | `docker-compose.yml` (be service env) | ✅ replaced with `${SPRING_MAIL_USERNAME}` / `${SPRING_MAIL_PASSWORD}` | Revoke the app password in the Google account; issue a new one; update deployment env | Send-mail test from the deployment | Registration OTP email arrives; no auth errors in logs | **Rotation pending external action** |
| MySQL root password (`12345`) | `docker-compose.yml` (db + be + healthcheck) | ✅ replaced with `${MYSQL_ROOT_PASSWORD}` | Change on any real deployment that used it (compose is dev-only, but treat as exposed) | `mysql -u root -p<old>` must fail | App boots; healthcheck passes | **Rotation pending external action** |
| Production env values (DB, AWS, Stripe, PayPal, VNPay, Vertex SA key) | **NOT committed** — `application-prod.properties` is env-placeholder-based (audit-2 N5 partially a false positive; see P0_A_IMPLEMENTATION.md Corrections) and `carbonx-ai-*.json` is git-ignored | n/a (already clean) | Confirm the deployment host's env/secret store is the only source; no copies in chats/notes/CI variables history | `git grep` of tracked files shows placeholders only (`ProdJwtSecretContractTest` guards jwt) | n/a | **No repo action required; verify operator hygiene** |
| Demo account passwords (`DataInitializer`) | Hardcoded by design, published in README for the demo | Left as-is (intentional demo accounts on an academic project) | If the deployment ever handles real data: delete demo seeding or gate behind a profile | n/a | n/a | **Accepted risk — revisit before real money (P1)** |

## Operator Action List (in order)

1. **JWT**: `openssl rand -base64 48` → set `JWT_SECRET` in the deployment env → restart BE → confirm old tokens rejected (users re-login).
2. **Gmail**: revoke exposed app password → create new app password → set `SPRING_MAIL_USERNAME`/`SPRING_MAIL_PASSWORD` → send a test OTP.
3. **MySQL**: if any persistent DB used the compose password → `ALTER USER 'root'@'%' IDENTIFIED BY '<new>'` → update env → restart.
4. **Sweep**: re-check Stripe/PayPal/AWS/VNPay consoles for suspicious activity while the repo was public with the JWT secret (forged tokens were possible). Rotate those keys too if in doubt — cheap insurance.
5. **Local dev**: `cp .env.example .env` (root and/or `backend/Market_carbon/`) and fill values; `.env` is git-ignored.

## Git History Note

Historical copies of `JwtConstant.java` and `docker-compose.yml` still contain the old values in git history. This phase intentionally does **not** rewrite history (risk to collaborators' clones). After rotation makes the old values useless, a history rewrite (BFG/git-filter-repo) is optional cleanup — the security fix is the rotation, not the rewrite.
