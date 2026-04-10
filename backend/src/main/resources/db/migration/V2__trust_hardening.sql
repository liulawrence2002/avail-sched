alter table events
    add column results_visibility varchar(32) not null default 'aggregate_public';

alter table participants
    add column email varchar(320);

create unique index if not exists idx_participants_event_email_unique
    on participants (event_id, lower(email))
    where email is not null;
