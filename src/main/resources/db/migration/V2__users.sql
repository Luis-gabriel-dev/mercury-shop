-- V2: usuários, papéis (RBAC) e tokens de uso único — Fase 2.

create table users (
    id             uuid          primary key,
    email          varchar(254)  not null,
    password_hash  varchar(100)  not null,
    full_name      varchar(160),
    phone          varchar(40),
    status         varchar(30)   not null,
    email_verified boolean       not null default false,
    version        bigint        not null default 0,
    created_at     timestamptz   not null,
    updated_at     timestamptz   not null,
    constraint uq_users_email unique (email)
);

create table user_roles (
    user_id uuid        not null,
    role    varchar(20) not null,
    constraint pk_user_roles primary key (user_id, role),
    constraint fk_user_roles_user foreign key (user_id) references users (id) on delete cascade
);

create table one_time_tokens (
    id         uuid         primary key,
    user_id    uuid         not null,
    token_hash varchar(64)  not null,
    purpose    varchar(30)  not null,
    expires_at timestamptz  not null,
    used_at    timestamptz,
    created_at timestamptz  not null,
    constraint uq_one_time_tokens_hash unique (token_hash),
    constraint fk_one_time_tokens_user foreign key (user_id) references users (id) on delete cascade
);

create index idx_one_time_tokens_user_purpose on one_time_tokens (user_id, purpose);
