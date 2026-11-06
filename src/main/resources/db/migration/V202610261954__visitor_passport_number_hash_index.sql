-- Index the passport-number blind index. V202608171753 dropped uq_visitors_passport_number
-- (it indexed the now-encrypted passport_number) and left passport_number_hash un-indexed until
-- the backfill finished; the follow-up index was never added, so every visitor create
-- (existsByPassportNumberHash) and by-passport lookup (findByPassportNumberHash) scanned the table.
--
-- Partial over live rows, like the index it replaces: soft-deleted visitors may be re-created and the
-- entity's @SQLRestriction("deleted = false") makes every lookup match this predicate. Also restores
-- the duplicate-passport guarantee at the database level for concurrent creates.
-- Assumes backfill is complete: no live duplicate hashes (a duplicate would fail this migration).
create unique index if not exists uq_visitors_passport_number_hash
    on visitors (passport_number_hash)
    where deleted = false;
