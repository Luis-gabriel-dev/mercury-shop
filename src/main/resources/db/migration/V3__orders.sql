-- V3: pedidos e itens de pedido — Fase 3.

create table orders (
    id              uuid           primary key,
    user_id         uuid           not null,
    status          varchar(20)    not null,
    total           numeric(12, 2) not null,
    idempotency_key varchar(120)   not null,
    version         bigint         not null default 0,
    created_at      timestamptz    not null,
    updated_at      timestamptz    not null,
    constraint uq_orders_idempotency_key unique (idempotency_key)
);

create table order_items (
    id           uuid           primary key,
    order_id     uuid           not null,
    product_id   uuid           not null,
    product_name varchar(160),
    unit_price   numeric(12, 2) not null,
    quantity     integer        not null,
    line_total   numeric(12, 2) not null,
    constraint fk_order_items_order foreign key (order_id) references orders (id) on delete cascade
);

create index idx_orders_user_id on orders (user_id);
create index idx_order_items_order_id on order_items (order_id);
