package com.hktv.ars.service;

import com.hktv.ars.data.RegionResponseData;

public interface AddressExtractionService {

    RegionResponseData extractAddresses(String address);

}
