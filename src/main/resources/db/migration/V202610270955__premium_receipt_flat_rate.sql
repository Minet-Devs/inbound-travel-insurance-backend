alter table premium_receipts
    drop column minor_premium;

update premium_receipts
set total_premium = 44;
