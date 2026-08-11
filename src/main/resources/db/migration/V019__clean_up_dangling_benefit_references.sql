-- V018 wiped and reseeded the benefit catalog, so any pre-existing row that
-- references a benefit now points at a benefit_id that no longer exists.
-- benefit_id is NOT NULL on all of these tables, so the references cannot be
-- nulled out — the dangling rows are removed. Order: claims (may point at a
-- preauthorization) -> preauthorizations -> visitor_benefits.

delete from claims;
delete from preauthorizations;
delete from visitor_benefits;
