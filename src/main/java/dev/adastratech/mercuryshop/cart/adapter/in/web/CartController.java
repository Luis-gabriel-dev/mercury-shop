package dev.adastratech.mercuryshop.cart.adapter.in.web;

import dev.adastratech.mercuryshop.cart.adapter.in.web.dto.AddCartItemRequest;
import dev.adastratech.mercuryshop.cart.adapter.in.web.dto.CartResponse;
import dev.adastratech.mercuryshop.cart.adapter.in.web.dto.SetCartItemQuantityRequest;
import dev.adastratech.mercuryshop.cart.application.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/cart")
class CartController {

    private final CartService cartService;

    CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return CartWebMapper.toResponse(cartService.view(userId(jwt)));
    }

    @PostMapping("/items")
    CartResponse addItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request) {
        return CartWebMapper.toResponse(
                cartService.addItem(userId(jwt), request.productId(), request.quantity()));
    }

    @PutMapping("/items/{productId}")
    CartResponse setItemQuantity(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId,
                                 @Valid @RequestBody SetCartItemQuantityRequest request) {
        return CartWebMapper.toResponse(
                cartService.setItemQuantity(userId(jwt), productId, request.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    CartResponse removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        return CartWebMapper.toResponse(cartService.removeItem(userId(jwt), productId));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clear(@AuthenticationPrincipal Jwt jwt) {
        cartService.clear(userId(jwt));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
