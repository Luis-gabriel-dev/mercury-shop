package dev.adastratech.mercuryshop.user.adapter.out.persistence;

import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;
import dev.adastratech.mercuryshop.user.domain.User;
import dev.adastratech.mercuryshop.user.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
class UserPersistenceAdapter implements UserRepository {

    private static final Set<String> SORTABLE = Set.of("email", "createdAt", "status");
    private static final String DEFAULT_SORT = "createdAt";

    private final UserJpaRepository repository;

    UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = repository.findById(user.getId())
                .map(existing -> {
                    apply(user, existing);
                    return existing;
                })
                .orElseGet(() -> toNewEntity(user));
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public PageResult<User> findAll(PageQuery page) {
        Page<UserJpaEntity> result = repository.findAll(
                PageRequest.of(page.page(), page.size(), toSort(page)));
        return new PageResult<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private Sort toSort(PageQuery page) {
        String property = (page.sortBy() != null && SORTABLE.contains(page.sortBy())) ? page.sortBy() : DEFAULT_SORT;
        Sort.Direction direction = page.direction() == PageQuery.Direction.DESC
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private UserJpaEntity toNewEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        apply(user, entity);
        return entity;
    }

    private void apply(User user, UserJpaEntity entity) {
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFullName(user.getFullName());
        entity.setPhone(user.getPhone());
        entity.setStatus(user.getStatus());
        entity.setEmailVerified(user.isEmailVerified());
        Set<dev.adastratech.mercuryshop.user.domain.Role> roles = user.getRoles();
        entity.setRoles(roles.isEmpty()
                ? EnumSet.noneOf(dev.adastratech.mercuryshop.user.domain.Role.class)
                : EnumSet.copyOf(roles));
    }

    private User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(), entity.getEmail(), entity.getPasswordHash(), entity.getFullName(),
                entity.getPhone(), entity.getStatus(), entity.isEmailVerified(), entity.getRoles(),
                entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
