
alter table preauthorizations
    add column converted_to_claim BOOLEAN default FALSE;
