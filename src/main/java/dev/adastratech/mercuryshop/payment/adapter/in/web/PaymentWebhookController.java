package dev.adastratech.mercuryshop.payment.adapter.in.web;

import dev.adastratech.mercuryshop.payment.application.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook do gateway de pagamento — público (Stripe não envia JWT), protegido pela verificação de
 * assinatura feita no gateway. Recebe o corpo cru (necessário para validar a assinatura) e responde
 * 2xx mesmo para eventos ignorados ou repetidos, para o gateway não reenviar.
 */
@RestController
@RequestMapping("/v1/payments")
class PaymentWebhookController {

    private final PaymentService paymentService;

    PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    ResponseEntity<Void> webhook(@RequestBody String payload,
                                 @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
