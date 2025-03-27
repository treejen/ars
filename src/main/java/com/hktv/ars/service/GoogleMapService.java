package com.hktv.ars.service;

import com.hktv.ars.data.RegionResponseData;

public interface GoogleMapService {

    RegionResponseData extractAddresses(String address);
}
