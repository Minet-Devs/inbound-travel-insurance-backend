-- Seed a bootstrap admin user for initial system access.
-- Credentials: admin@travel.local / admin123 (BCrypt) — change the password
-- immediately in any non-local environment.

insert into users (id, first_name, last_name, email, password, deleted, created_date, updated_date)
values (
    'ad3e13e1-5458-446d-bf52-0ff34a4e3142',
    'System',
    'Admin',
    'admin@travel.local',
    '$2a$10$aOb3RJyL.c8GAX955yruGO2TmuRxgKSpHceEm0vuJL/7mSWKXYAMK',
    false,
    now(),
    now()
);

insert into user_roles (user_id, role)
values ('ad3e13e1-5458-446d-bf52-0ff34a4e3142', 'ADMIN');