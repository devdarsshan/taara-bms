package com.taara.bms.entity.printing;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.entity.masterdata.StitchingSection;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.StitchingOrderStatus;
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
@Table(name = "printing_orders")
public class PrintingOrder extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "printing_section_id", nullable = false)
    private StitchingSection printingSection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private GarmentSize size;

    @Column(name = "pieces_ordered", nullable = false)
    private Integer piecesOrdered;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StitchingOrderStatus status;

    @Column(columnDefinition = "text")
    private String notes;
}
