package it.lsimpresa.manager.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.Collection;
import it.lsimpresa.manager.user.UserJpa;

public interface OrderRepository extends JpaRepository<OrderJpa, Long> {
    @EntityGraph(attributePaths = {"workOrder", "assignedWarehouse", "details", "details.article"})
    Optional<OrderJpa> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"workOrder", "assignedWarehouse", "details", "details.article"})
    @Query("select o from OrderJpa o where o.id = :id")
    Optional<OrderJpa> findDetailedByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OrderJpa o set o.assignedWarehouse = :warehouse, " +
            "o.takenInChargeAt = coalesce(o.takenInChargeAt, :takenInChargeAt) " +
            "where o.id = :id and o.assignedWarehouse is null and o.status in :statuses")
    int claimIfAvailable(@Param("id") Long id, @Param("warehouse") UserJpa warehouse,
                         @Param("statuses") Collection<OrderStatus> statuses,
                         @Param("takenInChargeAt") Instant takenInChargeAt);

    boolean existsByWorkOrderId(Long workOrderId);

    @Modifying(flushAutomatically = true)
    @Query("update OrderJpa o set o.name = :name, o.search = :name where o.workOrder.id = :workOrderId")
    int synchronizeWorkOrderName(@Param("workOrderId") Long workOrderId, @Param("name") String name);
}
