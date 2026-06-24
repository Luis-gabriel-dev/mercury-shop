package dev.adastratech.mercuryshop.cart.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, BigDecimal total) {
}
