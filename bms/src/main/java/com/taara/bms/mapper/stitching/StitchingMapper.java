package com.taara.bms.mapper.stitching;

import com.taara.bms.dto.stitching.StitchingDeliveryResponse;
import com.taara.bms.dto.stitching.StitchingOrderResponse;
import com.taara.bms.entity.stitching.StitchingDelivery;
import com.taara.bms.entity.stitching.StitchingOrder;
import com.taara.bms.mapper.common.ReferenceMapper;
import org.springframework.stereotype.Component;

@Component
public class StitchingMapper {

    private final ReferenceMapper referenceMapper;

    public StitchingMapper(ReferenceMapper referenceMapper) {
        this.referenceMapper = referenceMapper;
    }

    public StitchingOrderResponse toOrderResponse(StitchingOrder order, int deliveredPieces) {
        int pendingPieces = Math.max(order.getPiecesOrdered() - deliveredPieces, 0);
        return new StitchingOrderResponse(
                order.getId(),
                order.getAutoId(),
                order.getOrderDate(),
                referenceMapper.toSectionRef(order.getStitchingSection()),
                referenceMapper.toStyleRef(order.getStyle()),
                order.getPiecesOrdered(),
                deliveredPieces,
                pendingPieces,
                order.getStatus(),
                order.getNotes(),
                order.isDeleted(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public StitchingDeliveryResponse toDeliveryResponse(StitchingDelivery delivery) {
        return new StitchingDeliveryResponse(
                delivery.getId(),
                delivery.getAutoId(),
                delivery.getDeliveryDate(),
                delivery.getStitchingOrder().getId(),
                delivery.getStitchingOrder().getAutoId(),
                referenceMapper.toSectionRef(delivery.getStitchingOrder().getStitchingSection()),
                referenceMapper.toStyleRef(delivery.getStitchingOrder().getStyle()),
                delivery.getPiecesDelivered(),
                delivery.isDeleted(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}
