package com.taara.bms.repo.masterdata;

import com.taara.bms.entity.masterdata.Style;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StyleRepository extends JpaRepository<Style, UUID>, JpaSpecificationExecutor<Style> {

    Optional<Style> findByAutoIdIgnoreCase(String autoId);

    boolean existsByStyleNameIgnoreCaseAndIsDeletedFalse(String styleName);

    boolean existsByStyleNameIgnoreCaseAndIdNotAndIsDeletedFalse(String styleName, UUID id);
}
