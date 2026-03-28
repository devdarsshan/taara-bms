package com.taara.bms.entity.masterdata;

import com.taara.bms.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dia_auto_id", columnNames = "auto_id"),
        @UniqueConstraint(name = "uk_dia_value", columnNames = "dia_value")
})
public class Dia extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20)
    private String autoId;

    @Column(name = "dia_value", nullable = false, length = 50)
    private String diaValue;
}
