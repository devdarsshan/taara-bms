package com.taara.bms.repo.printing;

import com.taara.bms.entity.printing.PrintingDelivery;
import com.taara.bms.enums.GarmentSize;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrintingDeliveryRepository extends JpaRepository<PrintingDelivery, UUID>, JpaSpecificationExecutor<PrintingDelivery> {

    Optional<PrintingDelivery> findByAutoIdIgnoreCase(String autoId);

    @Query("""
            select coalesce(sum(d.piecesDelivered), 0)
            from PrintingDelivery d
            where d.isDeleted = false
              and d.printingOrder.id = :orderId
            """)
    Integer sumActiveDeliveredByOrder(@Param("orderId") UUID orderId);

    @Query("""
            select coalesce(sum(d.piecesDelivered), 0)
            from PrintingDelivery d
            where d.isDeleted = false
              and d.printingOrder.style.id = :styleId
              and d.printingOrder.size = :size
            """)
    Integer sumActiveDeliveredByStyleAndSize(@Param("styleId") UUID styleId, @Param("size") GarmentSize size);
}
