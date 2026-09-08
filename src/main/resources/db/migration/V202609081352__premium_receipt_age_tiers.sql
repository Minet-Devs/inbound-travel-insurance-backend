alter table premium_receipts
    add column infant_premium numeric(15, 2) not null default 0,
    add column minor_premium numeric(15, 2) not null default 22;
