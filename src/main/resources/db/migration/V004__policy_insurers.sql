-- A policy can now be backed by multiple insurers. Move the single
-- policies.insurer_id column into a policy_insurers collection table,
-- preserving existing assignments.

create table policy_insurers (
    policy_id  uuid not null references policies (id),
    insurer_id uuid not null,
    primary key (policy_id, insurer_id)
);

insert into policy_insurers (policy_id, insurer_id)
select id, insurer_id from policies;

drop index idx_policies_insurer_id;

alter table policies drop column insurer_id;

create index idx_policy_insurers_insurer_id on policy_insurers (insurer_id);