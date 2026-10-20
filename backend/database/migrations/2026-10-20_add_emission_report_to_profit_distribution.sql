-- 2026-10-20: link a payout distribution to the emission report it pays.
--
-- Why: a retry must pay only the owners whose detail is FAILED. Without this
-- link the service can only re-run the whole report, which would pay the
-- already-SUCCESS owners a second time.
--
-- Production runs with spring.jpa.hibernate.ddl-auto=validate, so this ALTER
-- must be applied before deploying the application build that contains the
-- ProfitDistribution.emissionReport mapping.

ALTER TABLE `profit_distribution`
    ADD COLUMN `emission_report_id` BIGINT NULL AFTER `project_id`;

ALTER TABLE `profit_distribution`
    ADD CONSTRAINT `fk_profit_distribution_report`
        FOREIGN KEY (`emission_report_id`) REFERENCES `emission_reports` (`id`);

CREATE INDEX `idx_profit_distribution_report`
    ON `profit_distribution` (`emission_report_id`);
