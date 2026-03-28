package com.taara.bms.repo.inhouse;

import com.taara.bms.entity.inhouse.CuttingEntryRow;
import com.taara.bms.enums.GarmentSize;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuttingEntryRowRepository extends JpaRepository<CuttingEntryRow, UUID> {

    @Query("""
            select coalesce(sum(r.outputPieces), 0)
            from CuttingEntry c
            join c.rows r
            where c.isDeleted = false
              and c.status = com.taara.bms.enums.CuttingStatus.COMPLETED
              and r.style.id = :styleId
              and r.size = :size
            """)
    Integer sumCompletedOutputPiecesByStyleAndSize(@Param("styleId") UUID styleId, @Param("size") GarmentSize size);

    @Query("""
            select r
            from CuttingEntry c
            join c.rows r
            where c.isDeleted = false
            """)
    List<CuttingEntryRow> findAllActiveRows();
}
