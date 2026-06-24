package dev.adastratech.mercuryshop.cart.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Carrinho de um usuário — guarda apenas produto + quantidade (o preço é resolvido no momento
 * da leitura/checkout, nunca congelado no carrinho). Modelo puro, sem dependência de infra.
 */
public class Cart {

    /** Limite de sanidade por item para evitar abuso. */
    public static final int MAX_QUANTITY_PER_ITEM = 1000;

    private final UUID userId;
    private final LinkedHashMap<UUID, Integer> items;

    private Cart(UUID userId, LinkedHashMap<UUID, Integer> items) {
        this.userId = userId;
        this.items = items;
    }

    public static Cart empty(UUID userId) {
        return new Cart(userId, new LinkedHashMap<>());
    }

    public static Cart of(UUID userId, Map<UUID, Integer> items) {
        return new Cart(userId, new LinkedHashMap<>(items));
    }

    /** Soma {@code quantity} à quantidade atual do produto. */
    public void add(UUID productId, int quantity) {
        requirePositive(quantity);
        int updated = items.getOrDefault(productId, 0) + quantity;
        items.put(productId, capped(updated));
    }

    /** Define a quantidade absoluta; {@code 0} remove o item. */
    public void setQuantity(UUID productId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }
        if (quantity == 0) {
            items.remove(productId);
        } else {
            items.put(productId, capped(quantity));
        }
    }

    public void remove(UUID productId) {
        items.remove(productId);
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CartItem> items() {
        return items.entrySet().stream()
                .map(e -> new CartItem(e.getKey(), e.getValue()))
                .toList();
    }

    public UUID userId() {
        return userId;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
    }

    private static int capped(int quantity) {
        return Math.min(quantity, MAX_QUANTITY_PER_ITEM);
    }
}
