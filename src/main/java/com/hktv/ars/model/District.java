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
@Table(name = "district")
@AllArgsConstructor
@NoArgsConstructor
public class District extends BaseModel{
    private String districtCode;
    private String deliveryZoneCode;
    private String districtNameEn;
    private String districtNameZh;
}
