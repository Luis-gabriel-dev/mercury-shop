package dev.adastratech.mercuryshop.product.adapter.in.web;

import dev.adastratech.mercuryshop.product.adapter.in.web.dto.CreateProductRequest;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.PageResponse;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.ProductResponse;
import dev.adastratech.mercuryshop.product.adapter.in.web.dto.UpdateProductRequest;
import dev.adastratech.mercuryshop.product.application.ProductService;
import dev.adastratech.mercuryshop.product.domain.ProductFilter;
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
@RequestMapping("/v1/products")
class ProductController {

    private final ProductService service;

    ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return WebMapper.toResponse(service.create(WebMapper.toCommand(request)));
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable UUID id) {
        return WebMapper.toResponse(service.get(id));
    }

    @GetMapping
    PageResponse<ProductResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "ASC") PageQuery.Direction direction) {
        ProductFilter filter = new ProductFilter(name, categoryId);
        PageQuery query = new PageQuery(page, size, sort, direction);
        return PageResponse.from(service.list(filter, query).map(WebMapper::toResponse));
    }

    @PatchMapping("/{id}")
    ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return WebMapper.toResponse(service.update(id, WebMapper.toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
