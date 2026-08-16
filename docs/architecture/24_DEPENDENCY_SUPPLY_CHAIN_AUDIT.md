# 24 — Dependency & Supply-Chain Audit

> Method: static analysis of `pom.xml` (full read) + knowledge-based version risk. `mvn dependency:tree` not executed in audit (offline env) — transitive picture: UNKNOWN beyond what pom implies. No automatic upgrades recommended.

## Direct Dependency Findings

| Dependency | Version | Status | Risk | Recommendation |
|---|---|---|---|---|
| spring-boot (BOM import) | 3.3.3 | Behind current 3.3.x/3.4 patches | Medium — Spring Boot patch releases carry security fixes; exact applicable CVEs UNKNOWN without advisory scan | Plan upgrade within 3.3.x latest, then 3.4 line; run OWASP dependency-check in CI (24-A) |
| jjwt (api/impl/jackson) | 0.11.5 | Old line (0.12.x current) | Medium — parsing API changes; no high-profile CVE known to audit, verify via scanner | Upgrade during Phase 2 with code migration (`parserBuilder` → new API) |
| paypal rest-api-sdk | 1.14.0 | **Deprecated/legacy SDK** (PayPal recommends Checkout SDK) | Medium — EOL-ish; no signature/webhook helpers matching modern flow | Replace as part of payment rewrite (ADR-002); do not patch around it |
| stripe-java | 24.9.0 | Acceptable at audit date | Low | Keep current with dependabot/renovate once CI exists |
| org.json | 20231013 | Redundant alongside Jackson | Low | Remove after usage check (AI configs) |
| jackson-dataformat-csv | declared **twice** (one unversioned, one 2.17.2) | Duplicate | Low | Dedupe (keep BOM-managed) |
| spring-boot-starter-oauth2-client | declared **twice** | Duplicate | Low | Remove one |
| commons-csv 1.10.0 **and** jackson-dataformat-csv **and** POI | three CSV/XLS stacks | Low-Medium (surface area) | Consolidate to one CSV stack after usage map (N14) |
| flying-saucer-pdf 9.1.22 **and** openhtmltopdf 1.0.10 | two HTML→PDF stacks | Low | Pick one (openhtmltopdf is the maintained fork family) |
| google-genai 1.24.0 + google-auth + httpclient5 | plus WebFlux WebClient Vertex stack | Two AI client stacks (A2) | Medium complexity, Low security | Consolidate behind one client in Phase 5 |
| p6spy 3.9.1 | new (uncommitted) | Dev-only tool | None — ensure prod profile disables SQL logging |
| mapstruct + lombok + binding | processors configured; mappers disabled | Dead weight | Remove or activate (ADR-007) |
| mysql-connector-j | BOM-managed | OK | Low | — |
| AWS SDK s3/auth/regions 2.25.48 | Slightly behind | Low | Routine bump with CI scanner |

## Known-Vulnerability Check

Status: **UNKNOWN** — no scanner run (no CI, offline audit). Do not treat absence of findings as safety. Required: OWASP dependency-check or GitHub `dependabot` alerts on the repo (it is public — enable Dependabot **now**, zero code change).

## Supply-Chain Practices

| Practice | State |
|---|---|
| Version pinning | Yes (explicit versions; BOM for Spring) |
| Dependency review in PRs | No CI at all |
| Automated advisories (Dependabot/renovate) | Absent — highest-value zero-code fix |
| SBOM | Absent |
| Unused dependency sweep | Needed: mapstruct (disabled), org.json, one CSV stack, one PDF stack, thymeleaf (only VNPay views), websocket starter (usage UNKNOWN — SSE is the visible mechanism; verify before removing) |

## 24-A — Minimum Actions (no upgrades)

1. Enable GitHub Dependabot alerts + security updates on the public repo (config file only).
2. Dedupe the two duplicate dependencies.
3. Add `mvn dependency:tree` output to CI artifacts for future audits.
4. Decide MapStruct in/out (ADR-007 gate).
