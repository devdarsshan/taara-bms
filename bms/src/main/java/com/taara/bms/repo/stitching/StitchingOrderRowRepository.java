package com.taara.bms.repo.stitching;

import com.taara.bms.entity.stitching.StitchingOrderRow;
import com.taara.bms.enums.GarmentSize;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StitchingOrderRowRepository extends JpaRepository<StitchingOrderRow, UUID> {

    @Query("""
            select coalesce(sum(r.piecesTaken), 0)
            from StitchingOrder o
            join o.rows r
            where o.isDeleted = false
              and r.style.id = :styleId
              and r.size = :size
            """)
    Integer sumActivePiecesTakenByStyleAndSize(@Param("styleId") UUID styleId, @Param("size") GarmentSize size);

    @Query("""
            select r
            from StitchingOrder o
            join o.rows r
            where o.isDeleted = false
            """)
    List<StitchingOrderRow> findAllActiveRows();
}
