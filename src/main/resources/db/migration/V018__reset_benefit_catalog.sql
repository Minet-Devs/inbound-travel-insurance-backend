-- Reset the global benefit catalog to the Inbound Travel Medical Insurance
-- Policy Document (July 2026), §5 "Limits of Cover": remove all existing
-- benefit rows and seed exactly the five mandated benefits with their limits.

delete from benefits;

insert into benefits (id, benefit_name, limit_amount, deleted, created_date, updated_date)
values
    ('062480d1-71a3-476c-9ba5-2d26cb748493', 'Medical Expenses', 20000.00, false, now(), now()),
    ('33e0f026-73a4-44d6-aab6-7dc14cbb7ba1', 'Emergency Medical Transportation/Evacuation', 25000.00, false, now(), now()),
    ('943aa918-9bc8-4f5f-90ff-ebe6b3097e7a', 'Prescribed Medicines', 300.00, false, now(), now()),
    ('237a625d-cbea-4da3-9c47-7e7238f8437f', 'Mental Illness', 1000.00, false, now(), now()),
    ('08546a27-43c8-49a2-86d4-bf1cbc4769b5', 'Repatriation of Mortal Remains', 5000.00, false, now(), now());
