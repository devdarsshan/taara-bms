package com.taara.bms.repo.stitching;

import com.taara.bms.entity.stitching.StitchingDelivery;
import com.taara.bms.enums.GarmentSize;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StitchingDeliveryRepository extends JpaRepository<StitchingDelivery, UUID>, JpaSpecificationExecutor<StitchingDelivery> {

    Optional<StitchingDelivery> findByAutoIdIgnoreCase(String autoId);

    @Query("""
            select coalesce(sum(d.piecesDelivered), 0)
            from StitchingDelivery d
            where d.isDeleted = false
              and d.style.id = :styleId
              and d.size = :size
            """)
    Integer sumActiveDeliveredByStyleAndSize(@Param("styleId") UUID styleId, @Param("size") GarmentSize size);
}
