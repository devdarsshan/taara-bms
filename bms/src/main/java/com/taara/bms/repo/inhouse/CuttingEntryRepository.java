package com.taara.bms.repo.inhouse;

import com.taara.bms.entity.inhouse.CuttingEntry;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuttingEntryRepository extends JpaRepository<CuttingEntry, UUID>, JpaSpecificationExecutor<CuttingEntry> {

    Optional<CuttingEntry> findByAutoIdIgnoreCase(String autoId);

    @Query("""
            select count(c) > 0
            from CuttingEntry c
            join c.rows r
            where c.isDeleted = false
              and r.style.id = :styleId
            """)
    boolean existsActiveByRowStyleId(@Param("styleId") UUID styleId);

    @Query("""
            select count(c) > 0
            from CuttingEntry c
            join c.rows r
            where c.isDeleted = false
              and r.dia.id = :diaId
            """)
    boolean existsActiveByRowDiaId(@Param("diaId") UUID diaId);

    @Query("""
            select coalesce(sum(r.quantityUsedKgs), 0)
            from CuttingEntry c
            join c.rows r
            where c.isDeleted = false
              and r.dia.id = :diaId
              and r.style.id = :styleId
            """)
    BigDecimal sumActiveQuantityByDiaAndStyle(@Param("diaId") UUID diaId, @Param("styleId") UUID styleId);
}
