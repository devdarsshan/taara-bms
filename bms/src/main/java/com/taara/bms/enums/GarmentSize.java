package com.taara.bms.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GarmentSize {
    XS,
    S,
    M,
    L,
    XL,
    @JsonProperty("2XL")
    _2XL
}
