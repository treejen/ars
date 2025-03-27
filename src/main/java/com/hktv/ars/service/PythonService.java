package com.hktv.ars.service;

import com.hktv.ars.data.RegionResponseData;

public interface PythonService {

    RegionResponseData extractAddresses(String address);
}
