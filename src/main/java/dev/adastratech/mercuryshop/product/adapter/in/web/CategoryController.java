package dev.adastratech.mercuryshop.product.adapter.in.web;

import dev.adastratech.mercuryshop.product.adapter.in.web.dto.CategoryResponse;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.CreateCategoryRequest;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.PageResponse;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.UpdateCategoryRequest;
import dev.adastratech.mercuryshop.product.application.CategoryService;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/categories")
class CategoryController {

    private final CategoryService service;

    CategoryController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return WebMapper.toResponse(service.create(WebMapper.toCommand(request)));
    }

    @GetMapping("/{id}")
    CategoryResponse get(@PathVariable UUID id) {
        return WebMapper.toResponse(service.get(id));
    }

    @GetMapping
    PageResponse<CategoryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "ASC") PageQuery.Direction direction) {
        PageQuery query = new PageQuery(page, size, sort, direction);
        return PageResponse.from(service.list(query).map(WebMapper::toResponse));
    }

    @PatchMapping("/{id}")
    CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return WebMapper.toResponse(service.update(id, WebMapper.toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
