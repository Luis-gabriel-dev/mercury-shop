-- V1: catálogo (categorias e produtos) — Fase 1.
-- Schema 100% versionado via Flyway; a aplicação roda com ddl-auto=validate.

create table categories (
    id          uuid          primary key,
    name        varchar(120)  not null,
    description varchar(500),
    version     bigint        not null default 0,
    created_at  timestamptz   not null,
    updated_at  timestamptz   not null,
    constraint uq_categories_name unique (name)
);

create table products (
    id             uuid           primary key,
    name           varchar(160)   not null,
    description    varchar(2000),
    price          numeric(12, 2) not null,
    stock_quantity integer        not null default 0,
    category_id    uuid,
    active         boolean        not null default true,
    version        bigint         not null default 0,
    created_at     timestamptz    not null,
    updated_at     timestamptz    not null,
    constraint fk_products_category foreign key (category_id) references categories (id),
    constraint chk_products_price check (price >= 0),
    constraint chk_products_stock check (stock_quantity >= 0)
);

-- Índice na coluna de busca/filtro mais comum (briefing seção 9).
create index idx_products_category_id on products (category_id);
