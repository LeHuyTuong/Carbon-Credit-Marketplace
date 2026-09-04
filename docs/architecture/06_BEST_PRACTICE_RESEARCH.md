# 06 — Best-Practice Research & Reference Repositories

## Methodology

Web research performed 2026-08-16: Spring Framework official reference (transactions), Stripe official webhook docs, PayPal webhook practice guides, modular-monolith/package-by-feature guides (Spring Modulith ecosystem, Ensolvers, codecentric, foojay). Each claim cross-checked against ≥2 sources where it drives a recommendation. Canonical repositories (Petclinic, Keycloak, JHipster) are cited from established public knowledge and flagged as such — their layouts are stable and publicly verifiable.

## Findings Applied to This Repository

### F1 — `@Transactional`: Spring's annotation vs jakarta's
- **Sources**: [Spring Framework reference — Using @Transactional](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) · [Baeldung — Spring vs JTA @Transactional](https://www.baeldung.com/spring-vs-jta-transactional) · [7 proxy gotchas](https://dev.to/thellu/why-transactional-doesnt-work-in-spring-7-proxy-gotchas-and-fixes-1l2b)
- **Finding**: jakarta's variant works in Spring but lacks `readOnly`, `isolation`, `timeout`, `NESTED`, and flexible rollback rules; self-invocation bypasses proxies for both.
- **Applied to**: Debt S1 (16 vs 7 file split, money paths on jakarta) and S3 (`getSelf()` hack exists *because* of self-invocation).

### F2 — Payment confirmation must be server-verified, idempotent, webhook-driven
- **Sources**: [Stripe webhooks (official)](https://docs.stripe.com/webhooks) · [PayPal webhooks guide](https://hookdeck.com/webhooks/platforms/guide-to-paypal-webhooks-features-and-best-practices) · [Stripe webhook best practices](https://www.stigg.io/blog-posts/best-practices-i-wish-we-knew-when-integrating-stripe-webhooks) · [Handling payment webhooks reliably](https://medium.com/@sohail_saifii/handling-payment-webhooks-reliably-idempotency-retries-validation-69b762720bf5) · [Stripe engineering: resilient webhook handlers](https://stripe.dev/blog/building-resilient-webhook-handlers-aws-dlqs-stripe-events)
- **Finding**: verify signature server-side → persist event → idempotent processing (event id / status under lock) → return fast; redirect pages must never trigger crediting.
- **Applied to**: the P0 deposit fix (13_SECURITY §SC1) and the `PaymentGateway` port in 09.

### F3 — Modular monolith + package-by-feature for small/medium teams
- **Sources**: [Spring Modulith](https://spring.io/projects/spring-modulith) · [codecentric: Spring Modulith + hexagonal](https://www.codecentric.de/en/knowledge-hub/blog/modularization-the-easy-way-spring-modulith-with-kotlin-and-hexagonal-architecture) · [Combining modular monolith and hexagonal](https://notes.softwarearchitect.id/p/combining-modular-monolith-and-hexagonal) · [Ensolvers comparative guide](https://www.ensolvers.com/post/comparative-guide-to-application-architectures-with-spring-boot-multi-module-hexagonal-and-microservices) · [foojay hexagonal approach](https://foojay.io/today/clean-and-modular-java-a-hexagonal-architecture-approach/)
- **Finding**: single-deployable feature modules with *verified* boundaries beat multi-module/microservices at this scale; hexagonal internals justified only where real infrastructure churn exists (payments, AI).
- **Applied to**: ADR-001 (target architecture), 09 (package design).

## Reference Repositories / Projects (6)

### R1 — spring-projects/spring-petclinic
- **URL**: https://github.com/spring-projects/spring-petclinic
- **Architecture**: package-by-layer canonical sample (controller/service/repository/model).
- **Package structure**: technical packages; per-entity organization inside them.
- **Dependency direction**: strictly downward; entities JPA-annotated but thin.
- **Testing strategy**: integration-first (`@SpringBootTest` with JDBC test slices).
- **Module boundaries**: none enforced (deliberately minimal).
- **Relevant practices**: shows our current layout is the framework default — not an anomaly to apologize for.
- **Applicable**: baseline sanity; their test-slice approach worth adopting for repository fetch-graph tests.
- **Not applicable**: its 2-table simplicity.

### R2 — spring-projects/spring-modulith (+ docs/examples)
- **URL**: https://github.com/spring-projects/spring-modulith
- **Architecture**: modular monolith; feature packages as modules; `ApplicationModule` verification tests; application events as internal contract.
- **Package structure**: `com.example.<module>` with public API at module root.
- **Dependency direction**: enforced by `ApplicationModules.verify()` tests (cycles and illegal references fail the build).
- **Testing strategy**: `ApplicationModulesTest` + scenario tests per module.
- **Module boundaries**: explicit, test-verified.
- **Relevant practices**: boundary verification as tests (not tooling ceremony) is exactly the guardrail model for Phase 6 (we will use ArchUnit for the same effect without adopting the framework).
- **Applicable**: target package model (09), ArchUnit rules.
- **Not applicable**: full adoption mid-project (adds a framework dependency for what ArchUnit already proves).

### R3 — keycloak/keycloak
- **URL**: https://github.com/keycloak/keycloak
- **Architecture**: large modular monolith; Maven module per feature/spi.
- **Package structure**: `server-spi` (contracts) + per-feature provider modules.
- **Dependency direction**: features depend on SPI, never on each other's internals.
- **Testing strategy**: per-module unit + integration suites.
- **Module boundaries**: compile-time via Maven + SPI interfaces.
- **Relevant practices**: proof that feature modularity scales *within one deployable*.
- **Applicable**: long-term direction if the team grows; the "service interface = module public API" rule.
- **Not applicable**: Maven multi-module overhead for a 4-person academic team (now).

### R4 — JHipster (generated apps)
- **URL**: https://www.jhipster.tech/
- **Architecture**: opinionated layered monolith (or microservice blueprint).
- **Package structure**: package-by-feature within `service`/`web` folders; DTO + MapStruct mapper + service per entity.
- **Dependency direction**: web → service → repository; Assembler classes for view models.
- **Testing strategy**: per-entity service tests, `@SpringBootTest` IT, CI gates by default, Flyway from day one.
- **Module boundaries**: conventions + generator discipline.
- **Applicable**: the "boring baseline" checklist used in 07's gap table (Flyway, CI, mapper discipline, `readOnly` transactions).
- **Not applicable**: generator lock-in.

### R5 — Stripe sample integrations (official patterns)
- **URL**: https://docs.stripe.com/webhooks (+ Stripe engineering blog above)
- **Architecture**: n/a — integration patterns.
- **Relevant practices**: signature verification, event persistence, idempotent handlers, fast 200s.
- **Applicable**: Phase 1 design verbatim.
- **Not applicable**: none.

### R6 — Netflix/Uber engineering blogs (deferred)
- **URL**: n/a (not fetched this session)
- **Status**: UNKNOWN — not needed at this scale; microservice-era practices intentionally excluded per Principle 4 (no overengineering).

## Research Conclusions

1. Fix payments with the officially documented webhook/verify patterns (F2) before any structural work.
2. Standardize on Spring's `@Transactional` (F1) — already started on this branch.
3. Adopt package-by-feature modular monolith with ArchUnit-verified boundaries (F3/R2), single module, no microservices (R3's lesson without its cost).
4. Use JHipster/R4's boring baseline (Flyway, CI, mapper discipline) as the definition of "done" for Phases 2-3.
