package com.taara.bms.repo.yarn;

import com.taara.bms.entity.yarn.YarnOrder;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface YarnOrderRepository extends JpaRepository<YarnOrder, UUID>, JpaSpecificationExecutor<YarnOrder> {

    Optional<YarnOrder> findByAutoIdIgnoreCase(String autoId);

    boolean existsByStyle_IdAndIsDeletedFalse(UUID styleId);

    @Query("""
            select coalesce(sum(y.quantityKgs), 0)
            from YarnOrder y
            where y.isDeleted = false
              and (:styleId is null or y.style.id = :styleId)
            """)
    BigDecimal sumActiveQuantityByStyle(@Param("styleId") UUID styleId);
}
