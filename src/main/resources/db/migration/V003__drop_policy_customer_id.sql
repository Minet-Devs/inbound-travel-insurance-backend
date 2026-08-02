-- Policies no longer reference a customer user. The CUSTOMER role was removed
-- from the user model; a dedicated Customer entity may replace this later.

drop index idx_policies_customer_id;

alter table policies drop column customer_id;