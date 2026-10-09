-- Reverts V202610071452: visitors are ACTIVE on creation again (restoring V021's default),
-- and the PENDING_ACTIVATION value goes back to PENDING (benefit column default as before). Rows still waiting for activation
-- are set ACTIVE; note no certificate serial or activation email is issued for them.
update visitors set visitor_status = 'ACTIVE' where visitor_status = 'PENDING_ACTIVATION';
update visitor_benefits set status = 'ACTIVE' where status = 'PENDING_ACTIVATION';

alter table visitors alter column visitor_status set default 'ACTIVE';
alter table visitor_benefits alter column status set default 'PENDING';
