package dev.adastratech.mercuryshop.shared.observability;

import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke do tracing (Fase 9): a ponte Micrometer→OpenTelemetry está no contexto e produz um span
 * ativo com traceId de 128 bits (32 hex). Não depende de backend — o export fica desligado em test;
 * o que se valida aqui é a instrumentação/propagação de contexto.
 */
class TracingSmokeIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private Tracer tracer;

    @Test
    void spanHasHexTraceId() {
        Span span = tracer.nextSpan().name("smoke");
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            String traceId = tracer.currentSpan().context().traceId();
            assertThat(traceId).matches("[0-9a-f]{32}");
        } finally {
            span.end();
        }
    }
}