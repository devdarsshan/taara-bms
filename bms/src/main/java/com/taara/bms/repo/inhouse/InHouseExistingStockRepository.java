package com.taara.bms.repo.inhouse;

import com.taara.bms.entity.inhouse.InHouseExistingStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InHouseExistingStockRepository extends JpaRepository<InHouseExistingStock, UUID> {

    List<InHouseExistingStock> findAllByIsDeletedFalseOrderByEntryDateDesc();

    @Query("select coalesce(sum(e.quantityKgs), 0) from InHouseExistingStock e where e.isDeleted = false and e.dia.id = :diaId and e.style.id = :styleId")
    BigDecimal sumQuantityByDiaAndStyle(@Param("diaId") UUID diaId, @Param("styleId") UUID styleId);
}
