package com.hktv.ars.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegionResponseData {
    String address;
    String street;
    String dist;
    String number;
    String estate;
    String deliveryZoneCode;
    BigDecimal latitude;
    BigDecimal longitude;
}
