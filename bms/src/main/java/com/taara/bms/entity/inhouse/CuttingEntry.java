package com.taara.bms.entity.inhouse;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.entity.masterdata.Dia;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.enums.CuttingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "cuttings")
public class CuttingEntry extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "cutting_date", nullable = false)
    private LocalDate cuttingDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_id", nullable = false)
    private Dia dia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Column(name = "quantity_used_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityUsedKgs;

    @Column(name = "output_pieces")
    private Integer outputPieces;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CuttingStatus status;

    @Column(columnDefinition = "text")
    private String notes;
}
