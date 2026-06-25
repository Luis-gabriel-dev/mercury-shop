-- V6: troca de e-mail — Fase 8.
-- O token de uso único passa a carregar o novo e-mail (alvo) para os tokens de propósito
-- EMAIL_CHANGE; nulo para os demais propósitos (verificação/reset).
alter table one_time_tokens add column payload varchar(254);
