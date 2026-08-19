alter table visitors
    add column payment_reference varchar(255),
    add column eta_reference varchar(255),
    add column entry_timestamp timestamptz,
    add column exit_timestamp timestamptz,
    add column port_of_entry varchar(255);
