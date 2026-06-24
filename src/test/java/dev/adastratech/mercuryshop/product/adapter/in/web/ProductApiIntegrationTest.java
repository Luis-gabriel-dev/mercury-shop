package dev.adastratech.mercuryshop.product.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catálogo ponta a ponta com segurança ligada: leitura pública, escrita só ADMIN.
 */
class ProductApiIntegrationTest extends IntegrationTestSupport {

    private static SimpleGrantedAuthority admin() {
        return new SimpleGrantedAuthority("ROLE_ADMIN");
    }

    @Test
    void adminCreatesAndAnyoneFetchesProduct() throws Exception {
        MvcResult categoryResult = mockMvc.perform(post("/v1/categories")
                        .with(jwt().authorities(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Periféricos %s", "description": "Acessórios"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        String categoryId = JsonPath.read(categoryResult.getResponse().getContentAsString(), "$.id");

        MvcResult productResult = mockMvc.perform(post("/v1/products")
                        .with(jwt().authorities(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mouse sem fio",
                                  "price": 99.90,
                                  "stockQuantity": 25,
                                  "categoryId": "%s"
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mouse sem fio"))
                .andExpect(jsonPath("$.version").doesNotExist())
                .andReturn();
        String productId = JsonPath.read(productResult.getResponse().getContentAsString(), "$.id");

        // Leitura é pública (sem token).
        mockMvc.perform(get("/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(25));
    }

    @Test
    void rejectsWriteWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X", "price": 1.00, "stockQuantity": 1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWriteWithInsufficientRole() throws Exception {
        mockMvc.perform(post("/v1/categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Proibida"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void returns404ForUnknownProduct() throws Exception {
        mockMvc.perform(get("/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
