package com.hktv.ars.model;

import com.hktv.ars.enums.AddressAnalysisType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "address_record")
@AllArgsConstructor
@NoArgsConstructor
public class AddressRecord extends BaseModel {

    private String address;
    private String estate;
    private String street;
    private String streetNumber;
    private String district;
    @Enumerated(EnumType.STRING)
    private AddressAnalysisType analysisType;
    private String originalDeliveryZoneCode;
    private String actualDeliveryZoneCode;
    private Boolean willDeliver;
    private LocalDateTime receiveTime;
    private Long updateBy;
    private String status;
    private BigDecimal latitude;
    private BigDecimal longitude;

}
