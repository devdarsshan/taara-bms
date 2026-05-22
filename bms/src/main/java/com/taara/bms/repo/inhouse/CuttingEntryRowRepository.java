package com.taara.bms.repo.inhouse;

import com.taara.bms.entity.inhouse.CuttingEntryRow;
import com.taara.bms.enums.GarmentSize;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuttingEntryRowRepository extends JpaRepository<CuttingEntryRow, UUID> {

    @Query("""
            select coalesce(sum(c.totalOutputPieces), 0)
            from CuttingEntry c
            join c.rows r
            where c.isDeleted = false
              and c.status = com.taara.bms.enums.CuttingStatus.COMPLETED
              and r.style.id = :styleId
              and r.size = :size
            """)
    BigDecimal sumCompletedOutputPiecesByStyleAndSize(@Param("styleId") UUID styleId, @Param("size") GarmentSize size);

    @Query("""
            select coalesce(sum(r.quantityUsedKgs), 0)
            from CuttingEntry c
            join c.rows r
            where c.isDeleted = false
              and r.dia.id = :diaId
              and r.style.id = :styleId
            """)
    BigDecimal sumActiveQuantityUsedByDiaAndStyle(@Param("diaId") UUID diaId, @Param("styleId") UUID styleId);

    @Query("""
            select r
            from CuttingEntry c
            join c.rows r
            where c.isDeleted = false
            """)
    List<CuttingEntryRow> findAllActiveRows();
}
