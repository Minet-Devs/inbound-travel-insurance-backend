-- A policy may now cover many visitors: drop the one-live-visitor-per-policy
-- constraint and keep a plain index for by-policy lookups.

drop index uq_visitors_policy_id;

create index idx_visitors_policy_id on visitors (policy_id);
