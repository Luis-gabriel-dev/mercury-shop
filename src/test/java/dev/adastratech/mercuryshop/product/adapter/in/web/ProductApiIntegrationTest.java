package dev.adastratech.mercuryshop.product.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração ponta a ponta com Postgres real (Testcontainers): roda Flyway,
 * cria categoria + produto e os lê de volta. Verifica também 404 e ausência de campo interno.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ProductApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndFetchesProduct() throws Exception {
        MvcResult categoryResult = mockMvc.perform(post("/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Periféricos", "description": "Acessórios de computador"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        String categoryId = JsonPath.read(categoryResult.getResponse().getContentAsString(), "$.id");

        MvcResult productResult = mockMvc.perform(post("/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mouse sem fio",
                                  "description": "2.4GHz",
                                  "price": 99.90,
                                  "stockQuantity": 25,
                                  "categoryId": "%s"
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mouse sem fio"))
                .andExpect(jsonPath("$.active").value(true))
                // campo interno não deve ser exposto (briefing 7.3)
                .andExpect(jsonPath("$.version").doesNotExist())
                .andReturn();
        String productId = JsonPath.read(productResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.stockQuantity").value(25))
                .andExpect(jsonPath("$.categoryId").value(categoryId));

        mockMvc.perform(get("/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(productId));
    }

    @Test
    void returns404ForUnknownProduct() throws Exception {
        mockMvc.perform(get("/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.requestId").exists());
    }
}
