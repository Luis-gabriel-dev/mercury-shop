package dev.adastratech.mercuryshop.cart.adapter.in.web;

import dev.adastratech.mercuryshop.cart.adapter.in.web.dto.CartItemResponse;
import dev.adastratech.mercuryshop.cart.adapter.in.web.dto.CartResponse;
import dev.adastratech.mercuryshop.cart.application.CartView;

final class CartWebMapper {

    private CartWebMapper() {
    }

    static CartResponse toResponse(CartView view) {
        return new CartResponse(
                view.lines().stream()
                        .map(line -> new CartItemResponse(
                                line.productId(), line.name(), line.unitPrice(),
                                line.quantity(), line.lineTotal()))
                        .toList(),
                view.total());
    }
}
