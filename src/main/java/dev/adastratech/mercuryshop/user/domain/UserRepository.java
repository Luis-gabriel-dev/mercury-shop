package dev.adastratech.mercuryshop.user.domain;

import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.application.PageResult;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída para persistência de usuários. */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    PageResult<User> findAll(PageQuery page);
}
