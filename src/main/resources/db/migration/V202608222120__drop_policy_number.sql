-- policyNumber no longer identifies a Policy; the policy id (UUID) is the
-- sole identifier now that policies are no longer tied to an external
-- policy-number scheme.

alter table policies
    drop constraint uq_policies_policy_number,
    drop column policy_number;
