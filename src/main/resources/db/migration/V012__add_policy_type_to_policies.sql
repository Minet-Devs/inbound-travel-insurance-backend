-- Cover-period type mandated by the Ministry of Health framework
-- (single-entry <=30d, single-entry 31-60d, IPMI 61d-12mo). Existing rows
-- default to the shortest period; new rows always supply an explicit type.

alter table policies
    add column policy_type varchar(40) not null default 'SINGLE_ENTRY_UP_TO_30_DAYS';
