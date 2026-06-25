package dev.adastratech.mercuryshop.product.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID>,
        JpaSpecificationExecutor<ProductJpaEntity> {

    /**
     * Busca full-text (Postgres) entre produtos ativos, ordenada por relevância (ts_rank).
     * Usa a coluna gerada {@code search_vector} (índice GIN) e {@code plainto_tsquery} para tolerar
     * entrada livre do usuário. A paginação vem do {@link Pageable} (sem sort: a ordem é o ranking).
     */
    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.active = true
              AND p.search_vector @@ plainto_tsquery('portuguese', :q)
            ORDER BY ts_rank(p.search_vector, plainto_tsquery('portuguese', :q)) DESC
            """,
            countQuery = """
            SELECT count(*) FROM products p
            WHERE p.active = true
              AND p.search_vector @@ plainto_tsquery('portuguese', :q)
            """,
            nativeQuery = true)
    Page<ProductJpaEntity> search(@Param("q") String q, Pageable pageable);
}
