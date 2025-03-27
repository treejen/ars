package com.hktv.ars.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
@Table(name = "system_variable")
@AllArgsConstructor
@NoArgsConstructor
public class SystemVariable extends BaseModel {
    private String name;
    private String value;
    private String description;
    private Boolean visible;
    private Long createdBy;
    private Long lastModifiedBy;
}
