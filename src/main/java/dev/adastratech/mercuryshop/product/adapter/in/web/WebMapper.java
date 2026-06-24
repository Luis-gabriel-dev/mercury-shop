package dev.adastratech.mercuryshop.product.adapter.in.web;

import dev.adastratech.mercuryshop.product.adapter.in.web.dto.CategoryResponse;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.CreateCategoryRequest;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.CreateProductRequest;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.ProductResponse;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.UpdateCategoryRequest;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.UpdateProductRequest;
import dev.adastratech.mercuryshop.product.application.command.CreateCategoryCommand;
import dev.adastratech.mercuryshop.product.application.command.CreateProductCommand;
import dev.adastratech.mercuryshop.product.application.command.UpdateCategoryCommand;
import dev.adastratech.mercuryshop.product.application.command.UpdateProductCommand;
import dev.adastratech.mercuryshop.product.domain.Category;
import dev.adastratech.mercuryshop.product.domain.Product;

/** Conversões entre DTOs da borda HTTP e os tipos da aplicação/domínio. */
final class WebMapper {

    private WebMapper() {
    }

    static CreateCategoryCommand toCommand(CreateCategoryRequest request) {
        return new CreateCategoryCommand(request.name(), request.description());
    }

    static UpdateCategoryCommand toCommand(UpdateCategoryRequest request) {
        return new UpdateCategoryCommand(request.name(), request.description());
    }

    static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(), category.getName(), category.getDescription(), category.getCreatedAt());
    }

    static CreateProductCommand toCommand(CreateProductRequest request) {
        return new CreateProductCommand(
                request.name(), request.description(), request.price(),
                request.stockQuantity(), request.categoryId());
    }

    static UpdateProductCommand toCommand(UpdateProductRequest request) {
        return new UpdateProductCommand(
                request.name(), request.description(), request.price(),
                request.stockQuantity(), request.categoryId(), request.active());
    }

    static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getStockQuantity(), product.getCategoryId(), product.isActive(), product.getCreatedAt());
    }
}
