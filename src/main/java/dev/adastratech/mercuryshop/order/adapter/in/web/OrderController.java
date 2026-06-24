package dev.adastratech.mercuryshop.order.adapter.in.web;

import dev.adastratech.mercuryshop.order.adapter.in.web.dto.OrderResponse;
import dev.adastratech.mercuryshop.order.adapter.in.web.dto.PageResponse;
import dev.adastratech.mercuryshop.order.application.CheckoutService;
import dev.adastratech.mercuryshop.order.application.OrderService;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/orders")
class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    OrderController(CheckoutService checkoutService, OrderService orderService) {
        this.checkoutService = checkoutService;
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse checkout(@AuthenticationPrincipal Jwt jwt,
                           @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return OrderWebMapper.toResponse(checkoutService.checkout(userId(jwt), idempotencyKey));
    }

    @GetMapping
    PageResponse<OrderResponse> listOwn(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") PageQuery.Direction direction) {
        PageQuery query = new PageQuery(page, size, sort, direction);
        return PageResponse.from(orderService.listOwn(userId(jwt), query).map(OrderWebMapper::toResponse));
    }

    @GetMapping("/{id}")
    OrderResponse get(@AuthenticationPrincipal Jwt jwt, Authentication authentication, @PathVariable UUID id) {
        return OrderWebMapper.toResponse(orderService.get(id, userId(jwt), isAdmin(authentication)));
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, Authentication authentication, @PathVariable UUID id) {
        return OrderWebMapper.toResponse(orderService.cancel(id, userId(jwt), isAdmin(authentication)));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
