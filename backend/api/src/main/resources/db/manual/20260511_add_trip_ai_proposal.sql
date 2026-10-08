create table trip_ai_proposal (
    trip_ai_proposal_id bigserial primary key,
    type varchar(50) not null,
    status varchar(50) not null,
    requester_member_id bigint not null,
    title varchar(255) not null,
    region_code varchar(100) not null,
    country_code varchar(10) not null,
    start_date date not null,
    end_date date not null,
    companion_type varchar(50) not null,
    travel_styles varchar(255) not null,
    input_snapshot text not null,
    ai_response_snapshot text not null,
    created_at timestamp not null,
    applied_at timestamp null,
    created_trip_id bigint null
);

create index idx_trip_ai_proposal_requester_status
    on trip_ai_proposal (requester_member_id, status);

create index idx_trip_ai_proposal_region_status
    on trip_ai_proposal (region_code, status);
