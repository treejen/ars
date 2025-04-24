package com.hktv.ars.service;

import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.model.AddressRecord;

public interface AddressAnalysisService {

    RegionResponseData analyzeAddress(AddressRecord result);
}
