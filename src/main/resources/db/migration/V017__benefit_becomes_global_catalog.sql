-- Benefits become a standalone global catalog: a benefit is now identified by
-- a free-text name and a limit of cover, no longer scoped to a policy or a
-- fixed BenefitType enum.
--
-- benefit_name is backfilled from the retired benefit_type (ENUM_CASE turned
-- into Title Case, e.g. MEDICAL_EXPENSES -> "Medical Expenses"). Existing rows
-- were one-per-policy, so names may repeat across rows; that is allowed — the
-- catalog does not enforce name uniqueness.

alter table benefits add column benefit_name varchar(255);

update benefits
set benefit_name = initcap(replace(benefit_type, '_', ' '));

alter table benefits alter column benefit_name set not null;

-- Drop the policy scoping: the per-policy uniqueness index, the lookup index,
-- and the columns themselves.
drop index if exists uq_benefits_policy_id_benefit_type;
drop index if exists idx_benefits_policy_id;

alter table benefits drop column benefit_type;
alter table benefits drop column policy_id;
