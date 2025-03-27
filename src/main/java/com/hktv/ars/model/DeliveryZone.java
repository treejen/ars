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
@Table(name = "delivery_zone")
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryZone extends BaseModel{
    private String zoneCode;
    private String zoneDesc;
}
