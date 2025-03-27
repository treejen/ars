package com.hktv.ars.service;

import com.hktv.ars.data.RegionResponseData;

public interface JeibaService {
    RegionResponseData extractAddresses(String text);
}
