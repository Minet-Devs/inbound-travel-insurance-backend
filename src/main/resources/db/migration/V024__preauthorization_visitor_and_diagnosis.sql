alter table preauthorizations
    add column visitor_id uuid,
    add column icd11_code_id uuid;

create index idx_preauthorizations_visitor_id on preauthorizations (visitor_id);
create index idx_preauthorizations_icd11_code_id on preauthorizations (icd11_code_id);