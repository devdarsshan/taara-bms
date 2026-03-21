package com.taara.bms.repo.stitching;

import com.taara.bms.entity.stitching.StitchingDelivery;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StitchingDeliveryRepository extends JpaRepository<StitchingDelivery, UUID>, JpaSpecificationExecutor<StitchingDelivery> {

    Optional<StitchingDelivery> findByAutoIdIgnoreCase(String autoId);

    boolean existsByStitchingOrder_IdAndIsDeletedFalse(UUID orderId);

    @Query("""
            select coalesce(sum(s.piecesDelivered), 0)
            from StitchingDelivery s
            where s.isDeleted = false
              and s.stitchingOrder.id = :orderId
            """)
    Integer sumActivePiecesByOrder(@Param("orderId") UUID orderId);
}
