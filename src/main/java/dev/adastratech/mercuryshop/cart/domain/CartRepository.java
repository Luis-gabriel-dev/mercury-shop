package dev.adastratech.mercuryshop.cart.domain;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída para persistência do carrinho (implementada sobre Redis). */
public interface CartRepository {

    Optional<Cart> findByUserId(UUID userId);

    void save(Cart cart);

    void deleteByUserId(UUID userId);
}
