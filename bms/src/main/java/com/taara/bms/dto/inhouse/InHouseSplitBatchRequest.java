package com.taara.bms.dto.inhouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InHouseSplitBatchRequest(
        @NotEmpty(message = "At least one split is required")
        List<@Valid InHouseSplitRequest> splits
) {
}
