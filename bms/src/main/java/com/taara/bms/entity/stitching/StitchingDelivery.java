package com.taara.bms.entity.stitching;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.entity.masterdata.StitchingSection;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.enums.GarmentSize;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stitching_deliveries")
public class StitchingDelivery extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stitching_section_id", nullable = false)
    private StitchingSection stitchingSection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private GarmentSize size;

    @Column(name = "pieces_delivered", nullable = false)
    private Integer piecesDelivered;

    @OneToMany(mappedBy = "stitchingDelivery", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StitchingDeliveryAllocation> allocations = new ArrayList<>();
}
