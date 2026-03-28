package com.taara.bms.dto.masterdata;

import com.taara.bms.enums.SectionProcessType;
import com.taara.bms.enums.StitchingSectionType;
import java.time.LocalDateTime;
import java.util.UUID;

public record StitchingSectionResponse(
        UUID id,
        String autoId,
        String sectionName,
        StitchingSectionType type,
        SectionProcessType processType,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
