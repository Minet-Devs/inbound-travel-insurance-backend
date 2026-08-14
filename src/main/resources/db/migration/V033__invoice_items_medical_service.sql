alter table invoices drop column if exists medical_service_id;
alter table invoice_items add column if not exists medical_service_id uuid references medical_services (id);
create index if not exists idx_invoice_items_medical_service_id on invoice_items (medical_service_id);
