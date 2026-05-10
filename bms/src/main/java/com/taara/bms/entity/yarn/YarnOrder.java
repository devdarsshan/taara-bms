package com.taara.bms.entity.yarn;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.entity.masterdata.Style;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "yarn_orders")
public class YarnOrder extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Column(name = "quantity_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKgs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stitching_section_id")
    private com.taara.bms.entity.masterdata.StitchingSection stitchingSection;

    @Column(name = "supplier_notes", columnDefinition = "text")
    private String supplierNotes;
}
