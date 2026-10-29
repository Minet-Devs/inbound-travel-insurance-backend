-- Backfill pre-authorizations created before exchange_rate was captured with the KES->USD rate 0.007702.
update preauthorizations set exchange_rate = 0.007702 where exchange_rate is null;
