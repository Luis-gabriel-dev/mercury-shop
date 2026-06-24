package dev.adastratech.mercuryshop.cart.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Quantidade absoluta; 0 remove o item. */
public record SetCartItemQuantityRequest(

        @NotNull
        @PositiveOrZero
        Integer quantity) {
}
