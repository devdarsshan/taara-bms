package com.taara.bms.mapper.inhouse;

import com.taara.bms.dto.inhouse.CuttingResponse;
import com.taara.bms.dto.inhouse.InHouseDeliveryResponse;
import com.taara.bms.dto.inhouse.InHouseStockSplitResponse;
import com.taara.bms.entity.inhouse.CuttingEntry;
import com.taara.bms.entity.inhouse.InHouseDelivery;
import com.taara.bms.entity.inhouse.InHouseStockSplit;
import com.taara.bms.mapper.common.ReferenceMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class InHouseMapper {

    private final ReferenceMapper referenceMapper;

    public InHouseMapper(ReferenceMapper referenceMapper) {
        this.referenceMapper = referenceMapper;
    }

    public InHouseDeliveryResponse toDeliveryResponse(InHouseDelivery delivery, BigDecimal allocatedQuantity) {
        return new InHouseDeliveryResponse(
                delivery.getId(),
                delivery.getAutoId(),
                delivery.getDeliveryDate(),
                delivery.getSpinningDelivery().getId(),
                delivery.getSpinningDelivery().getAutoId(),
                referenceMapper.toStyleRef(delivery.getStyle()),
                delivery.getQuantityKgs(),
                allocatedQuantity,
                delivery.getSplitStatus(),
                delivery.isDeleted(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }

    public InHouseStockSplitResponse toSplitResponse(InHouseStockSplit split, boolean canDelete) {
        return new InHouseStockSplitResponse(
                split.getId(),
                split.getAutoId(),
                split.getDelivery().getId(),
                split.getDelivery().getAutoId(),
                referenceMapper.toDiaRef(split.getDia()),
                referenceMapper.toStyleRef(split.getStyle()),
                split.getQuantityKgs(),
                canDelete,
                split.isDeleted(),
                split.getCreatedAt(),
                split.getUpdatedAt()
        );
    }

    public CuttingResponse toCuttingResponse(CuttingEntry cuttingEntry) {
        return new CuttingResponse(
                cuttingEntry.getId(),
                cuttingEntry.getAutoId(),
                cuttingEntry.getCuttingDate(),
                referenceMapper.toDiaRef(cuttingEntry.getDia()),
                referenceMapper.toStyleRef(cuttingEntry.getStyle()),
                cuttingEntry.getQuantityUsedKgs(),
                cuttingEntry.getOutputPieces(),
                cuttingEntry.getStatus(),
                cuttingEntry.getNotes(),
                cuttingEntry.isDeleted(),
                cuttingEntry.getCreatedAt(),
                cuttingEntry.getUpdatedAt()
        );
    }
}
