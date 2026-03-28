package com.taara.bms.repo.stitching;

import com.taara.bms.entity.stitching.StitchingOrder;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StitchingOrderRepository extends JpaRepository<StitchingOrder, UUID>, JpaSpecificationExecutor<StitchingOrder> {

    Optional<StitchingOrder> findByAutoIdIgnoreCase(String autoId);

    @Query("""
            select count(o) > 0
            from StitchingOrder o
            join o.rows r
            where o.isDeleted = false
              and r.style.id = :styleId
            """)
    boolean existsActiveByRowStyleId(@Param("styleId") UUID styleId);

    @Query("""
            select count(o) > 0
            from StitchingOrder o
            join o.rows r
            where o.isDeleted = false
              and r.stitchingSection.id = :sectionId
            """)
    boolean existsActiveByRowSectionId(@Param("sectionId") UUID sectionId);
}
