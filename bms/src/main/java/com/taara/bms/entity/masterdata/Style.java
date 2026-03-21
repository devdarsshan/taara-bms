package com.taara.bms.entity.masterdata;

import com.taara.bms.entity.common.BaseEntity;
import com.taara.bms.helper.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "styles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_style_auto_id", columnNames = "auto_id"),
        @UniqueConstraint(name = "uk_style_name", columnNames = "style_name")
})
public class Style extends BaseEntity {

    @Column(name = "auto_id", nullable = false, length = 20)
    private String autoId;

    @Column(name = "style_name", nullable = false, length = 150)
    private String styleName;

    @Convert(converter = StringListConverter.class)
    @Column(name = "colors", nullable = false, columnDefinition = "text")
    private List<String> colors = new ArrayList<>();
}
