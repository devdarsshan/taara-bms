package com.taara.bms.mapper.masterdata;

import com.taara.bms.dto.masterdata.DiaResponse;
import com.taara.bms.dto.masterdata.StitchingSectionResponse;
import com.taara.bms.dto.masterdata.StyleResponse;
import com.taara.bms.entity.masterdata.Dia;
import com.taara.bms.entity.masterdata.StitchingSection;
import com.taara.bms.entity.masterdata.Style;
import org.springframework.stereotype.Component;

@Component
public class MasterDataMapper {

    public StyleResponse toStyleResponse(Style style) {
        return new StyleResponse(
                style.getId(),
                style.getAutoId(),
                style.getStyleName(),
                style.getColors(),
                style.isDeleted(),
                style.getCreatedAt(),
                style.getUpdatedAt()
        );
    }

    public DiaResponse toDiaResponse(Dia dia) {
        return new DiaResponse(
                dia.getId(),
                dia.getAutoId(),
                dia.getDiaValue(),
                dia.isDeleted(),
                dia.getCreatedAt(),
                dia.getUpdatedAt()
        );
    }

    public StitchingSectionResponse toSectionResponse(StitchingSection section) {
        return new StitchingSectionResponse(
                section.getId(),
                section.getAutoId(),
                section.getSectionName(),
                section.getType(),
                section.getProcessType(),
                section.isDeleted(),
                section.getCreatedAt(),
                section.getUpdatedAt()
        );
    }
}
