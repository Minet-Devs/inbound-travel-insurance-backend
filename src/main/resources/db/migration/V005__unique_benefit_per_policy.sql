-- A policy must not carry two benefits with the same name. Case-insensitive,
-- and scoped to live rows so a soft-deleted benefit's name can be reused.

create unique index uq_benefits_policy_id_name
    on benefits (policy_id, lower(name))
    where deleted = false;
