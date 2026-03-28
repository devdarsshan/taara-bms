package com.taara.bms.dto.masterdata;

import com.taara.bms.enums.SectionProcessType;

public record ProcessTypeOptionResponse(
        String label,
        SectionProcessType value
) {
}
