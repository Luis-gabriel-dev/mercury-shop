package dev.adastratech.mercuryshop.payment.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import dev.adastratech.mercuryshop.invoice.domain.InvoiceRepository;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pagamento via gateway (stub): iniciar (PENDING + client secret) → webhook de sucesso marca PAID →
 * pipeline assíncrono OrderPaid (via outbox) gera a fatura. Cobre também idempotência do webhook.
 */
class PaymentAndInvoiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private InvoiceRepository invoices;

    private RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private RequestPostProcessor customer(UUID userId) {
        return jwt().jwt(j -> j.subject(userId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private String createProduct(int stock, String price) throws Exception {
        MvcResult category = mockMvc.perform(post("/v1/categories").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cat %s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isCreated()).andReturn();
        String categoryId = JsonPath.read(category.getResponse().getContentAsString(), "$.id");
        MvcResult product = mockMvc.perform(post("/v1/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Produto", "price": %s, "stockQuantity": %d, "categoryId": "%s"}
                                """.formatted(price, stock, categoryId)))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(product.getResponse().getContentAsString(), "$.id");
    }

    private String checkout(UUID user, String productId) throws Exception {
        mockMvc.perform(post("/v1/cart/items").with(customer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": \"%s\", \"quantity\": 1}".formatted(productId)))
                .andExpect(status().isOk());
        MvcResult order = mockMvc.perform(post("/v1/orders").with(customer(user))
                        .header("Idempotency-Key", "k-" + UUID.randomUUID()))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(order.getResponse().getContentAsString(), "$.id");
    }

    /** Evento de sucesso no formato do gateway stub (sem assinatura). */
    private void sendSuccessWebhook(String orderId) throws Exception {
        mockMvc.perform(post("/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"payment_succeeded\", \"orderId\": \"%s\"}".formatted(orderId)))
                .andExpect(status().isOk());
    }

    @Test
    void initiatePaymentThenWebhookMarksPaidAndGeneratesInvoice() throws Exception {
        String productId = createProduct(5, "30.00");
        UUID user = UUID.randomUUID();
        String orderId = checkout(user, productId);

        // 1) Iniciar: pedido segue PENDING e recebemos o client secret para o cliente concluir.
        mockMvc.perform(post("/v1/orders/{id}/pay", orderId).with(customer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.clientSecret").isNotEmpty());
        mockMvc.perform(get("/v1/orders/{id}", orderId).with(customer(user)))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 2) Webhook de sucesso → pedido PAID.
        sendSuccessWebhook(orderId);
        mockMvc.perform(get("/v1/orders/{id}", orderId).with(customer(user)))
                .andExpect(jsonPath("$.status").value("PAID"));

        // 3) Fatura gerada de forma assíncrona (OrderPaid via outbox → worker).
        UUID orderUuid = UUID.fromString(orderId);
        await().atMost(Duration.ofSeconds(15))
                .until(() -> invoices.findByOrderId(orderUuid).isPresent());
        assertThat(invoices.findByOrderId(orderUuid).orElseThrow().getNumber()).startsWith("INV-");
    }

    @Test
    void duplicateWebhookIsIdempotent() throws Exception {
        String productId = createProduct(5, "30.00");
        UUID user = UUID.randomUUID();
        String orderId = checkout(user, productId);
        mockMvc.perform(post("/v1/orders/{id}/pay", orderId).with(customer(user))).andExpect(status().isOk());

        // Mesmo evento entregue duas vezes → ambas 200; o pedido continua PAID (sem reprocessar).
        sendSuccessWebhook(orderId);
        sendSuccessWebhook(orderId);

        mockMvc.perform(get("/v1/orders/{id}", orderId).with(customer(user)))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void cannotInitiatePaymentOnAnAlreadyPaidOrder() throws Exception {
        String productId = createProduct(5, "30.00");
        UUID user = UUID.randomUUID();
        String orderId = checkout(user, productId);
        mockMvc.perform(post("/v1/orders/{id}/pay", orderId).with(customer(user))).andExpect(status().isOk());
        sendSuccessWebhook(orderId);

        mockMvc.perform(post("/v1/orders/{id}/pay", orderId).with(customer(user)))
                .andExpect(status().isConflict());
    }
}
