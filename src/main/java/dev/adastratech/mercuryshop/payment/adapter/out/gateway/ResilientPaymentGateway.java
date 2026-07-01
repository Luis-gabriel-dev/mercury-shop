package dev.adastratech.mercuryshop.payment.adapter.out.gateway;

import dev.adastratech.mercuryshop.payment.domain.PaymentGateway;
import dev.adastratech.mercuryshop.shared.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Decorator resiliente sobre o gateway de pagamento ativo (Fase 10). Aplica <b>retry</b> (falhas
 * transitórias) e <b>circuit breaker</b> (fail-fast quando o gateway está fora) na criação da
 * cobrança — a única chamada de rede. Falha após as tentativas, ou circuito aberto, viram um erro
 * <b>503</b> claro em vez de pendurar/estourar o request. O parse do webhook é local (HMAC), sem
 * rede: é delegado direto, para que erro de assinatura vire 400 na hora, sem retry.
 *
 * <p>É {@code @Primary}, então o {@code PaymentService} recebe este; o gateway concreto (stub ou
 * Stripe) é injetado pelo nome comum {@code paymentGatewayDelegate}.
 */
@Component
@Primary
class ResilientPaymentGateway implements PaymentGateway {

    static final String INSTANCE = "paymentGateway";
    private static final Logger log = LoggerFactory.getLogger(ResilientPaymentGateway.class);

    private final PaymentGateway delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    ResilientPaymentGateway(@Qualifier("paymentGatewayDelegate") PaymentGateway delegate,
                            CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.retry = retryRegistry.retry(INSTANCE);
    }

    @Override
    public PaymentIntent createIntent(UUID orderId, BigDecimal amount, String currency) {
        Supplier<PaymentIntent> call = () -> delegate.createIntent(orderId, amount, currency);
        // Composição: circuit breaker por FORA do retry — o CB só registra o desfecho final (após as
        // tentativas) e, quando aberto, corta a chamada sem nem tentar (CallNotPermittedException).
        Supplier<PaymentIntent> resilient =
                CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, call));
        try {
            return resilient.get();
        } catch (CallNotPermittedException open) {
            throw new ServiceUnavailableException(
                    "Gateway de pagamento temporariamente indisponível; tente novamente em instantes");
        } catch (RuntimeException failure) {
            log.warn("Falha ao criar cobrança no gateway após retries: {}", failure.getMessage());
            throw new ServiceUnavailableException(
                    "Não foi possível contatar o gateway de pagamento; tente novamente");
        }
    }

    @Override
    public PaymentEvent parseEvent(String payload, String signatureHeader) {
        return delegate.parseEvent(payload, signatureHeader);
    }
}
