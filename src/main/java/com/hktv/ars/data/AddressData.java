package com.hktv.ars.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class AddressData {
    private String code;
    private String deliveryZoneCode;
    private String enName;
    private String zhName;
    private BigDecimal latitude;
    private BigDecimal longitude;

}
