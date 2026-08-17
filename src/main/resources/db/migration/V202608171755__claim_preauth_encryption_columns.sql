-- Widen claim/preauthorization free-text medical columns to text to
-- accommodate AES-256-GCM ciphertext (see common/crypto).

alter table claims
    alter column description type text,
    alter column prescription type text,
    alter column decision_reason type text;

alter table preauthorizations
    alter column service_description type text,
    alter column decision_reason type text;
