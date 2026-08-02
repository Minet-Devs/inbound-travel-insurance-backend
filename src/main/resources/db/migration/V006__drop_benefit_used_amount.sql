-- Benefits no longer track consumption; the draw-down mechanism was removed.

alter table benefits drop column used_amount;
