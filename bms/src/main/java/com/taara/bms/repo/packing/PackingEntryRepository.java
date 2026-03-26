package com.taara.bms.repo.packing;

import com.taara.bms.entity.packing.PackingEntry;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.PackingStockType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PackingEntryRepository extends JpaRepository<PackingEntry, UUID>, JpaSpecificationExecutor<PackingEntry> {

    Optional<PackingEntry> findByAutoIdIgnoreCase(String autoId);

    @Query("""
            select coalesce(sum(p.correctlyPackedPieces + p.defectivePieces), 0)
            from PackingEntry p
            where p.isDeleted = false
              and p.stockType = :stockType
              and p.style.id = :styleId
              and p.size = :size
            """)
    Integer sumConsumedBySourceAndStyleAndSize(
            @Param("stockType") PackingStockType stockType,
            @Param("styleId") UUID styleId,
            @Param("size") GarmentSize size
    );

    @Query("""
            select coalesce(sum(p.defectivePieces), 0)
            from PackingEntry p
            where p.isDeleted = false
            """)
    Integer sumAllDefectivePieces();
}
