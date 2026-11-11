alter table claims add column status_changed_at timestamp with time zone;
alter table claims add column status_comments text;
alter table claims add column minet_last_event_id varchar(100);
