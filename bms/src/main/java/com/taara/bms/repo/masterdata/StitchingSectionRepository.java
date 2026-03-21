package com.taara.bms.repo.masterdata;

import com.taara.bms.entity.masterdata.StitchingSection;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StitchingSectionRepository extends JpaRepository<StitchingSection, UUID>, JpaSpecificationExecutor<StitchingSection> {

    Optional<StitchingSection> findByAutoIdIgnoreCase(String autoId);

    boolean existsBySectionNameIgnoreCaseAndIsDeletedFalse(String sectionName);

    boolean existsBySectionNameIgnoreCaseAndIdNotAndIsDeletedFalse(String sectionName, UUID id);
}
