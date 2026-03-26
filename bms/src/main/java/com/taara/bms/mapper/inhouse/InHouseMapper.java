package com.taara.bms.mapper.inhouse;

import com.taara.bms.dto.inhouse.CuttingRowResponse;
import com.taara.bms.dto.inhouse.CuttingResponse;
import com.taara.bms.dto.inhouse.InHouseDeliveryResponse;
import com.taara.bms.dto.inhouse.InHouseStockSplitResponse;
import com.taara.bms.entity.inhouse.CuttingEntry;
import com.taara.bms.entity.inhouse.CuttingEntryRow;
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
                cuttingEntry.getTotalQuantityUsedKgs(),
                cuttingEntry.getTotalOutputPieces(),
                cuttingEntry.getPcsPerKg(),
                cuttingEntry.getStatus(),
                cuttingEntry.getNotes(),
                cuttingEntry.getRows().stream().map(this::toCuttingRowResponse).toList(),
                cuttingEntry.isDeleted(),
                cuttingEntry.getCreatedAt(),
                cuttingEntry.getUpdatedAt()
        );
    }

    public CuttingRowResponse toCuttingRowResponse(CuttingEntryRow row) {
        return new CuttingRowResponse(
                row.getId(),
                referenceMapper.toDiaRef(row.getDia()),
                referenceMapper.toStyleRef(row.getStyle()),
                row.getSize(),
                row.getQuantityUsedKgs(),
                row.getOutputPieces()
        );
    }
}
