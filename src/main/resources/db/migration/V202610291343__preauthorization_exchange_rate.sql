-- KES->USD rate snapshotted when a pre-authorization is created; null when the rate service was unavailable.
alter table preauthorizations add column if not exists exchange_rate numeric(12, 6);
