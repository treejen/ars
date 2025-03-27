package com.hktv.ars.service;

import com.hktv.ars.data.RegionResponseData;

public interface AhocorasickService {

    RegionResponseData extractAddresses(String address);

    RegionResponseData getDeliveryZoneCode(RegionResponseData regionResponseData);

}
