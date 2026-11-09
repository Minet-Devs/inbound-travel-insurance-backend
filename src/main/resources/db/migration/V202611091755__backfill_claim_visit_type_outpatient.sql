-- Claims created before visit_type existed (or without one) default to OUTPATIENT.
update claims set visit_type = 'OUTPATIENT' where visit_type is null or trim(visit_type) = '';
