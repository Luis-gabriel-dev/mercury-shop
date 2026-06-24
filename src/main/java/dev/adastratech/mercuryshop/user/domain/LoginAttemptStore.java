package dev.adastratech.mercuryshop.user.domain;

import java.time.Duration;

/**
 * Porta para o controle de tentativas de login (no Redis). Após N falhas dentro da
 * janela, a conta fica temporariamente bloqueada (backoff) — evita brute force sem
 * marcar o usuário como BLOCKED permanentemente.
 */
public interface LoginAttemptStore {

    boolean isLocked(String email);

    /** Registra uma falha; ao atingir o limite, aplica o bloqueio temporário. */
    void recordFailure(String email);

    /** Limpa o contador (login bem-sucedido). */
    void reset(String email);

    /** Tempo restante de bloqueio (para o cabeçalho Retry-After); {@code ZERO} se não bloqueado. */
    Duration lockTimeRemaining(String email);
}
