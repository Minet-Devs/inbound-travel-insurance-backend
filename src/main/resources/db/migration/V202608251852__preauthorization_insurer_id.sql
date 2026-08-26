-- Denormalize the preauthorization's insurer onto the preauthorizations
-- table, mirroring visitors.insurer_id/claims.insurer_id, so preauthorizations
-- can be scoped/queried by insurer without joining through policies.

alter table preauthorizations add column insurer_id uuid;

update preauthorizations pa
set insurer_id = p.insurer_id
from policies p
where p.id = pa.policy_id;

alter table preauthorizations alter column insurer_id set not null;

create index idx_preauthorizations_insurer_id on preauthorizations (insurer_id);
