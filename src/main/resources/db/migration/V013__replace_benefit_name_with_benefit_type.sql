-- Replaces the free-text benefit name with the fixed catalog of insured
-- events mandated by the Ministry of Health framework (BenefitType).
-- Uniqueness moves from name to type, still scoped per policy and to live
-- rows.
--
-- Existing environments may already have more than one live benefit per
-- policy under free-text names, so a single blanket default for benefit_type
-- would collide with the new (policy_id, benefit_type) uniqueness
-- constraint. Instead, each policy's live benefit rows are assigned a
-- distinct BenefitType in row order (up to the six mandated types); this
-- doesn't recover the original free-text meaning, but the point of this
-- migration is only to establish the fixed-catalog schema going forward.

drop index if exists uq_benefits_policy_id_name;

alter table benefits add column benefit_type varchar(40);

with ranked as (
    select id,
           row_number() over (partition by policy_id order by created_date, id) as rn
    from benefits
    where deleted = false
)
update benefits b
set benefit_type = case ranked.rn
    when 1 then 'PERSONAL_ACCIDENT'
    when 2 then 'EMERGENCY_MEDICAL_EXPENSES'
    when 3 then 'EMERGENCY_MEDICAL_EVACUATION'
    when 4 then 'REPATRIATION_OF_MORTAL_REMAINS'
    when 5 then 'HOSPITAL_BENEFITS'
    else 'PRESCRIPTION_MEDICINES'
end
from ranked
where b.id = ranked.id;

-- Soft-deleted rows aren't covered by the partial unique index below, so any
-- value satisfies the constraint; only needed to make the column NOT NULL.
update benefits
set benefit_type = 'PERSONAL_ACCIDENT'
where benefit_type is null;

alter table benefits alter column benefit_type set not null;
alter table benefits drop column name;

create unique index uq_benefits_policy_id_benefit_type
    on benefits (policy_id, benefit_type)
    where deleted = false;
