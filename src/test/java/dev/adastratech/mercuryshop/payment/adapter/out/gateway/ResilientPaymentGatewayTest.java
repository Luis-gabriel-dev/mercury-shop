package dev.adastratech.mercuryshop.payment.adapter.out.gateway;

import dev.adastratech.mercuryshop.payment.domain.PaymentGateway;
import dev.adastratech.mercuryshop.shared.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Resiliência do gateway (Fase 10) — unitário, sem Spring: o decorator repete falhas transitórias e,
 * esgotadas as tentativas, converte em 503 (ServiceUnavailableException); no sucesso, delega direto.
 */
class ResilientPaymentGatewayTest {

    private static final int MAX_ATTEMPTS = 3;

    private ResilientPaymentGateway wrapping(PaymentGateway delegate) {
        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(MAX_ATTEMPTS).waitDuration(Duration.ofMillis(1)).build());
        return new ResilientPaymentGateway(delegate, CircuitBreakerRegistry.ofDefaults(), retryRegistry);
    }

    @Test
    void retriesThenTranslatesToServiceUnavailable() {
        AtomicInteger calls = new AtomicInteger();
        PaymentGateway failing = new PaymentGateway() {
            @Override
            public PaymentIntent createIntent(UUID orderId, BigDecimal amount, String currency) {
                calls.incrementAndGet();
                throw new IllegalStateException("gateway fora");
            }

            @Override
            public PaymentEvent parseEvent(String payload, String signatureHeader) {
                return null;
            }
        };

        assertThatThrownBy(() ->
                wrapping(failing).createIntent(UUID.randomUUID(), new BigDecimal("10.00"), "brl"))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(calls.get()).isEqualTo(MAX_ATTEMPTS); // repetiu antes de desistir
    }

    @Test
    void passesThroughWhenGatewaySucceeds() {
        PaymentGateway ok = new PaymentGateway() {
            @Override
            public PaymentIntent createIntent(UUID orderId, BigDecimal amount, String currency) {
                return new PaymentIntent("pi_1", "pi_1_secret");
            }

            @Override
            public PaymentEvent parseEvent(String payload, String signatureHeader) {
                return null;
            }
        };

        PaymentGateway.PaymentIntent intent =
                wrapping(ok).createIntent(UUID.randomUUID(), new BigDecimal("10.00"), "brl");

        assertThat(intent.reference()).isEqualTo("pi_1");
        assertThat(intent.clientSecret()).isEqualTo("pi_1_secret");
    }
}
