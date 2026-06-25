package dev.adastratech.mercuryshop.payment.adapter.in.web;

import dev.adastratech.mercuryshop.payment.adapter.in.web.dto.PaymentResponse;
import dev.adastratech.mercuryshop.payment.application.PaymentService;
import dev.adastratech.mercuryshop.payment.domain.Payment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/orders")
class PaymentController {

    private final PaymentService paymentService;

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{id}/pay")
    PaymentResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Payment payment = paymentService.pay(id, UUID.fromString(jwt.getSubject()));
        return new PaymentResponse(payment.getOrderId(), payment.getId(),
                payment.getStatus().name(), payment.getAmount());
    }
}
