package com.taara.bms.entity.spinning;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.yarn.YarnOrder;
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
@Table(name = "spinning_orders")
public class SpinningOrder extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_yarn_order_id")
    private YarnOrder linkedYarnOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @Column(name = "quantity_sent_kgs", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantitySentKgs;

    @Column(name = "factory_notes", columnDefinition = "text")
    private String factoryNotes;

    @Column(name = "auto_created", nullable = false)
    private boolean autoCreated;
}
