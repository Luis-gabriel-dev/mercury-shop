package dev.adastratech.mercuryshop.product.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de domínio de Categoria — puro, sem dependência de JPA/HTTP.
 * A identidade (UUID) é atribuída no momento da criação, no domínio.
 */
public class Category {

    private final UUID id;
    private String name;
    private String description;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Category(UUID id, String name, String description, Long version,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = normalizeName(name);
        this.description = description;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Cria uma nova categoria com identidade nova. Timestamps/versão são preenchidos na persistência. */
    public static Category create(String name, String description) {
        return new Category(UUID.randomUUID(), name, description, null, null, null);
    }

    /** Reconstrói uma categoria já existente (vinda da persistência). */
    public static Category reconstitute(UUID id, String name, String description, Long version,
                                        Instant createdAt, Instant updatedAt) {
        return new Category(id, name, description, version, createdAt, updatedAt);
    }

    public void rename(String name) {
        this.name = normalizeName(name);
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        return name.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
