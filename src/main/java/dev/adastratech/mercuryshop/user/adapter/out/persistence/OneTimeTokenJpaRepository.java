package dev.adastratech.mercuryshop.user.adapter.out.persistence;

import dev.adastratech.mercuryshop.user.domain.TokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface OneTimeTokenJpaRepository extends JpaRepository<OneTimeTokenJpaEntity, UUID> {

    Optional<OneTimeTokenJpaEntity> findByTokenHash(String tokenHash);

    void deleteByUserIdAndPurpose(UUID userId, TokenPurpose purpose);
}
