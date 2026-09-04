package it.lsimpresa.manager.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderJpa, Long> {
    @EntityGraph(attributePaths = {"details", "details.article"})
    Optional<OrderJpa> findDetailedById(Long id);
}
