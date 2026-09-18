package it.lsimpresa.manager.article;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<ArticleJpa, Long> {
    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    Optional<ArticleJpa> findBySkuIgnoreCase(String sku);
}
