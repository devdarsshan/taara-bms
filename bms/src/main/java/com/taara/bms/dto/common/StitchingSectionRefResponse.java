package com.taara.bms.dto.common;

import com.taara.bms.enums.SectionProcessType;
import com.taara.bms.enums.StitchingSectionType;
import java.util.UUID;

public record StitchingSectionRefResponse(
        UUID id,
        String autoId,
        String sectionName,
        StitchingSectionType type,
        SectionProcessType processType
) {
}
