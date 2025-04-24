package com.hktv.ars.service;

import com.hktv.ars.data.AddressData;
import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.enums.AddressType;

import java.util.List;

public interface AhocorasickService {

    List<String> initMapByAddressType(AddressType addressType, List<AddressData> addressDataList);

    void initModel(List<String> keywords);

    RegionResponseData getDeliveryZoneCode(RegionResponseData regionResponseData);

}
