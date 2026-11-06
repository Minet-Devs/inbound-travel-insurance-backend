-- USD equivalent of a claim's approved amount, for the claims dashboard.
--
-- approved_amount is stored in the claim's own currency. The base amount is the
-- approved amount scaled by the claim's claimed_amount_base / claimed_amount ratio
-- (rather than exchange_rate, because claimed_amount_base is the sum of invoice
-- base totals when invoices are attached). Legacy claims were backfilled at a 1.0
-- rate, so their figure is the same approximation as claimed_amount_base.

alter table claims add column if not exists approved_amount_base numeric(15, 2);

update claims
set approved_amount_base = round(approved_amount * claimed_amount_base / claimed_amount, 2)
where approved_amount is not null
  and claimed_amount > 0
  and claimed_amount_base is not null;
