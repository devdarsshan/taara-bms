package com.taara.bms.repo.spinning;

import com.taara.bms.entity.spinning.SpinningDelivery;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpinningDeliveryRepository extends JpaRepository<SpinningDelivery, UUID>, JpaSpecificationExecutor<SpinningDelivery> {

    Optional<SpinningDelivery> findByAutoIdIgnoreCase(String autoId);

    boolean existsByStyle_IdAndIsDeletedFalse(UUID styleId);

    @Query("""
            select coalesce(sum(s.finalQuantityKgs), 0)
            from SpinningDelivery s
            where s.isDeleted = false
              and (:styleId is null or s.style.id = :styleId)
            """)
    BigDecimal sumActiveFinalQuantityByStyle(@Param("styleId") UUID styleId);
}
