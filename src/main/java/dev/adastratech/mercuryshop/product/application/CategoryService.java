package dev.adastratech.mercuryshop.product.application;

import dev.adastratech.mercuryshop.product.application.command.CreateCategoryCommand;
import dev.adastratech.mercuryshop.product.application.command.UpdateCategoryCommand;
import dev.adastratech.mercuryshop.product.domain.Category;
import dev.adastratech.mercuryshop.product.domain.CategoryRepository;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;
import dev.adastratech.mercuryshop.shared.exception.ConflictException;
import dev.adastratech.mercuryshop.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Casos de uso de Categoria. Depende apenas da porta {@link CategoryRepository}. */
@Service
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional
    public Category create(CreateCategoryCommand command) {
        Category category = Category.create(command.name(), command.description());
        if (categories.existsByName(category.getName())) {
            throw new ConflictException("Já existe uma categoria com esse nome");
        }
        return categories.save(category);
    }

    @Transactional(readOnly = true)
    public Category get(UUID id) {
        return categories.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
    }

    @Transactional(readOnly = true)
    public PageResult<Category> list(PageQuery page) {
        return categories.findAll(page);
    }

    @Transactional
    public Category update(UUID id, UpdateCategoryCommand command) {
        Category category = get(id);
        if (command.name() != null) {
            String newName = command.name().trim();
            if (!category.getName().equalsIgnoreCase(newName) && categories.existsByName(newName)) {
                throw new ConflictException("Já existe uma categoria com esse nome");
            }
            category.rename(newName);
        }
        if (command.description() != null) {
            category.changeDescription(command.description());
        }
        return categories.save(category);
    }

    @Transactional
    public void delete(UUID id) {
        if (!categories.existsById(id)) {
            throw new NotFoundException("Categoria não encontrada");
        }
        categories.deleteById(id);
    }
}
