package com.hktv.ars.model;

import com.hktv.ars.data.AddressData;
import com.hktv.ars.data.ExcelMappingData;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@Builder
@Entity
@Table(name = "estate")
@AllArgsConstructor
@NoArgsConstructor
public class Estate extends BaseModel {
    private String estateCode;
    private String districtCode;
    private String deliveryZoneCode;
    private Boolean isActive;
    private Boolean willDelivery;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String estateNameEn;
    private String estateNameZh;

    public static Estate covertExcelData(ExcelMappingData data) {
        try {
            return Estate.builder()
                    .estateCode(data.getEstateCode())
                    .districtCode(data.getDistrictCode())
                    .deliveryZoneCode(data.getDeliveryZoneCode())
                    .isActive("Y".equals(data.getIsActive()) ? Boolean.TRUE : Boolean.FALSE)
                    .willDelivery("Y".equals(data.getWillDelivery()) ? Boolean.TRUE : Boolean.FALSE)
                    .latitude(new BigDecimal(data.getLatitude().trim()).setScale(6, RoundingMode.DOWN))
                    .longitude(new BigDecimal(data.getLongitude().trim()).setScale(6, RoundingMode.DOWN))
                    .estateNameEn(data.getEstateNameEn())
                    .estateNameZh(data.getEstateNameZh())
                    .build();
        } catch (Exception e) {
            System.out.println(e);
            System.out.println(data);
        }
        return null;
    }

    public static AddressData convertToAddressData(Estate data) {
        return AddressData.builder()
                .code(data.getEstateCode())
                .deliveryZoneCode(data.getDeliveryZoneCode())
                .enName(data.getEstateNameEn())
                .zhName(data.getEstateNameZh())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .build();
    }
}
