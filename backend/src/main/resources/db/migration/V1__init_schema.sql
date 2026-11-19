create table client (
    id            bigint generated always as identity primary key,
    name          varchar(120) not null,
    industry      varchar(80)  not null,
    contact_name  varchar(120) not null,
    contact_email varchar(254) not null,
    created_at    timestamptz  not null
);

create table content (
    id         bigint generated always as identity primary key,
    client_id  bigint       not null references client (id),
    title      varchar(200) not null,
    type       varchar(20)  not null check (type in ('SOCIAL_POST', 'BLOG_POST', 'WEB_PAGE')),
    channel    varchar(60),
    status     varchar(20)  not null check (status in ('DRAFT', 'IN_REVIEW', 'PUBLISHED')),
    due_date   date         not null,
    notes      text,
    created_at timestamptz  not null,
    updated_at timestamptz  not null
);

create index idx_content_client_id on content (client_id);
create index idx_content_status on content (status);
create index idx_content_due_date on content (due_date);

create table activity_log (
    id          bigint generated always as identity primary key,
    content_id  bigint      not null references content (id),
    action      varchar(20) not null check (action in ('CREATED', 'UPDATED', 'STATUS_CHANGED')),
    from_status varchar(20) check (from_status in ('DRAFT', 'IN_REVIEW', 'PUBLISHED')),
    to_status   varchar(20) check (to_status in ('DRAFT', 'IN_REVIEW', 'PUBLISHED')),
    created_at  timestamptz not null
);

create index idx_activity_log_content_id on activity_log (content_id);
create index idx_activity_log_created_at on activity_log (created_at);
