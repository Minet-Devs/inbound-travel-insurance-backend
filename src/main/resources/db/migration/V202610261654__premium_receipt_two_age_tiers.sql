alter table premium_receipts
    drop column infant_premium;

update premium_receipts
set minor_premium = 22,
    total_premium = 44;
