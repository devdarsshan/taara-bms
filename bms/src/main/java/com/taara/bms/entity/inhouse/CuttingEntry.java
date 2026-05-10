package com.taara.bms.entity.inhouse;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.enums.CuttingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "cuttings")
public class CuttingEntry extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "cutting_date", nullable = false)
    private LocalDate cuttingDate;

    @Column(name = "total_quantity_used_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalQuantityUsedKgs;

    @Column(name = "total_output_pieces", nullable = false)
    private Integer totalOutputPieces = 0;

    @Column(name = "pcs_per_kg", precision = 12, scale = 2)
    private BigDecimal pcsPerKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CuttingStatus status;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "cuttingEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CuttingEntryRow> rows = new ArrayList<>();
}
