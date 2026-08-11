-- Realign the benefit catalog with the Inbound Travel Medical Insurance Policy
-- Document (July 2026), §5 "Limits of Cover", which defines exactly five
-- benefits. The BenefitType enum was renamed to match the document wording, so
-- existing benefit_type values must be migrated or they would fail to
-- deserialize (EnumType.STRING).

update benefits set benefit_type = 'MEDICAL_EXPENSES'     where benefit_type = 'EMERGENCY_MEDICAL_EXPENSES';
update benefits set benefit_type = 'PRESCRIBED_MEDICINES' where benefit_type = 'PRESCRIPTION_MEDICINES';
update benefits set benefit_type = 'MENTAL_ILLNESS'       where benefit_type = 'HOSPITAL_BENEFITS';

-- PERSONAL_ACCIDENT is no longer part of the mandated catalog. Soft-delete
-- those benefits and any visitor_benefits assigned from them; the partial
-- unique indexes only cover live rows, so soft-deleted rows drop out cleanly
-- and are never loaded (@SQLRestriction "deleted = false").
update visitor_benefits vb
set deleted = true, deleted_date = now()
from benefits b
where vb.benefit_id = b.id
  and b.benefit_type = 'PERSONAL_ACCIDENT'
  and vb.deleted = false;

update benefits
set deleted = true, deleted_date = now()
where benefit_type = 'PERSONAL_ACCIDENT'
  and deleted = false;
