package dev.adastratech.mercuryshop.order.adapter.out.persistence;

import dev.adastratech.mercuryshop.order.domain.Order;
import dev.adastratech.mercuryshop.order.domain.OrderItem;
import dev.adastratech.mercuryshop.order.domain.OrderRepository;
import dev.adastratech.mercuryshop.order.domain.OrderStatus;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
class OrderPersistenceAdapter implements OrderRepository {

    private static final Set<String> SORTABLE = Set.of("createdAt", "total", "status");
    private static final String DEFAULT_SORT = "createdAt";

    private final OrderJpaRepository repository;

    OrderPersistenceAdapter(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = repository.findById(order.getId())
                .map(existing -> {
                    existing.setStatus(order.getStatus());
                    return existing;
                })
                .orElseGet(() -> toNewEntity(order));
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public PageResult<Order> findByUserId(UUID userId, PageQuery page) {
        return toPageResult(repository.findByUserId(userId, pageRequest(page)));
    }

    @Override
    public PageResult<Order> findAll(PageQuery page) {
        return toPageResult(repository.findAll(pageRequest(page)));
    }

    @Override
    public List<UUID> findPendingIdsCreatedBefore(Instant cutoff, int limit) {
        return repository.findIdsByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, cutoff, PageRequest.of(0, limit));
    }

    private PageResult<Order> toPageResult(Page<OrderJpaEntity> result) {
        return new PageResult<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private PageRequest pageRequest(PageQuery page) {
        String property = (page.sortBy() != null && SORTABLE.contains(page.sortBy())) ? page.sortBy() : DEFAULT_SORT;
        Sort.Direction direction = page.direction() == PageQuery.Direction.DESC
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page.page(), page.size(), Sort.by(direction, property));
    }

    private OrderJpaEntity toNewEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getId());
        entity.setUserId(order.getUserId());
        entity.setStatus(order.getStatus());
        entity.setTotal(order.getTotal());
        entity.setIdempotencyKey(order.getIdempotencyKey());
        for (OrderItem item : order.getItems()) {
            OrderItemJpaEntity itemEntity = new OrderItemJpaEntity();
            itemEntity.setId(UUID.randomUUID());
            itemEntity.setProductId(item.productId());
            itemEntity.setProductName(item.productName());
            itemEntity.setUnitPrice(item.unitPrice());
            itemEntity.setQuantity(item.quantity());
            itemEntity.setLineTotal(item.lineTotal());
            entity.addItem(itemEntity);
        }
        return entity;
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(i -> new OrderItem(i.getProductId(), i.getProductName(), i.getUnitPrice(),
                        i.getQuantity(), i.getLineTotal()))
                .toList();
        return Order.reconstitute(
                entity.getId(), entity.getUserId(), entity.getStatus(), items, entity.getTotal(),
                entity.getIdempotencyKey(), entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
