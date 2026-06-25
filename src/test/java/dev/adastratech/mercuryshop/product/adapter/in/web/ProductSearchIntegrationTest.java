package dev.adastratech.mercuryshop.product.adapter.in.web;

import dev.adastratech.mercuryshop.product.domain.Product;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Busca full-text do catálogo (Postgres FTS via coluna gerada + GIN). Termos únicos evitam colisão
 * com produtos criados por outros testes no banco compartilhado.
 */
class ProductSearchIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProductRepository products;

    private Product product(String name, String description) {
        return Product.create(name, description, new BigDecimal("100.00"), 10, null);
    }

    @Test
    void fullTextSearchMatchesNameAndDescription() throws Exception {
        String tag = "zorptronic"; // termo único
        products.save(product("Teclado " + tag, "switch azul"));        // casa no NOME
        products.save(product("Mouse comum", "sem o termo buscado"));   // não casa
        products.save(product("Cadeira", "modelo " + tag + " confortável")); // casa na DESCRIÇÃO

        mockMvc.perform(get("/v1/products").param("q", tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void fullTextSearchAppliesPortugueseStemming() throws Exception {
        products.save(product("Garrafas térmicas zqubix", "para viagem"));

        // busca no singular casa a forma plural do produto (stemming 'portuguese': térmicas → térmica)
        mockMvc.perform(get("/v1/products").param("q", "térmica zqubix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
