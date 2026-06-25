-- V7: MFA/TOTP — Fase 8.
-- Segredo TOTP (Base32) e flag de ativação por usuário. mfa_enabled só vira true após o usuário
-- confirmar um código válido no /mfa/enable.
alter table users add column mfa_secret  varchar(64);
alter table users add column mfa_enabled boolean not null default false;
