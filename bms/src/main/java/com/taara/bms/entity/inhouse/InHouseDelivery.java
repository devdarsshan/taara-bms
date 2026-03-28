package com.taara.bms.entity.inhouse;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.spinning.SpinningDelivery;
import com.taara.bms.enums.SplitStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "inhouse_deliveries")
public class InHouseDelivery extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spinning_delivery_id", nullable = false, unique = true)
    private SpinningDelivery spinningDelivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Column(name = "quantity_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKgs;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_status", nullable = false, length = 30)
    private SplitStatus splitStatus;
}
