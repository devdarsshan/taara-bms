package com.taara.bms.repo.inhouse;

import com.taara.bms.entity.inhouse.InHouseStockSplit;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InHouseStockSplitRepository extends JpaRepository<InHouseStockSplit, UUID>, JpaSpecificationExecutor<InHouseStockSplit> {

    Optional<InHouseStockSplit> findByAutoIdIgnoreCase(String autoId);

    boolean existsByStyle_IdAndIsDeletedFalse(UUID styleId);

    boolean existsByDia_IdAndIsDeletedFalse(UUID diaId);

    List<InHouseStockSplit> findByDelivery_IdAndIsDeletedFalse(UUID deliveryId);

    @Query("""
            select coalesce(sum(s.quantityKgs), 0)
            from InHouseStockSplit s
            where s.isDeleted = false
              and s.delivery.id = :deliveryId
            """)
    BigDecimal sumActiveQuantityByDelivery(@Param("deliveryId") UUID deliveryId);

    @Query("""
            select coalesce(sum(s.quantityKgs), 0)
            from InHouseStockSplit s
            where s.isDeleted = false
              and s.dia.id = :diaId
              and s.style.id = :styleId
            """)
    BigDecimal sumActiveQuantityByDiaAndStyle(@Param("diaId") UUID diaId, @Param("styleId") UUID styleId);
}
