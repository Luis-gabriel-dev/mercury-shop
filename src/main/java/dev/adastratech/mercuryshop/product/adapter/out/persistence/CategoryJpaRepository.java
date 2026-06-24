package dev.adastratech.mercuryshop.product.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    boolean existsByName(String name);
}
