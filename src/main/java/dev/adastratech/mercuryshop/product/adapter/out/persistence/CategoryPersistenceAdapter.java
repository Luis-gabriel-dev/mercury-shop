package dev.adastratech.mercuryshop.product.adapter.out.persistence;

import dev.adastratech.mercuryshop.product.domain.Category;
import dev.adastratech.mercuryshop.product.domain.CategoryRepository;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Adapter de saída que implementa a porta {@link CategoryRepository} sobre Spring Data JPA. */
@Component
class CategoryPersistenceAdapter implements CategoryRepository {

    private static final Set<String> SORTABLE = Set.of("name", "createdAt");
    private static final String DEFAULT_SORT = "createdAt";

    private final CategoryJpaRepository repository;

    CategoryPersistenceAdapter(CategoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = repository.findById(category.getId())
                .map(existing -> {
                    apply(category, existing);
                    return existing;
                })
                .orElseGet(() -> toNewEntity(category));
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public PageResult<Category> findAll(PageQuery page) {
        Page<CategoryJpaEntity> result = repository.findAll(
                PageRequest.of(page.page(), page.size(), toSort(page)));
        return new PageResult<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private Sort toSort(PageQuery page) {
        String property = (page.sortBy() != null && SORTABLE.contains(page.sortBy()))
                ? page.sortBy() : DEFAULT_SORT;
        Sort.Direction direction = page.direction() == PageQuery.Direction.DESC
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private CategoryJpaEntity toNewEntity(Category category) {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(category.getId());
        apply(category, entity);
        return entity;
    }

    private void apply(Category category, CategoryJpaEntity entity) {
        entity.setName(category.getName());
        entity.setDescription(category.getDescription());
    }

    private Category toDomain(CategoryJpaEntity entity) {
        return Category.reconstitute(
                entity.getId(), entity.getName(), entity.getDescription(),
                entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
