package com.taara.bms.repo.masterdata;

import com.taara.bms.entity.masterdata.Dia;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DiaRepository extends JpaRepository<Dia, UUID>, JpaSpecificationExecutor<Dia> {

    Optional<Dia> findByAutoIdIgnoreCase(String autoId);

    boolean existsByDiaValueIgnoreCaseAndIsDeletedFalse(String diaValue);

    boolean existsByDiaValueIgnoreCaseAndIdNotAndIsDeletedFalse(String diaValue, UUID id);
}
