package dev.adastratech.mercuryshop.order.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Carrinho → checkout ponta a ponta: baixa de estoque, idempotência, cancelamento e ownership. */
class CartAndOrderApiIntegrationTest extends IntegrationTestSupport {

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
                                {"name": "Limitado", "price": %s, "stockQuantity": %d, "categoryId": "%s"}
                                """.formatted(price, stock, categoryId)))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(product.getResponse().getContentAsString(), "$.id");
    }

    private void addToCart(UUID userId, String productId, int quantity) throws Exception {
        mockMvc.perform(post("/v1/cart/items").with(customer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": "%s", "quantity": %d}
                                """.formatted(productId, quantity)))
                .andExpect(status().isOk());
    }

    private MvcResult checkout(UUID userId, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/v1/orders").with(customer(userId))
                        .header("Idempotency-Key", idempotencyKey))
                .andReturn();
    }

    @Test
    void checkoutDecrementsStockAndClearsCart() throws Exception {
        String productId = createProduct(5, "10.00");
        UUID user = UUID.randomUUID();
        addToCart(user, productId, 2);

        checkout(user, "key-" + UUID.randomUUID());

        mockMvc.perform(get("/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(3));
        mockMvc.perform(get("/v1/cart").with(customer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void checkoutIsIdempotentForSameKey() throws Exception {
        String productId = createProduct(5, "10.00");
        UUID user = UUID.randomUUID();
        addToCart(user, productId, 2);
        String key = "key-" + UUID.randomUUID();

        MvcResult first = checkout(user, key);
        String firstId = JsonPath.read(first.getResponse().getContentAsString(), "$.id");

        MvcResult second = checkout(user, key);
        String secondId = JsonPath.read(second.getResponse().getContentAsString(), "$.id");

        org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);
        // estoque baixado apenas uma vez
        mockMvc.perform(get("/v1/products/{id}", productId))
                .andExpect(jsonPath("$.stockQuantity").value(3));
    }

    @Test
    void checkoutWithInsufficientStockReturns409() throws Exception {
        String productId = createProduct(1, "10.00");
        UUID user = UUID.randomUUID();
        addToCart(user, productId, 2);

        mockMvc.perform(post("/v1/orders").with(customer(user)).header("Idempotency-Key", "k-" + UUID.randomUUID()))
                .andExpect(status().isConflict());
    }

    @Test
    void emptyCartCheckoutReturns422() throws Exception {
        mockMvc.perform(post("/v1/orders").with(customer(UUID.randomUUID()))
                        .header("Idempotency-Key", "k-" + UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("EMPTY_CART"));
    }

    @Test
    void cancelRestoresStock() throws Exception {
        String productId = createProduct(5, "10.00");
        UUID user = UUID.randomUUID();
        addToCart(user, productId, 2);
        String orderId = JsonPath.read(checkout(user, "k-" + UUID.randomUUID()).getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/v1/orders/{id}/cancel", orderId).with(customer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/v1/products/{id}", productId))
                .andExpect(jsonPath("$.stockQuantity").value(5));
    }

    @Test
    void otherUserCannotSeeOrder() throws Exception {
        String productId = createProduct(5, "10.00");
        UUID owner = UUID.randomUUID();
        addToCart(owner, productId, 1);
        String orderId = JsonPath.read(checkout(owner, "k-" + UUID.randomUUID()).getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/v1/orders/{id}", orderId).with(customer(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }
}
