package com.taara.bms.repo.spinning;

import com.taara.bms.entity.spinning.SpinningOrder;
import com.taara.bms.entity.yarn.YarnOrder;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpinningOrderRepository extends JpaRepository<SpinningOrder, UUID>, JpaSpecificationExecutor<SpinningOrder> {

    Optional<SpinningOrder> findByAutoIdIgnoreCase(String autoId);

    Optional<SpinningOrder> findByLinkedYarnOrderAndIsDeletedFalse(YarnOrder yarnOrder);

    boolean existsByStyle_IdAndIsDeletedFalse(UUID styleId);

    @Query("""
            select coalesce(sum(s.quantitySentKgs), 0)
            from SpinningOrder s
            where s.isDeleted = false
              and (:styleId is null or s.style.id = :styleId)
              and (:sectionId is null or s.stitchingSection.id = :sectionId)
            """)
    BigDecimal sumActiveQuantity(@Param("styleId") UUID styleId, @Param("sectionId") UUID sectionId);
}
