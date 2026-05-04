package com.taara.bms.entity.inhouse;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.entity.masterdata.Dia;
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
@Table(name = "in_house_existing_stocks")
public class InHouseExistingStock extends BaseEntity {

    @Column(name = "auto_id", nullable = false, unique = true, length = 20)
    private String autoId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_id", nullable = false)
    private Dia dia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Column(name = "quantity_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKgs;

    @Column(columnDefinition = "text")
    private String notes;
}
