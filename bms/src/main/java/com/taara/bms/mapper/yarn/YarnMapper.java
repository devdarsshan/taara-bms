package com.taara.bms.mapper.yarn;

import com.taara.bms.dto.yarn.YarnOrderResponse;
import com.taara.bms.entity.yarn.YarnOrder;
import com.taara.bms.mapper.common.ReferenceMapper;
import org.springframework.stereotype.Component;

@Component
public class YarnMapper {

    private final ReferenceMapper referenceMapper;

    public YarnMapper(ReferenceMapper referenceMapper) {
        this.referenceMapper = referenceMapper;
    }

    public YarnOrderResponse toResponse(YarnOrder yarnOrder) {
        return new YarnOrderResponse(
                yarnOrder.getId(),
                yarnOrder.getAutoId(),
                yarnOrder.getOrderDate(),
                referenceMapper.toStyleRef(yarnOrder.getStyle()),
                yarnOrder.getQuantityKgs(),
                yarnOrder.getSupplierNotes(),
                yarnOrder.isDeleted(),
                yarnOrder.getCreatedAt(),
                yarnOrder.getUpdatedAt()
        );
    }
}
