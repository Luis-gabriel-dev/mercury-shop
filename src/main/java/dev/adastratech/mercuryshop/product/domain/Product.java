package dev.adastratech.mercuryshop.product.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de domínio de Produto — puro, sem dependência de JPA/HTTP.
 * Mantém as invariantes do catálogo (preço e estoque não-negativos, nome obrigatório).
 * O campo {@code version} prepara o terreno para o lock otimista da Fase 3.
 * Implementa {@link Serializable} para poder ser cacheado no Redis (Fase 4).
 */
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private UUID categoryId;
    private boolean active;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Product(UUID id, String name, String description, BigDecimal price, int stockQuantity,
                    UUID categoryId, boolean active, Long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = normalizeName(name);
        this.description = description;
        this.price = requireNonNegativePrice(price);
        this.stockQuantity = requireNonNegativeStock(stockQuantity);
        this.categoryId = categoryId;
        this.active = active;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Cria um novo produto (ativo) com identidade nova. */
    public static Product create(String name, String description, BigDecimal price,
                                 int stockQuantity, UUID categoryId) {
        return new Product(UUID.randomUUID(), name, description, price, stockQuantity,
                categoryId, true, null, null, null);
    }

    /** Reconstrói um produto já existente (vindo da persistência). */
    public static Product reconstitute(UUID id, String name, String description, BigDecimal price,
                                       int stockQuantity, UUID categoryId, boolean active, Long version,
                                       Instant createdAt, Instant updatedAt) {
        return new Product(id, name, description, price, stockQuantity, categoryId, active,
                version, createdAt, updatedAt);
    }

    public void rename(String name) {
        this.name = normalizeName(name);
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changePrice(BigDecimal price) {
        this.price = requireNonNegativePrice(price);
    }

    public void changeStockQuantity(int stockQuantity) {
        this.stockQuantity = requireNonNegativeStock(stockQuantity);
    }

    public void changeCategory(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        return name.trim();
    }

    private static BigDecimal requireNonNegativePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Product price must not be null");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Product price must not be negative");
        }
        return price;
    }

    private static int requireNonNegativeStock(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Product stock quantity must not be negative");
        }
        return stockQuantity;
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

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public boolean isActive() {
        return active;
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
