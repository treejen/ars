package com.hktv.ars.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExcelMappingData {
    private String estateCode;

    private String streetCode;

    private String districtCode;

    private String deliveryZoneCode;

    private String isActive;
    private String willDelivery;
    private String latitude;
    private String longitude;

    private String districtNameEn;
    private String districtNameZh;

    private String estateNameEn;
    private String estateNameZh;

    private String streetNameEn;
    private String streetNameZh;

    private String streetNumber;


}
