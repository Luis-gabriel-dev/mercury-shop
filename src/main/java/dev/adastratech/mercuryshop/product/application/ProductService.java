package dev.adastratech.mercuryshop.product.application;

import dev.adastratech.mercuryshop.product.application.command.CreateProductCommand;
import dev.adastratech.mercuryshop.product.application.command.UpdateProductCommand;
import dev.adastratech.mercuryshop.product.domain.CategoryRepository;
import dev.adastratech.mercuryshop.product.domain.Product;
import dev.adastratech.mercuryshop.product.domain.ProductFilter;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;
import dev.adastratech.mercuryshop.shared.exception.NotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de Produto. Depende das portas {@link ProductRepository} e {@link CategoryRepository}. */
@Service
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional
    public Product create(CreateProductCommand command) {
        requireCategoryExists(command.categoryId());
        Product product = Product.create(
                command.name(), command.description(), command.price(),
                command.stockQuantity(), command.categoryId());
        return products.save(product);
    }

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Product get(UUID id) {
        return products.findById(id)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));
    }

    @Transactional(readOnly = true)
    public PageResult<Product> list(ProductFilter filter, PageQuery page) {
        return products.findAll(filter, page);
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public Product update(UUID id, UpdateProductCommand command) {
        Product product = products.findById(id)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));
        if (command.name() != null) {
            product.rename(command.name());
        }
        if (command.description() != null) {
            product.changeDescription(command.description());
        }
        if (command.price() != null) {
            product.changePrice(command.price());
        }
        if (command.stockQuantity() != null) {
            product.changeStockQuantity(command.stockQuantity());
        }
        if (command.categoryId() != null) {
            requireCategoryExists(command.categoryId());
            product.changeCategory(command.categoryId());
        }
        if (command.active() != null) {
            if (command.active()) {
                product.activate();
            } else {
                product.deactivate();
            }
        }
        return products.save(product);
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void delete(UUID id) {
        if (!products.existsById(id)) {
            throw new NotFoundException("Produto não encontrado");
        }
        products.deleteById(id);
    }

    private void requireCategoryExists(UUID categoryId) {
        if (categoryId != null && !categories.existsById(categoryId)) {
            throw new NotFoundException("Categoria não encontrada");
        }
    }
}
