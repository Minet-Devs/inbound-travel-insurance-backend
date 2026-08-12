-- Add branding logo URL and policy token to insurers.

alter table insurers add column logo_url varchar(500);
alter table insurers add column policy_token bigint;
