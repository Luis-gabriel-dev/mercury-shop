package dev.adastratech.mercuryshop.shared.audit;

/**
 * Porta de saída da trilha de auditoria: registra um evento de segurança de forma append-only.
 * O {@code detail} já vem seguro (userId ou e-mail mascarado) — nunca PII crua nem segredos.
 */
public interface AuditEventStore {

    void append(String event, String detail, String requestId);
}
