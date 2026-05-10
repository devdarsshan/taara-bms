package com.taara.bms.mapper.stitching;

import com.taara.bms.dto.stitching.StitchingDeliveryResponse;
import com.taara.bms.dto.stitching.StitchingOrderRowResponse;
import com.taara.bms.dto.stitching.StitchingOrderResponse;
import com.taara.bms.entity.stitching.StitchingDelivery;
import com.taara.bms.entity.stitching.StitchingOrder;
import com.taara.bms.entity.stitching.StitchingOrderRow;
import com.taara.bms.mapper.common.ReferenceMapper;
import org.springframework.stereotype.Component;

@Component
public class StitchingMapper {

    private final ReferenceMapper referenceMapper;

    public StitchingMapper(ReferenceMapper referenceMapper) {
        this.referenceMapper = referenceMapper;
    }

    public StitchingOrderResponse toOrderResponse(StitchingOrder order, int deliveredPieces) {
        int pendingPieces = Math.max(order.getExpectedPieces() - deliveredPieces, 0);
        int totalPiecesTaken = order.getRows().stream().mapToInt(StitchingOrderRow::getPiecesTaken).sum();
        return new StitchingOrderResponse(
                order.getId(),
                order.getAutoId(),
                order.getOrderDate(),
                order.getExpectedSize(),
                order.getExpectedPieces(),
                deliveredPieces,
                pendingPieces,
                totalPiecesTaken,
                order.getStatus(),
                order.getNotes(),
                order.getRows().stream().map(this::toOrderRowResponse).toList(),
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
                referenceMapper.toSectionRef(delivery.getStitchingSection()),
                referenceMapper.toStyleRef(delivery.getStyle()),
                delivery.getSize(),
                delivery.getPiecesDelivered(),
                delivery.isDeleted(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }

    public StitchingOrderRowResponse toOrderRowResponse(StitchingOrderRow row) {
        java.math.BigDecimal totalPrice = row.getRatePerPiece() != null ? row.getRatePerPiece().multiply(java.math.BigDecimal.valueOf(row.getPiecesTaken())) : null;
        return new StitchingOrderRowResponse(
                row.getId(),
                referenceMapper.toSectionRef(row.getStitchingSection()),
                referenceMapper.toStyleRef(row.getStyle()),
                row.getSize(),
                row.getPiecesTaken(),
                row.getRatePerPiece(),
                totalPrice
        );
    }
}
