package it.lsimpresa.manager.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderJpa, Long> {
    @EntityGraph(attributePaths = {"workOrder", "details", "details.article"})
    Optional<OrderJpa> findDetailedById(Long id);

    boolean existsByWorkOrderId(Long workOrderId);

    @Modifying(flushAutomatically = true)
    @Query("update OrderJpa o set o.name = :name, o.search = :name where o.workOrder.id = :workOrderId")
    int synchronizeWorkOrderName(@Param("workOrderId") Long workOrderId, @Param("name") String name);
}
