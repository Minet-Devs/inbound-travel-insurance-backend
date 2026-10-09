insert into organizations (id, name, organization_type, email, deleted, created_date, updated_date)
values (
    'c4fb4b07-468d-4063-8088-e4ad4f10534b',
    'Minet Insurance Brokers Limited',
    'ADMIN',
    'hussein.mishobo@minet.co.ke',
    false,
    now(),
    now()
);

insert into users (id, first_name, last_name, email, password, role, organization_id, deleted, created_date, updated_date)
values (
    '596401d0-da7b-44fe-b446-f9330ff8f4ae',
    'Hussein',
    'Mishobo',
    'hussein.mishobo@minet.co.ke',
    '$2a$10$TNUTZKqlca5wQzdics1TJ.KoImwMtDIuOYLwAjVVBv0Ivv5oZZpiC',
    'ADMIN',
    'c4fb4b07-468d-4063-8088-e4ad4f10534b',
    false,
    now(),
    now()
);
