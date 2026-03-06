create table events (
    id bigserial primary key,
    public_id varchar(32) not null unique,
    host_token varchar(128) not null unique,
    title varchar(160) not null,
    description text,
    timezone varchar(64) not null,
    slot_minutes integer not null,
    duration_minutes integer not null,
    start_date date not null,
    end_date date not null,
    daily_start_time time not null,
    daily_end_time time not null,
    created_at timestamptz not null default now()
);

create table participants (
    id bigserial primary key,
    event_id bigint not null references events(id) on delete cascade,
    token varchar(128) not null unique,
    display_name varchar(120) not null,
    created_at timestamptz not null default now()
);

create index idx_participants_event_id on participants(event_id);

create table availability (
    id bigserial primary key,
    participant_id bigint not null references participants(id) on delete cascade,
    event_id bigint not null references events(id) on delete cascade,
    slot_start_utc timestamptz not null,
    weight numeric(3,2) not null,
    unique (participant_id, slot_start_utc)
);

create index idx_availability_event_slot on availability(event_id, slot_start_utc);

create table final_selection (
    id bigserial primary key,
    event_id bigint not null unique references events(id) on delete cascade,
    slot_start_utc timestamptz not null,
    finalized_at timestamptz not null default now()
);

create table event_stats (
    event_id bigint primary key references events(id) on delete cascade,
    view_count bigint not null default 0,
    response_count bigint not null default 0
);

