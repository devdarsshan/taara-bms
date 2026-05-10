package com.taara.bms.entity.spinning;

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
@Table(name = "spinning_deliveries")
public class SpinningDelivery extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Column(name = "actual_quantity_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal actualQuantityKgs;

    @Column(name = "buffer_quantity_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal bufferQuantityKgs;

    @Column(name = "final_quantity_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalQuantityKgs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stitching_section_id")
    private com.taara.bms.entity.masterdata.StitchingSection stitchingSection;

    @Column(name = "price_per_kg", precision = 12, scale = 2)
    private BigDecimal pricePerKg;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "balance_amount", precision = 12, scale = 2)
    private BigDecimal balanceAmount;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private com.taara.bms.enums.PaymentStatus paymentStatus;

    @Column(columnDefinition = "text")
    private String notes;
}
