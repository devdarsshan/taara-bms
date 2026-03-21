package com.taara.bms.repo.stitching;

import com.taara.bms.entity.stitching.StitchingOrder;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StitchingOrderRepository extends JpaRepository<StitchingOrder, UUID>, JpaSpecificationExecutor<StitchingOrder> {

    Optional<StitchingOrder> findByAutoIdIgnoreCase(String autoId);

    boolean existsByStyle_IdAndIsDeletedFalse(UUID styleId);

    boolean existsByStitchingSection_IdAndIsDeletedFalse(UUID sectionId);
}
