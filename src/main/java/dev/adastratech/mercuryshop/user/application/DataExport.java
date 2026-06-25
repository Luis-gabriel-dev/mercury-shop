package dev.adastratech.mercuryshop.user.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Pacote de dados pessoais do usuário (portabilidade — LGPD). Contém apenas dados não sensíveis. */
public record DataExport(Profile profile, List<Order> orders) {

    public record Profile(UUID id, String email, String fullName, String phone, String status,
                          Set<String> roles, boolean emailVerified, boolean mfaEnabled, Instant createdAt) {
    }

    public record Order(UUID id, String status, BigDecimal total, Instant createdAt, List<Item> items) {
    }

    public record Item(String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }
}
