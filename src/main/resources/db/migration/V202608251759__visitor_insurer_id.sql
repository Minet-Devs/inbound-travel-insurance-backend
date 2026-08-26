-- Denormalize the visitor's insurer onto the visitors table, mirroring the
-- existing claims.insurer_id column, so visitors can be scoped/queried by
-- insurer without joining through policies.

alter table visitors add column insurer_id uuid;

update visitors v
set insurer_id = p.insurer_id
from policies p
where p.id = v.policy_id;

alter table visitors alter column insurer_id set not null;

create index idx_visitors_insurer_id on visitors (insurer_id);
