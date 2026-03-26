package com.taara.bms.repo.stitching;

import com.taara.bms.entity.stitching.StitchingDeliveryAllocation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StitchingDeliveryAllocationRepository extends JpaRepository<StitchingDeliveryAllocation, UUID> {

    @Query("""
            select coalesce(sum(a.allocatedPieces), 0)
            from StitchingDeliveryAllocation a
            where a.stitchingOrder.id = :orderId
              and a.stitchingDelivery.isDeleted = false
            """)
    Integer sumActiveAllocatedByOrder(@Param("orderId") UUID orderId);
}
