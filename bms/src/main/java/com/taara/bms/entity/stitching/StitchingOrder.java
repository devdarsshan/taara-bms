package com.taara.bms.entity.stitching;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.StitchingOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "stitching_orders")
public class StitchingOrder extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_size", nullable = false, length = 10)
    private GarmentSize expectedSize;

    @Column(name = "expected_pieces", nullable = false)
    private Integer expectedPieces;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StitchingOrderStatus status;

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "stitchingOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StitchingOrderRow> rows = new ArrayList<>();
}
