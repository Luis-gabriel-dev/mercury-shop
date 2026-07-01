-- V9: trilha de auditoria de segurança persistida (Fase 11).
-- Append-only: a aplicação só INSERE (nunca update/delete). Sem PII — e-mails já entram mascarados
-- e o 'detail' guarda apenas userId ou e-mail mascarado. Consultável/retenível para compliance.

create table audit_event (
    id          uuid          primary key,
    event       varchar(60)   not null,   -- ex.: LOGIN_SUCCEEDED, REFRESH_TOKEN_REUSE_DETECTED
    detail      varchar(200),              -- userId=<uuid> ou email=<mascarado>
    request_id  varchar(60),
    created_at  timestamptz   not null
);

create index idx_audit_event_created_at on audit_event (created_at);
create index idx_audit_event_event on audit_event (event);
