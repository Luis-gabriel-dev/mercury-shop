package dev.adastratech.mercuryshop.product.application;

import dev.adastratech.mercuryshop.product.application.command.UpdateProductCommand;
import dev.adastratech.mercuryshop.product.domain.Product;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** O catálogo é cacheado no Redis: get serve do cache; edição via service faz evict. */
class CacheIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository products;

    @Test
    void getIsCachedAndEvictedOnUpdate() {
        Product saved = products.save(
                Product.create("Cacheável", null, new BigDecimal("10.00"), 100, null));
        UUID id = saved.getId();

        // Popula o cache.
        assertThat(productService.get(id).getPrice()).isEqualByComparingTo("10.00");

        // Muda direto no repositório (sem passar pelo service → NÃO faz evict).
        Product fromDb = products.findById(id).orElseThrow();
        fromDb.changePrice(new BigDecimal("99.00"));
        products.save(fromDb);

        // Ainda retorna o valor cacheado (prova o cache).
        assertThat(productService.get(id).getPrice()).isEqualByComparingTo("10.00");

        // Edição via service faz evict → próxima leitura reflete o novo valor.
        productService.update(id, new UpdateProductCommand(null, null, new BigDecimal("50.00"), null, null, null));
        assertThat(productService.get(id).getPrice()).isEqualByComparingTo("50.00");
    }
}
