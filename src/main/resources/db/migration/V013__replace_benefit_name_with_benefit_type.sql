-- Replaces the free-text benefit name with the fixed catalog of insured
-- events mandated by the Ministry of Health framework (BenefitType).
-- Existing rows default to PERSONAL_ACCIDENT; uniqueness moves from name to
-- type, still scoped per policy and to live rows.

drop index if exists uq_benefits_policy_id_name;

alter table benefits drop column name;
alter table benefits add column benefit_type varchar(40) not null default 'PERSONAL_ACCIDENT';

create unique index uq_benefits_policy_id_benefit_type
    on benefits (policy_id, benefit_type)
    where deleted = false;
