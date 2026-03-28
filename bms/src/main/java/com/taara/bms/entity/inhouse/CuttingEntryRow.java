package com.taara.bms.entity.inhouse;

import com.taara.bms.entity.masterdata.Dia;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.enums.GarmentSize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cutting_rows")
public class CuttingEntryRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cutting_entry_id", nullable = false)
    private CuttingEntry cuttingEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_id", nullable = false)
    private Dia dia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private GarmentSize size;

    @Column(name = "quantity_used_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityUsedKgs;

    @Column(name = "output_pieces")
    private Integer outputPieces;
}
