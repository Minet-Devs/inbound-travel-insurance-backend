-- Lifecycle status for a visitor; existing rows start as PENDING.

alter table visitors
    add column visitor_status varchar(20) not null default 'PENDING';
