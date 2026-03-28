package com.taara.bms.repo.inhouse;

import com.taara.bms.entity.inhouse.InHouseDelivery;
import com.taara.bms.entity.spinning.SpinningDelivery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InHouseDeliveryRepository extends JpaRepository<InHouseDelivery, UUID>, JpaSpecificationExecutor<InHouseDelivery> {

    Optional<InHouseDelivery> findByAutoIdIgnoreCase(String autoId);

    boolean existsByStyle_IdAndIsDeletedFalse(UUID styleId);

    Optional<InHouseDelivery> findBySpinningDeliveryAndIsDeletedFalse(SpinningDelivery spinningDelivery);
}
