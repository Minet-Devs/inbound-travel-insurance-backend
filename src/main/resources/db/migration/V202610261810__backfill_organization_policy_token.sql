-- Organization.policy_token was only ever copied organization -> insurer, so it
-- drifted as policies were consumed/restored. Copy the insurer's current token
-- onto its organization. Organizations linked to more than one insurer are
-- skipped, since there is no unambiguous value to copy.
update organizations o
set policy_token = i.policy_token
from insurers i
where i.organization_id = o.id
  and o.policy_token is distinct from i.policy_token
  and (select count(*) from insurers x where x.organization_id = o.id) = 1;
