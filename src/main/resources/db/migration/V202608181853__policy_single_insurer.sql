-- Policies now cover exactly one insurer. Move the policy_insurers
-- collection table back into a single policies.insurer_id column.
-- Where a policy was (incorrectly) backed by more than one insurer,
-- the lowest insurer_id is kept deterministically.

alter table policies add column insurer_id uuid;

update policies p
set insurer_id = (
    select min(pi.insurer_id)
    from policy_insurers pi
    where pi.policy_id = p.id
);

alter table policies alter column insurer_id set not null;

drop table policy_insurers;

create index idx_policies_insurer_id on policies (insurer_id);
