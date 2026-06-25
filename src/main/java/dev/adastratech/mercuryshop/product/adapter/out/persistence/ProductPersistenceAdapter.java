package dev.adastratech.mercuryshop.product.adapter.out.persistence;

import dev.adastratech.mercuryshop.product.domain.Product;
import dev.adastratech.mercuryshop.product.domain.ProductFilter;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Adapter de saída que implementa a porta {@link ProductRepository} sobre Spring Data JPA. */
@Component
class ProductPersistenceAdapter implements ProductRepository {

    private static final Set<String> SORTABLE = Set.of("name", "price", "stockQuantity", "createdAt");
    private static final String DEFAULT_SORT = "createdAt";

    private final ProductJpaRepository repository;

    ProductPersistenceAdapter(ProductJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = repository.findById(product.getId())
                .map(existing -> {
                    apply(product, existing);
                    return existing;
                })
                .orElseGet(() -> toNewEntity(product));
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public PageResult<Product> findAll(ProductFilter filter, PageQuery page) {
        Page<ProductJpaEntity> result = repository.findAll(
                buildSpecification(filter),
                PageRequest.of(page.page(), page.size(), toSort(page)));
        return new PageResult<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public PageResult<Product> search(String text, PageQuery page) {
        Page<ProductJpaEntity> result = repository.search(text, PageRequest.of(page.page(), page.size()));
        return new PageResult<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private Specification<ProductJpaEntity> buildSpecification(ProductFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.hasName()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + filter.name().toLowerCase() + "%"));
            }
            if (filter.hasCategory()) {
                predicates.add(cb.equal(root.get("categoryId"), filter.categoryId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort toSort(PageQuery page) {
        String property = (page.sortBy() != null && SORTABLE.contains(page.sortBy()))
                ? page.sortBy() : DEFAULT_SORT;
        Sort.Direction direction = page.direction() == PageQuery.Direction.DESC
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private ProductJpaEntity toNewEntity(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(product.getId());
        apply(product, entity);
        return entity;
    }

    private void apply(Product product, ProductJpaEntity entity) {
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setPrice(product.getPrice());
        entity.setStockQuantity(product.getStockQuantity());
        entity.setCategoryId(product.getCategoryId());
        entity.setActive(product.isActive());
    }

    private Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(
                entity.getId(), entity.getName(), entity.getDescription(), entity.getPrice(),
                entity.getStockQuantity(), entity.getCategoryId(), entity.isActive(),
                entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
