package com.taara.bms.mapper.spinning;

import com.taara.bms.dto.spinning.SpinningDeliveryResponse;
import com.taara.bms.dto.spinning.SpinningOrderResponse;
import com.taara.bms.entity.spinning.SpinningDelivery;
import com.taara.bms.entity.spinning.SpinningOrder;
import com.taara.bms.mapper.common.ReferenceMapper;
import org.springframework.stereotype.Component;

@Component
public class SpinningMapper {

    private final ReferenceMapper referenceMapper;

    public SpinningMapper(ReferenceMapper referenceMapper) {
        this.referenceMapper = referenceMapper;
    }

    public SpinningOrderResponse toOrderResponse(SpinningOrder order) {
        return new SpinningOrderResponse(
                order.getId(),
                order.getAutoId(),
                order.getDispatchDate(),
                order.getLinkedYarnOrder() != null ? order.getLinkedYarnOrder().getId() : null,
                order.getLinkedYarnOrder() != null ? order.getLinkedYarnOrder().getAutoId() : null,
                referenceMapper.toStyleRef(order.getStyle()),
                order.getQuantitySentKgs(),
                order.getFactoryNotes(),
                order.isAutoCreated(),
                order.isDeleted(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public SpinningDeliveryResponse toDeliveryResponse(SpinningDelivery delivery) {
        return new SpinningDeliveryResponse(
                delivery.getId(),
                delivery.getAutoId(),
                delivery.getDeliveryDate(),
                referenceMapper.toStyleRef(delivery.getStyle()),
                delivery.getActualQuantityKgs(),
                delivery.getBufferQuantityKgs(),
                delivery.getFinalQuantityKgs(),
                delivery.getNotes(),
                delivery.isDeleted(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}
