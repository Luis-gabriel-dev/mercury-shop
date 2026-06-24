package dev.adastratech.mercuryshop.order.adapter.in.web;

import dev.adastratech.mercuryshop.order.adapter.in.web.dto.OrderItemResponse;
import dev.adastratech.mercuryshop.order.adapter.in.web.dto.OrderResponse;
import dev.adastratech.mercuryshop.order.domain.Order;

final class OrderWebMapper {

    private OrderWebMapper() {
    }

    static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getTotal(),
                order.getItems().stream()
                        .map(i -> new OrderItemResponse(i.productId(), i.productName(),
                                i.unitPrice(), i.quantity(), i.lineTotal()))
                        .toList(),
                order.getCreatedAt());
    }
}
