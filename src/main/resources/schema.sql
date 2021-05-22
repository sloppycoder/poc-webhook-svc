create table if not exists webhook_requests (
    id serial primary key,
    message_id varchar(36), -- uuid of message
    client_id varchar(16),
    message_type varchar(16),
    payload varchar(2000),
    status varchar(10),
    retries int default  0,
    retry_after timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create unique index if not exists webhook_requests_message_id on webhook_requests(message_id);
create index if not exists webhook_requets_client_id on webhook_requests(client_id);