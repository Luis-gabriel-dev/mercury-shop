package dev.adastratech.mercuryshop.user.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Resposta de usuário. Nunca expõe passwordHash nem campos internos (version). */
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String status,
        boolean emailVerified,
        List<String> roles,
        Instant createdAt) {
}
