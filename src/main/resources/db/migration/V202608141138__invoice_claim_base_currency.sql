-- Base-currency (USD) equivalents for invoices, invoice items and claims.
--
-- Frontend inputs arrive in KES and are stored raw (unchanged); the USD base
-- equivalents are computed at save time using the ExchangeRate-API snapshot
-- (see CurrencyConversionService) and cached for 24h. Legacy rows have no
-- historical rate available, so they are backfilled at a 1.0 rate with their
-- raw amounts as the base-equivalent snapshot.

alter table invoices add column if not exists exchange_rate    numeric(12, 6);
alter table invoices add column if not exists base_currency    varchar(3);
alter table invoices add column if not exists base_total_amount numeric(15, 2);
alter table invoices add column if not exists fx_rate_date     timestamptz;

alter table invoice_items add column if not exists base_unit_price numeric(15, 2);
alter table invoice_items add column if not exists base_amount      numeric(15, 2);

alter table claims add column if not exists currency            varchar(3);
alter table claims add column if not exists base_currency       varchar(3);
alter table claims add column if not exists claimed_amount_base numeric(15, 2);
alter table claims add column if not exists exchange_rate       numeric(12, 6);
alter table claims add column if not exists fx_rate_date        timestamptz;

update invoices
set exchange_rate = 1.0,
    base_currency = 'USD',
    base_total_amount = total_amount,
    fx_rate_date = now();

update invoice_items
set base_unit_price = unit_price,
    base_amount = amount;

update claims
set currency = 'KES',
    base_currency = 'USD',
    claimed_amount_base = claimed_amount,
    exchange_rate = 1.0,
    fx_rate_date = now();

alter table invoices
    alter column exchange_rate set not null,
    alter column base_currency set not null,
    alter column base_total_amount set not null,
    alter column fx_rate_date set not null;

alter table invoice_items
    alter column base_unit_price set not null,
    alter column base_amount set not null;

alter table claims
    alter column currency set not null,
    alter column base_currency set not null;
