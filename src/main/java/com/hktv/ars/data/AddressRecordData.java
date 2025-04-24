package com.hktv.ars.data;

import java.time.LocalDateTime;

public interface AddressRecordData {

    String getAddress();
    String getEstate();
    String getStreet();
    String getStreetNumber();
    String getDistrict();
    String getAnalysisType();
    String getOriginalDeliveryZoneCode();
    String getActualDeliveryZoneCode();
    String getWillDeliver();
    LocalDateTime getReceiveTime();
    String getStatus();
    Double getLatitude();
    Double getLongitude();
    LocalDateTime getCreationDate();
    LocalDateTime getLastModifiedDate();
    String getUpdateBy();

}
