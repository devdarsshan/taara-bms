package com.taara.bms.repo.printing;

import com.taara.bms.entity.printing.PrintingOrder;
import com.taara.bms.enums.GarmentSize;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrintingOrderRepository extends JpaRepository<PrintingOrder, UUID>, JpaSpecificationExecutor<PrintingOrder> {

    Optional<PrintingOrder> findByAutoIdIgnoreCase(String autoId);

    boolean existsByStyle_IdAndIsDeletedFalse(UUID styleId);

    boolean existsByPrintingSection_IdAndIsDeletedFalse(UUID sectionId);

    @Query("""
            select coalesce(sum(o.piecesOrdered), 0)
            from PrintingOrder o
            where o.isDeleted = false
              and o.style.id = :styleId
              and o.size = :size
            """)
    Integer sumActiveOrderedByStyleAndSize(@Param("styleId") UUID styleId, @Param("size") GarmentSize size);
}
