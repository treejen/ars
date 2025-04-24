package com.hktv.ars.model;

import com.hktv.ars.data.AddressData;
import com.hktv.ars.data.ExcelMappingData;
import com.hktv.ars.util.CleanWordUtil;
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
@Table(name = "street_number")
@AllArgsConstructor
@NoArgsConstructor
public class StreetNumber extends BaseModel {
    private String streetCode;
    private String streetNumberCode;
    private String deliveryZoneCode;
    private Boolean willDelivery;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String streetNumber;

    public static StreetNumber covertExcelData(ExcelMappingData data) {
        try {
            return StreetNumber.builder()
                    .streetNumberCode(data.getStreetNumberCode())
                    .streetCode(data.getStreetCode())
                    .deliveryZoneCode(data.getDeliveryZoneCode())
                    .willDelivery("Y".equals(data.getWillDelivery()) ? Boolean.TRUE : Boolean.FALSE)
                    .latitude(new BigDecimal(data.getLatitude().trim()).setScale(6, RoundingMode.DOWN))
                    .longitude(new BigDecimal(data.getLongitude().trim()).setScale(6, RoundingMode.DOWN))
                    .streetNumber(CleanWordUtil.truncateString(CleanWordUtil.removeBrackets(data.getStreetNumber()), 10))
                    .build();
        } catch (Exception e) {
            System.out.println(e);
            System.out.println(data);
        }
        return null;
    }

    public static AddressData convertToAddressData(StreetNumber data) {
        return AddressData.builder()
                .code(data.getStreetCode())
                .deliveryZoneCode(data.getDeliveryZoneCode())
                .enName(data.getStreetNumber())
                .zhName(data.getStreetNumber())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .build();
    }
}
