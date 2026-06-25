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

/** Pagamento (PENDING→PAID) e o pipeline assíncrono OrderPaid → fatura gerada por worker. */
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

    @Test
    void payMarksOrderPaidAndGeneratesInvoiceAsync() throws Exception {
        String productId = createProduct(5, "30.00");
        UUID user = UUID.randomUUID();
        String orderId = checkout(user, productId);

        mockMvc.perform(post("/v1/orders/{id}/pay", orderId).with(customer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.orderId").value(orderId));

        mockMvc.perform(get("/v1/orders/{id}", orderId).with(customer(user)))
                .andExpect(jsonPath("$.status").value("PAID"));

        // O worker gera a fatura de forma assíncrona ao consumir OrderPaid.
        UUID orderUuid = UUID.fromString(orderId);
        await().atMost(Duration.ofSeconds(15))
                .until(() -> invoices.findByOrderId(orderUuid).isPresent());
        assertThat(invoices.findByOrderId(orderUuid).orElseThrow().getNumber()).startsWith("INV-");
    }

    @Test
    void cannotPayAnAlreadyPaidOrder() throws Exception {
        String productId = createProduct(5, "30.00");
        UUID user = UUID.randomUUID();
        String orderId = checkout(user, productId);

        mockMvc.perform(post("/v1/orders/{id}/pay", orderId).with(customer(user)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/orders/{id}/pay", orderId).with(customer(user)))
                .andExpect(status().isConflict());
    }
}
