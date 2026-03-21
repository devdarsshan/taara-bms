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

    boolean existsByStyle_IdAndIsDeletedFalse(UUID styleId);

    boolean existsByDia_IdAndIsDeletedFalse(UUID diaId);

    @Query("""
            select coalesce(sum(c.quantityUsedKgs), 0)
            from CuttingEntry c
            where c.isDeleted = false
              and c.dia.id = :diaId
              and c.style.id = :styleId
            """)
    BigDecimal sumActiveQuantityByDiaAndStyle(@Param("diaId") UUID diaId, @Param("styleId") UUID styleId);
}
