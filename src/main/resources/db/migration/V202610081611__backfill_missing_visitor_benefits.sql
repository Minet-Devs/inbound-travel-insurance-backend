-- Back-fills visitor_benefits for visitors that have no live row for a catalog benefit
-- (e.g. visitors created before auto-seeding existed, or while the catalog was empty).
-- Mirrors VisitorCreatedListener: each missing benefit is assigned at the catalog limit
-- with the visitor's current status. Existing rows are never touched, and the NOT EXISTS
-- guard keeps the migration idempotent and within uq_visitor_benefits_visitor_id_benefit_id.
-- Uses gen_random_uuid() (built in from PostgreSQL 13).
insert into visitor_benefits (id, visitor_id, benefit_id, limit_amount, status, deleted, created_date, updated_date)
select gen_random_uuid(), v.id, b.id, b.limit_amount, v.visitor_status, false, now(), now()
from visitors v
cross join benefits b
where v.deleted = false
  and b.deleted = false
  and not exists (
      select 1
      from visitor_benefits vb
      where vb.visitor_id = v.id
        and vb.benefit_id = b.id
        and vb.deleted = false);
