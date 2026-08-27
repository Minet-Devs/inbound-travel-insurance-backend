create sequence certificate_serial_seq start with 1 increment by 1;

alter table visitors add column certificate_serial_number varchar(32);

create unique index uq_visitors_certificate_serial_number
    on visitors (certificate_serial_number)
    where deleted = false and certificate_serial_number is not null;
