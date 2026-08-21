alter table users add column role varchar(50);

update users u
set role = coalesce(
    (select ur.role from user_roles ur where ur.user_id = u.id order by ur.role limit 1),
    'ADMIN'
);

alter table users alter column role set not null;

drop table user_roles;
