package com.taara.bms.service.common;

public enum AutoIdSequence {
    STYLE("style_auto_seq", "STY"),
    DIA("dia_auto_seq", "DIA"),
    STITCHING_SECTION("stitching_section_auto_seq", "SEC"),
    YARN_ORDER("yarn_order_auto_seq", "YRN"),
    SPINNING_ORDER("spinning_order_auto_seq", "SPO"),
    SPINNING_DELIVERY("spinning_delivery_auto_seq", "SPD"),
    INHOUSE_DELIVERY("inhouse_delivery_auto_seq", "IHD"),
    INHOUSE_STOCK_SPLIT("inhouse_stock_split_auto_seq", "STK"),
    CUTTING("cutting_auto_seq", "CUT"),
    STITCHING_ORDER("stitching_order_auto_seq", "STO"),
    STITCHING_DELIVERY("stitching_delivery_auto_seq", "STD");

    private final String sequenceName;
    private final String prefix;

    AutoIdSequence(String sequenceName, String prefix) {
        this.sequenceName = sequenceName;
        this.prefix = prefix;
    }

    public String sequenceName() {
        return sequenceName;
    }

    public String prefix() {
        return prefix;
    }
}
