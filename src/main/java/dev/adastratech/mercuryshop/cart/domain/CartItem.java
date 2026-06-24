package dev.adastratech.mercuryshop.cart.domain;

import java.util.UUID;

/** Item do carrinho: referência ao produto + quantidade (preço é resolvido na leitura). */
public record CartItem(UUID productId, int quantity) {
}
