-- Optional premium amount actually paid by the visitor, to 2 decimal places.
alter table visitors
    add column premium_amount_paid numeric(12, 2);
