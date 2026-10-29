-- USD equivalent of a pre-authorization's approved amount (approved_amount * exchange_rate).
-- Null until approved, or when no exchange rate was captured.
alter table preauthorizations add column if not exists approved_amount_in_dollars numeric(15, 2);

-- Backfill historical rows (including soft-deleted ones).
update preauthorizations
set approved_amount_in_dollars = round(approved_amount * exchange_rate, 2)
where approved_amount is not null
  and exchange_rate is not null;
