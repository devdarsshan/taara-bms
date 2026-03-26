package com.taara.bms.entity.printing;

import com.taara.bms.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "printing_deliveries")
public class PrintingDelivery extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20, unique = true)
    private String autoId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "printing_order_id", nullable = false)
    private PrintingOrder printingOrder;

    @Column(name = "pieces_delivered", nullable = false)
    private Integer piecesDelivered;
}
