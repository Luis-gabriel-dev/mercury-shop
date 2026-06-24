package dev.adastratech.mercuryshop.product.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID>,
        JpaSpecificationExecutor<ProductJpaEntity> {
}
