-- The Ministry of Health's three-tier cover-period classification (single
-- entry <=30d, single entry 31-60d, IPMI 61d-12mo) has been replaced by a
-- single policy covering any travel period from 1 day up to 12 months,
-- enforced directly on the visitor's dateIn/dateOut in VisitorServiceImpl.
-- policyType no longer expresses any real variation, so it is dropped.

alter table policies
    drop column policy_type;
