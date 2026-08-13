-- Attach a medical service to an invoice (ID-only cross-feature reference,
-- following the pattern of claim_invoices). The service name is resolved
-- through MedicalServiceService when building the API response.

alter table invoices add column if not exists medical_service_id uuid references medical_services (id);

create index if not exists idx_invoices_medical_service_id on invoices (medical_service_id);
