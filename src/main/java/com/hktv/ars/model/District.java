package com.hktv.ars.model;

import com.hktv.ars.data.AddressData;
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

    public static AddressData convertToAddressData(District data) {
        return AddressData.builder()
                .code(data.getDistrictCode())
                .deliveryZoneCode(data.getDeliveryZoneCode())
                .enName(data.getDistrictNameEn())
                .zhName(data.getDistrictNameZh())
                .build();
    }
}
