package com.taara.bms.entity.masterdata;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.enums.StitchingSectionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stitching_sections", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stitching_section_auto_id", columnNames = "auto_id"),
        @UniqueConstraint(name = "uk_stitching_section_name", columnNames = "section_name")
})
public class StitchingSection extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20)
    private String autoId;

    @Column(name = "section_name", nullable = false, length = 150)
    private String sectionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StitchingSectionType type;
}
