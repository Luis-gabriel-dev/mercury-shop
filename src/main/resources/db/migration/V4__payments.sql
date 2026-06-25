-- V4: pagamentos e faturas — Fase 4. (orders.status já comporta o valor PAID.)

create table payments (
    id              uuid           primary key,
    order_id        uuid           not null,
    status          varchar(20)    not null,
    amount          numeric(12, 2) not null,
    transaction_ref varchar(100),
    created_at      timestamptz    not null,
    constraint uq_payments_order unique (order_id),
    constraint fk_payments_order foreign key (order_id) references orders (id) on delete cascade
);

create table invoices (
    id         uuid        primary key,
    order_id   uuid        not null,
    number     varchar(40) not null,
    issued_at  timestamptz not null,
    created_at timestamptz not null,
    constraint uq_invoices_order unique (order_id),
    constraint uq_invoices_number unique (number),
    constraint fk_invoices_order foreign key (order_id) references orders (id) on delete cascade
);
