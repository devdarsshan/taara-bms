package com.taara.bms.entity.packing;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.PackingStockType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "packing_entries")
public class PackingEntry extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "packing_date", nullable = false)
    private LocalDate packingDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private GarmentSize size;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_type", nullable = false, length = 20)
    private PackingStockType stockType;

    @Column(name = "correctly_packed_pieces", nullable = false)
    private Integer correctlyPackedPieces;

    @Column(name = "defective_pieces", nullable = false)
    private Integer defectivePieces;
}
