package com.taara.bms.entity.stitching;

import com.taara.bms.entity.masterdata.StitchingSection;
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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stitching_order_rows")
public class StitchingOrderRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stitching_order_id", nullable = false)
    private StitchingOrder stitchingOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stitching_section_id", nullable = false)
    private StitchingSection stitchingSection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private GarmentSize size;

    @Column(name = "pieces_taken", nullable = false)
    private Integer piecesTaken;

    @Column(name = "rate_per_piece", precision = 12, scale = 2)
    private java.math.BigDecimal ratePerPiece;
}
