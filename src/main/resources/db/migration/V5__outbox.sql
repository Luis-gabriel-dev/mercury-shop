-- V5: Transactional Outbox — Fase 6.
-- Eventos de domínio são gravados nesta tabela DENTRO da mesma transação da escrita de negócio
-- (ex.: pagamento). Um relay publica no RabbitMQ depois e marca como PUBLISHED, garantindo
-- entrega at-least-once mesmo que o broker esteja indisponível no instante do commit.

create table outbox_event (
    id           uuid          primary key,
    type         varchar(160)  not null,   -- nome da classe do evento (para desserializar)
    routing_key  varchar(120)  not null,
    payload      varchar(4000) not null,   -- evento serializado em JSON
    status       varchar(20)   not null,   -- PENDING | PUBLISHED
    attempts     integer       not null default 0,
    created_at   timestamptz   not null,
    published_at timestamptz
);

-- O relay busca pendentes em ordem de criação; o índice cobre esse acesso.
create index idx_outbox_pending on outbox_event (status, created_at);