package com.taara.bms.mapper.common;

import com.taara.bms.dto.common.DiaRefResponse;
import com.taara.bms.dto.common.StitchingSectionRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.entity.masterdata.Dia;
import com.taara.bms.entity.masterdata.StitchingSection;
import com.taara.bms.entity.masterdata.Style;
import org.springframework.stereotype.Component;

@Component
public class ReferenceMapper {

    public StyleRefResponse toStyleRef(Style style) {
        return new StyleRefResponse(style.getId(), style.getAutoId(), style.getStyleName());
    }

    public DiaRefResponse toDiaRef(Dia dia) {
        return new DiaRefResponse(dia.getId(), dia.getAutoId(), dia.getDiaValue());
    }

    public StitchingSectionRefResponse toSectionRef(StitchingSection section) {
        return new StitchingSectionRefResponse(
                section.getId(),
                section.getAutoId(),
                section.getSectionName(),
                section.getType(),
                section.getProcessType()
        );
    }
}
