-- Visitors now start PENDING_ACTIVATION (replacing PENDING, and reversing V021's
-- ACTIVE default); the certificate and activation email are issued on activation.
update visitors set visitor_status = 'PENDING_ACTIVATION' where visitor_status = 'PENDING';
update visitor_benefits set status = 'PENDING_ACTIVATION' where status = 'PENDING';

alter table visitors alter column visitor_status set default 'PENDING_ACTIVATION';
alter table visitor_benefits alter column status set default 'PENDING_ACTIVATION';
