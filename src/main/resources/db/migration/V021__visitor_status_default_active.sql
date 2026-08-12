-- Visitors now start ACTIVE rather than PENDING (see Visitor entity default).
-- Align the column default so inserts that bypass JPA agree with the entity.

alter table visitors alter column visitor_status set default 'ACTIVE';
