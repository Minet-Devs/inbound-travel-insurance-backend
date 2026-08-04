-- Benefit assignments mirror their visitor's lifecycle status; existing rows
-- start as PENDING.

alter table visitor_benefits
    add column status varchar(20) not null default 'PENDING';
