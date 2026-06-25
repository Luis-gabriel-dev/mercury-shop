package dev.adastratech.mercuryshop.order.adapter.in.web;

import dev.adastratech.mercuryshop.order.adapter.in.web.dto.OrderResponse;
import dev.adastratech.mercuryshop.order.adapter.in.web.dto.PageResponse;
import dev.adastratech.mercuryshop.order.application.OrderService;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Operações administrativas de pedidos: listagem de todos (ADMIN) e fulfillment ship/deliver
 * (ADMIN ou STAFF). Autorização no SecurityConfig sobre /v1/admin/**.
 */
@RestController
@RequestMapping("/v1/admin/orders")
class AdminOrderController {

    private final OrderService orderService;

    AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    PageResponse<OrderResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") PageQuery.Direction direction) {
        PageQuery query = new PageQuery(page, size, sort, direction);
        return PageResponse.from(orderService.listAll(query).map(OrderWebMapper::toResponse));
    }

    @PostMapping("/{id}/ship")
    OrderResponse ship(@PathVariable UUID id) {
        return OrderWebMapper.toResponse(orderService.ship(id));
    }

    @PostMapping("/{id}/deliver")
    OrderResponse deliver(@PathVariable UUID id) {
        return OrderWebMapper.toResponse(orderService.deliver(id));
    }
}
