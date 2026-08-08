-- Policy no longer carries its own cover date range: policyType already
-- expresses the cover-period category, and the actual per-traveler cover
-- period is each Visitor's own dateIn/dateOut, validated against the
-- policy's policyType.

alter table policies
    drop column cover_start_date,
    drop column cover_end_date;
