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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fulfillment do pedido: ship/deliver (ADMIN/STAFF), RBAC e transições de estado inválidas. */
class OrderFulfillmentIntegrationTest extends IntegrationTestSupport {

    private RequestPostProcessor withRole(String role) {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private RequestPostProcessor customer(UUID userId) {
        return jwt().jwt(j -> j.subject(userId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private String createProduct() throws Exception {
        MvcResult category = mockMvc.perform(post("/v1/categories").with(withRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cat %s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isCreated()).andReturn();
        String categoryId = JsonPath.read(category.getResponse().getContentAsString(), "$.id");
        MvcResult product = mockMvc.perform(post("/v1/products").with(withRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Produto", "price": 30.00, "stockQuantity": 5, "categoryId": "%s"}
                                """.formatted(categoryId)))
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

    private void pay(UUID user, String orderId) throws Exception {
        mockMvc.perform(post("/v1/orders/{id}/pay", orderId).with(customer(user)))
                .andExpect(status().isOk());
    }

    @Test
    void staffShipsAndDeliversAPaidOrder() throws Exception {
        UUID user = UUID.randomUUID();
        String orderId = checkout(user, createProduct());
        pay(user, orderId);

        mockMvc.perform(post("/v1/admin/orders/{id}/ship", orderId).with(withRole("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        mockMvc.perform(post("/v1/admin/orders/{id}/deliver", orderId).with(withRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void customerCannotShip() throws Exception {
        UUID user = UUID.randomUUID();
        String orderId = checkout(user, createProduct());
        pay(user, orderId);

        mockMvc.perform(post("/v1/admin/orders/{id}/ship", orderId).with(customer(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotShipAnOrderThatWasNotPaid() throws Exception {
        UUID user = UUID.randomUUID();
        String orderId = checkout(user, createProduct()); // segue PENDING

        mockMvc.perform(post("/v1/admin/orders/{id}/ship", orderId).with(withRole("ADMIN")))
                .andExpect(status().isConflict());
    }
}
